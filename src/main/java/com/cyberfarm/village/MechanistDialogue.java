package com.cyberfarm.village;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;
import com.cyberfarm.item.BlueprintDiscItem;
import com.cyberfarm.registry.ModVillagers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * What the Drone Mechanist says when a player right-clicks him. The first time, his story and his
 * offer of a tech village, with three answers; once a player founds it, the village's progress: the
 * buildings finished since the last talk; a new level when its prosperity reaches the mark and the
 * last level's buildings stand (talking to him is what raises it), with that level's blueprint
 * discs; otherwise what it still needs, and a word for the time of day. Every player gets their own
 * discs. Trading is a button at the end of each talk; a name tag still names him.
 *
 * <p>Every line takes the same arguments: the player, a village level, a building, the Mechanist's
 * name and the village's founder.
 */
public final class MechanistDialogue {
    public enum Choice { ACCEPT, DECLINE, DOUBT, TRADE, BYE }

    public static final String KEY = "dialogue.cyberfarm.mechanist.";
    /** How many names a Mechanist may get, {@code name.0} to {@code name.11} in the language file. */
    private static final int NAMES = 12;
    private static final double REACH = 8.0;
    private static final int DAY = 24_000;

    private MechanistDialogue() {}

    public static boolean isMechanist(Entity entity) {
        return entity instanceof Villager villager && !villager.isBaby()
                && villager.getVillagerData().profession().is(ModVillagers.DRONE_MECHANIST.getKey());
    }

    /** A right-click on the Mechanist talks to him instead of opening his trades. */
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isMechanist(event.getTarget()) || event.getItemStack().is(Items.NAME_TAG)) {
            return;
        }
        Villager villager = (Villager) event.getTarget();
        if (villager.isTrading() || villager.isSleeping()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {
            talk(player, villager);
        }
    }

    private static void talk(ServerPlayer player, Villager villager) {
        ServerLevel level = (ServerLevel) villager.level();
        if (!villager.hasCustomName()) {
            villager.setCustomName(Component.translatable(KEY + "name." + villager.getRandom().nextInt(NAMES)));
        }
        SupplyTerminalBlockEntity terminal = terminal(villager);
        Lines lines = new Lines(player, villager, terminal);
        List<Choice> choices = List.of(Choice.TRADE, Choice.BYE);
        if (terminal == null) {
            lines.any("no_terminal", 2);
        } else if (!terminal.founded()) {
            if (terminal.hasMet(player.getUUID())) {
                lines.add("return");
            } else {
                terminal.meet(player.getUUID());
                for (int i = 1; i <= 4; i++) {
                    lines.add("intro." + i);
                }
                lines.add("proposal");
            }
            choices = List.of(Choice.ACCEPT, Choice.DECLINE, Choice.DOUBT);
        } else {
            if (!player.getUUID().equals(terminal.owner()) && !terminal.hasMet(player.getUUID())) {
                terminal.meet(player.getUUID());
                lines.any("guest", 2);
            }
            news(lines, terminal);
            if (level.getRaidAt(villager.blockPosition()) != null) {
                lines.add("idle.raid");
            } else if (terminal.canLevelUp()) {
                terminal.raiseLevel(level);
                lines.level = terminal.villageLevel();
                lines.add("level." + lines.level);
                give(player, terminal);
                lines.add("level.build");
                if (lines.level == SupplyTerminalBlockEntity.MAX_LEVEL) {
                    for (int i = 1; i <= 3; i++) {
                        lines.add("finale." + i);
                    }
                }
            } else {
                // A player who missed a level's discs (away, or new to the village) gets their own
                if (give(player, terminal)) {
                    lines.add("discs");
                }
                if (terminal.villageLevel() >= SupplyTerminalBlockEntity.MAX_LEVEL) {
                    lines.any("after", 3);
                } else {
                    lines.building = terminal.missingBuilding();
                    boolean unlearned = player.getInventory().contains(stack -> BlueprintDiscItem.unlearned(stack, player));
                    lines.any(unlearned ? "unlearned" : lines.building != null ? "need_building" : "need_prosperity", 3);
                    idle(lines, level, terminal);
                }
            }
        }
        send(player, villager, lines.lines, choices);
    }

    /** The buildings finished since the last talk: a line for each, its own for some first ones. */
    private static void news(Lines lines, SupplyTerminalBlockEntity terminal) {
        for (Identifier id : terminal.takeNews()) {
            VillagePlan.unlock(id).ifPresent(unlock -> {
                lines.building = unlock.displayName();
                if (unlock.tier() == 1 && VillagePlan.SPECIAL_FIRSTS.contains(unlock.building())) {
                    lines.add("built." + unlock.building());
                } else {
                    lines.any("built", 3);
                }
            });
        }
        lines.building = null;
    }

    /** Hands {@code player} the discs of every level they have not had them for; true if there were any. */
    private static boolean give(ServerPlayer player, SupplyTerminalBlockEntity terminal) {
        List<VillagePlan.Unlock> discs = terminal.takeDiscs(player.getUUID());
        for (VillagePlan.Unlock unlock : discs) {
            ItemStack disc = BlueprintDiscItem.of(unlock);
            if (!player.getInventory().add(disc)) {
                player.drop(disc, false);
            }
        }
        return !discs.isEmpty();
    }

    /** A goodbye for the time of day, or the market. */
    private static void idle(Lines lines, ServerLevel level, SupplyTerminalBlockEntity terminal) {
        long time = level.getOverworldClockTime() % DAY;
        if (terminal.marketDay()) {
            lines.add("idle.market_day");
        } else if (time < 3_000) {
            lines.add("idle.morning");
        } else if (time > 13_000 && time < 23_000) {
            lines.add("idle.night");
        } else {
            lines.any("idle", 4);
        }
    }

    /** The player's answer, or a button at the end of a talk. */
    public static void choose(ServerPlayer player, int entityId, Choice choice) {
        Entity entity = player.level().getEntity(entityId);
        if (!isMechanist(entity) || player.distanceToSqr(entity) > REACH * REACH) {
            return;
        }
        Villager villager = (Villager) entity;
        switch (choice) {
            case TRADE -> villager.mobInteract(player, InteractionHand.MAIN_HAND);
            case BYE -> {}
            case ACCEPT, DECLINE, DOUBT -> {
                SupplyTerminalBlockEntity terminal = terminal(villager);
                if (terminal == null || terminal.founded()) {
                    return;
                }
                Lines lines = new Lines(player, villager, terminal);
                if (choice == Choice.ACCEPT) {
                    terminal.found(player);
                    lines.add("accept.1");
                    lines.add("accept.2");
                    villager.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.0F, 1.0F);
                } else {
                    lines.add(choice == Choice.DECLINE ? "decline" : "doubt");
                }
                send(player, villager, lines.lines, List.of(Choice.TRADE, Choice.BYE));
            }
        }
    }

    private static void send(ServerPlayer player, Villager villager, List<Component> lines, List<Choice> choices) {
        villager.playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 1.0F);
        PacketDistributor.sendToPlayer(player, new MechanistDialoguePayload(villager.getId(), villager.getName(), lines, choices));
    }

    /** The terminal the Mechanist works at, if it still stands. */
    private static @Nullable SupplyTerminalBlockEntity terminal(Villager villager) {
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(site -> site.dimension() == villager.level().dimension() && villager.level().isLoaded(site.pos()))
                .map(site -> villager.level().getBlockEntity(site.pos()) instanceof SupplyTerminalBlockEntity terminal ? terminal : null)
                .orElse(null);
    }

    /** The talk being put together, with the arguments every line takes. */
    private static final class Lines {
        final List<Component> lines = new ArrayList<>();
        final ServerPlayer player;
        final Villager villager;
        final String host;
        int level;
        @Nullable Component building;

        Lines(ServerPlayer player, Villager villager, @Nullable SupplyTerminalBlockEntity terminal) {
            this.player = player;
            this.villager = villager;
            this.host = terminal != null ? terminal.ownerName() : "";
            // Reminders speak of the level the village is working towards
            this.level = terminal != null ? terminal.villageLevel() + 1 : 1;
        }

        void add(String key) {
            lines.add(Component.translatable(KEY + key, player.getName(), level, building != null ? building : Component.empty(),
                    villager.getName(), host));
        }

        /** One of {@code count} variants, {@code key.1} to {@code key.count}, at random. */
        void any(String key, int count) {
            add(key + "." + (1 + villager.getRandom().nextInt(count)));
        }
    }
}
