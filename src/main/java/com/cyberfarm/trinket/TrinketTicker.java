package com.cyberfarm.trinket;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.item.GeigerCounterItem;
import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModItems;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * The server side of every trinket, once per player tick: the Gravity Boots switch gravity off, the chip keeps Night
 * Vision and feeds the player, the Pocket Drone pulls drops in and is kept charged from the Energy Sphere, and once a
 * second the sphere pays for what ran. Each write to a trinket is synced to the client, so they are kept rare.
 */
public final class TrinketTicker {
    /** The Gravity Boots' modifier: gravity times zero. The client holds the height (TrinketClient#hover). */
    public static final UUID HOVER = UUID.fromString("7d3c2a8e-5b1f-4c6a-9e2d-1f0b3a4c5d6e");
    private static final AttributeModifier NO_GRAVITY = new AttributeModifier(HOVER, CyberFarm.MODID + ":gravity_boots", -1.0,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    /** +40% walking speed while hovering. */
    public static final double HOVER_SPEED_BONUS = 0.4;
    private static final AttributeModifier HOVER_SPEED = new AttributeModifier(HOVER, CyberFarm.MODID + ":gravity_boots_speed", HOVER_SPEED_BONUS,
            AttributeModifier.Operation.MULTIPLY_BASE);
    /** Step assist while hovering: from the usual 0.6 to 1.1 blocks, so a full block never stops the player (the client glides up it). */
    private static final AttributeModifier HOVER_STEP = new AttributeModifier(HOVER, CyberFarm.MODID + ":gravity_boots_step", 0.5,
            AttributeModifier.Operation.ADDITION);

    /**
     * At run time {@code ServerGamePacketListenerImpl.aboveGroundTickCount}: 1.20.1 kicks a player floating for 4 s
     * whatever their gravity (26.x spares one with none), so a hovering player's count is kept at 0.
     */
    private static final Field ABOVE_GROUND = ObfuscationReflectionHelper.findField(ServerGamePacketListenerImpl.class, "f_9737_");

    public static final double POCKET_DRONE_RANGE = 8.0;
    public static final int FEED_COST = 50;
    private static final float GEIGER_HUD_THRESHOLD = 1.0F;

    private TrinketTicker() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TrinketTicker::onPlayerTick);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
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
            ItemStack sphere = UtilityBeltItem.content(player, UtilityBeltItem.SPHERE);
            if (sphere.getItem() instanceof EnergySphereItem tier) {
                TrinketCharging.chargeFromSphere(player, tier.maxInput() * 20, 20);
            }
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
        // 1.20.1 has Forge's gravity attribute where 26.x has the vanilla one
        AttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity != null && active != (gravity.getModifier(HOVER) != null)) {
            if (active) {
                gravity.addTransientModifier(NO_GRAVITY);
            } else {
                gravity.removeModifier(HOVER);
            }
        }
        // A little faster while hovering (the user's call: without it they only float)
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && active != (speed.getModifier(HOVER) != null)) {
            if (active) {
                speed.addTransientModifier(HOVER_SPEED);
            } else {
                speed.removeModifier(HOVER);
            }
        }
        // 1.20.1 has Forge's step height addition where 26.x has the vanilla attribute
        AttributeInstance step = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (step != null && active != (step.getModifier(HOVER) != null)) {
            if (active) {
                step.addTransientModifier(HOVER_STEP);
            } else {
                step.removeModifier(HOVER);
            }
        }
        if (active) {
            // The field carries the player down: a long drop never hurts
            player.resetFallDistance();
            try {
                ABOVE_GROUND.setInt(player.connection, 0);
            } catch (IllegalAccessException e) {
                // findField made it accessible; nothing else can go wrong here
            }
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
        Item item = stack.getItem();
        FoodProperties food = item.getFoodProperties();
        // 1.20.1 has no consume effects to read: stews, honey and chorus fruit are told apart by their class
        if (food == null || !food.getEffects().isEmpty() || stack.hasCraftingRemainingItem() || item instanceof BowlFoodItem
                || item instanceof HoneyBottleItem || item instanceof ChorusFruitItem) {
            return 0;
        }
        return food.getNutrition();
    }

}
