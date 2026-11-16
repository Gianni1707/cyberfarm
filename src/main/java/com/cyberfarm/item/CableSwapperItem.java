package com.cyberfarm.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.cyberfarm.block.entity.BatteryBankBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * The Cable Swapper: right-click a cable or pipe and the whole line of it that is joined to the one clicked becomes the
 * next tier up, from the cables in the player's inventory; the old ones go back to the inventory. Sneaking swaps only
 * the one clicked. On a Battery Bank it sets the clicked side's mode. It reads tiers from the block ids, so it works on other mods' cables of the same mod too
 * (CyberFarm's energy_conduit to advanced_energy_conduit, Mekanism's basic_universal_cable to advanced_universal_cable).
 * <p>
 * shortcut: tiers named by a word in the id (basic, advanced, elite...) only; ids like "cable_mk2" are not recognised,
 * add their pattern if a mod people use names its tiers that way.
 */
public class CableSwapperItem extends Item {
    /** The tier words, lowest first. An id without one sits between basic and advanced (CyberFarm's base cables). */
    static final List<String> TIERS = List.of("basic", "advanced", "improved", "reinforced", "hardened", "elite", "ultimate");
    private static final double NO_WORD = 0.5;
    /** Words in an id that make it a cable or pipe; anything else (a machine with tiers, say) is left alone. */
    private static final List<String> CONDUITS = List.of("cable", "pipe", "conduit", "duct", "tube", "wire", "transporter", "conductor");
    /** The most blocks one click swaps. */
    private static final int MAX_LINE = 1024;

    public CableSwapperItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Whether the id names a cable or pipe. */
    static boolean isConduit(Identifier id) {
        for (String word : id.getPath().split("[_/]")) {
            if (CONDUITS.contains(word) || CONDUITS.contains(word.replaceAll("s$", ""))) {
                return true;
            }
        }
        return false;
    }

    /** The next tier's id in the same mod, the first that {@code exists}; null at the top or without tiers. */
    static @Nullable Identifier nextTier(Identifier id, Predicate<Identifier> exists) {
        String[] words = id.getPath().split("_");
        int at = -1;
        for (int i = 0; i < words.length && at < 0; i++) {
            if (TIERS.contains(words[i])) {
                at = i;
            }
        }
        double rank = at < 0 ? NO_WORD : TIERS.indexOf(words[at]);
        for (int t = 0; t < TIERS.size(); t++) {
            if (t <= rank) {
                continue;
            }
            String path;
            if (at < 0) {
                path = TIERS.get(t) + "_" + id.getPath();
            } else {
                String[] swapped = words.clone();
                swapped[at] = TIERS.get(t);
                path = String.join("_", swapped);
            }
            Identifier candidate = Identifier.fromNamespaceAndPath(id.getNamespace(), path);
            if (exists.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Before the block: a cable's own right-click would toggle its pulling arm, a Battery Bank's would open its GUI. On a
     * Battery Bank it sets the clicked side, In, Out and Off in turn (holding the swapper shows every side's mode).
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        boolean bank = context.getLevel().getBlockEntity(clicked) instanceof BatteryBankBlockEntity;
        if (!bank && !isConduit(BuiltInRegistries.BLOCK.getKey(context.getLevel().getBlockState(clicked).getBlock()))) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (level.getBlockEntity(clicked) instanceof BatteryBankBlockEntity battery) {
            Direction face = context.getClickedFace();
            battery.cycle(face);
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.battery_bank.side",
                    Component.translatable("gui.cyberfarm.battery_bank.mode." + battery.mode(face).name().toLowerCase(Locale.ROOT))));
            level.playSound(null, clicked, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        return swapLine(level, player, clicked);
    }

    /** Anything else it is used on is not a cable. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
            context.getPlayer().sendOverlayMessage(Component.translatable("message.cyberfarm.cable_swapper.not_cable"));
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult swapLine(ServerLevel level, Player player, BlockPos clicked) {
        Block from = level.getBlockState(clicked).getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(from);
        Identifier nextId = nextTier(id, BuiltInRegistries.BLOCK::containsKey);
        Block to = nextId == null ? null : BuiltInRegistries.BLOCK.getValue(nextId);
        if (to == null || to.asItem() == Items.AIR) {
            return say(player, Component.translatable("message.cyberfarm.cable_swapper.top"));
        }
        List<BlockPos> line = player.isSecondaryUseActive() ? List.of(clicked) : line(level, clicked, from);
        int swapped = 0;
        for (BlockPos pos : line) {
            if (!player.getAbilities().instabuild && !take(player, to.asItem())) {
                break;
            }
            swap(level, pos, to);
            if (!player.getAbilities().instabuild && !player.getInventory().add(new ItemStack(from.asItem()))) {
                player.drop(new ItemStack(from.asItem()), false);
            }
            swapped++;
        }
        Component name = to.getName();
        if (swapped == 0) {
            return say(player, Component.translatable("message.cyberfarm.cable_swapper.missing", name));
        }
        level.playSound(null, clicked, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.3F);
        player.sendOverlayMessage(swapped < line.size()
                ? Component.translatable("message.cyberfarm.cable_swapper.partial", swapped, line.size(), name)
                : Component.translatable("message.cyberfarm.cable_swapper.swapped", swapped, name));
        return InteractionResult.SUCCESS;
    }

    /** The blocks of the same cable joined to {@code start}, nearest first, up to {@link #MAX_LINE}. */
    private static List<BlockPos> line(ServerLevel level, BlockPos start, Block block) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(List.of(start));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty() && found.size() < MAX_LINE) {
            BlockPos pos = queue.poll();
            found.add(pos);
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (level.isLoaded(next) && seen.add(next) && level.getBlockState(next).is(block)) {
                    queue.add(next);
                }
            }
        }
        return found;
    }

    /** One of {@code item} out of the inventory; false if there is none. */
    private static boolean take(Player player, Item item) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** The new tier in place, keeping every block state setting the two share (connections, which sides pull, water). */
    private static void swap(ServerLevel level, BlockPos pos, Block to) {
        BlockState old = level.getBlockState(pos);
        BlockState state = to.defaultBlockState();
        for (Property<?> property : old.getProperties()) {
            state = copy(old, state, property);
        }
        level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), Block.UPDATE_ALL);
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        for (Property<?> same : to.getProperties()) {
            if (same.getName().equals(property.getName()) && same.getValueClass() == property.getValueClass()) {
                @SuppressWarnings("unchecked")
                Property<T> target = (Property<T>) same;
                T value = from.getValue(property);
                return target.getPossibleValues().contains(value) ? to.setValue(target, value) : to;
            }
        }
        return to;
    }

    private static InteractionResult say(Player player, Component message) {
        player.sendOverlayMessage(message);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cyberfarm.cable_swapper").withStyle(ChatFormatting.GRAY));
    }
}
