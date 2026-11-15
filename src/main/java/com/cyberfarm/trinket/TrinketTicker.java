package com.cyberfarm.trinket;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.item.GeigerCounterItem;
import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The server side of every trinket, once per player tick: the Gravity Boots switch gravity off, the chip keeps Night
 * Vision and feeds the player, the Pocket Drone pulls drops in and is kept charged from the Energy Sphere, and once a
 * second the sphere pays for what ran. Each write to a trinket is synced to the client, so they are kept rare.
 */
public final class TrinketTicker {
    /** The Gravity Boots' modifier: gravity times zero. The client holds the height (TrinketClient#hover). */
    public static final Identifier HOVER = CyberFarm.id("gravity_boots");
    private static final AttributeModifier NO_GRAVITY = new AttributeModifier(HOVER, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public static final double POCKET_DRONE_RANGE = 8.0;
    public static final int FEED_COST = 50;
    private static final float GEIGER_HUD_THRESHOLD = 1.0F;

    private TrinketTicker() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TrinketTicker::onPlayerTick);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean hovering = hover(player);
        ItemStack drone = UtilityBeltItem.content(player, UtilityBeltItem.DRONE);
        boolean flying = PocketDroneItem.isActive(drone) && PocketDroneItem.energy(drone) > 0;
        DroneFetch.tick(player, level, flying);
        if (NeuralChipItem.runs(player, ChipProgram.GEIGER_HUD) && !hasGeigerCounter(player)) {
            float radiation = Radiation.level(level, player.blockPosition());
            // Only where there is something to report, so the hotbar line stays free elsewhere
            if (radiation >= GEIGER_HUD_THRESHOLD) {
                GeigerCounterItem.report(player, level, radiation);
            }
        }
        if (player.tickCount % 20 == 0) {
            ItemStack chip = NeuralChipItem.worn(player);
            nightVision(player, chip);
            int cost = hovering ? GravityBootsItem.COST_PER_TICK : 0;
            // Night Vision costs while it is switched on, the Geiger HUD while it has something to report
            boolean geigerReports = Radiation.level(level, player.blockPosition()) >= GEIGER_HUD_THRESHOLD && !hasGeigerCounter(player);
            for (ChipProgram program : NeuralChipItem.programs(chip)) {
                if ((program != ChipProgram.NIGHT_VISION || NeuralChipItem.isOn(chip, program)) && (program != ChipProgram.GEIGER_HUD || geigerReports)) {
                    cost += program.costPerTick();
                }
            }
            int fed = NeuralChipItem.runs(player, ChipProgram.AUTO_FEEDER) && feed(player) ? FEED_COST : 0;
            UtilityBeltItem.use(player, cost * 20 + fed);
            topUpDrone(player, drone, flying);
        }
    }

    /**
     * Once a second: a flying drone pays its own battery, and the sphere tops the battery up whether the drone flies or
     * not. Switched off when the battery runs out.
     */
    private static void topUpDrone(ServerPlayer player, ItemStack drone, boolean flying) {
        if (drone.isEmpty()) {
            return;
        }
        int energy = PocketDroneItem.energy(drone) - (flying ? PocketDroneItem.COST_PER_TICK * 20 : 0);
        int wanted = Math.min(PocketDroneItem.CHARGE_PER_TICK * 20, PocketDroneItem.CAPACITY - Math.max(0, energy));
        int charged = wanted > 0 ? UtilityBeltItem.use(player, wanted) : 0;
        int next = Math.max(0, energy + charged);
        if (next != PocketDroneItem.energy(drone) || (next == 0 && PocketDroneItem.isActive(drone))) {
            boolean active = PocketDroneItem.isActive(drone) && next > 0;
            UtilityBeltItem.update(player, UtilityBeltItem.DRONE, stack -> PocketDroneItem.withActive(PocketDroneItem.withEnergy(stack, next), active));
        }
    }

    private static boolean hasGeigerCounter(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.GEIGER_COUNTER.get()) || player.getOffhandItem().is(ModItems.GEIGER_COUNTER.get());
    }

    /** Gravity on or off for the Gravity Boots. Returns whether the player hovers. */
    private static boolean hover(ServerPlayer player) {
        ItemStack boots = Trinkets.worn(player, TrinketType.FEET, GravityBootsItem.class);
        boolean active = !boots.isEmpty() && GravityBootsItem.height(boots) > 0 && UtilityBeltItem.energy(player) > 0
                && !player.getAbilities().flying && !player.isFallFlying() && !player.isPassenger() && !player.isSpectator()
                && !player.isSleeping();
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null && active != gravity.hasModifier(HOVER)) {
            if (active) {
                gravity.addTransientModifier(NO_GRAVITY);
            } else {
                gravity.removeModifier(HOVER);
            }
        }
        if (active) {
            // The field carries the player down: a long drop never hurts
            player.resetFallDistance();
        }
        return active;
    }

    /**
     * Night Vision for as long as the program is on, with no particles and no icon, so it never blinks out the way a
     * running-out potion does. A potion's own Night Vision is left alone.
     */
    private static void nightVision(ServerPlayer player, ItemStack chip) {
        boolean wanted = NeuralChipItem.isOn(chip, ChipProgram.NIGHT_VISION) && UtilityBeltItem.energy(player) > 0;
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        boolean ours = current != null && current.isInfiniteDuration() && !current.isVisible() && !current.showIcon();
        if (wanted && current == null) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
        } else if (!wanted && ours) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /**
     * Auto-Feeder: when the player is hungry enough, eats the food from the inventory that fills the most without
     * wasting any. Food with side effects (rotten flesh, golden apples...) is never touched. Returns whether it ate.
     * shortcut: food that leaves something behind (stews, honey) is skipped too; give back the remainder to allow it.
     */
    static boolean feed(ServerPlayer player) {
        int missing = 20 - player.getFoodData().getFoodLevel();
        Inventory inventory = player.getInventory();
        int best = -1;
        int bestNutrition = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            int nutrition = plainNutrition(inventory.getItem(i));
            if (nutrition > bestNutrition && nutrition <= missing) {
                best = i;
                bestNutrition = nutrition;
            }
        }
        if (best < 0) {
            return false;
        }
        ItemStack food = inventory.getItem(best);
        inventory.setItem(best, food.finishUsingItem(player.level(), player));
        return true;
    }

    /** The nutrition of a food with no side effects and nothing left behind; 0 for anything else. */
    static int plainNutrition(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (food == null || consumable == null || !consumable.onConsumeEffects().isEmpty() || stack.has(DataComponents.USE_REMAINDER)) {
            return 0;
        }
        return food.nutrition();
    }

}
