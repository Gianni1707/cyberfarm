package com.cyberfarm.drone.job;

import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Rancher Module: shears every shearable mob in the work area (sheep, mooshrooms, any mod's
 * shearable mob) and milks cows and goats with empty buckets from the dock. Wool, milk and the
 * rest go home in the cargo.
 */
public class RancherJob extends DroneJob {
    public static final int ENERGY_COST = 15;
    /** Empty buckets loaded from the dock per trip. */
    private static final int BUCKETS_PER_TRIP = 16;
    /** How close above the animal the drone must be to work on it. */
    private static final double REACH = 1.2;

    private final LivingEntity animal;

    private RancherJob(LivingEntity animal) {
        this.animal = animal;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (drone.isCargoFull()) {
            return null;
        }
        boolean hasBuckets = drone.cargoContains(RancherJob::isBucket);
        // Buckets can only be loaded while the drone sits on its dock
        boolean canMilk = hasBuckets || (drone.isAtDock() && dock.contains(RancherJob::isBucket));
        AABB area = AABB.of(dock.getWorkArea()).inflate(0.0, 2.0, 0.0);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (drone.isUnreachable(entity.blockPosition()) || !(canShear(level, entity) || (canMilk && isMilkable(entity)))) {
                continue;
            }
            double distance = entity.distanceToSqr(drone);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        if (best == null) {
            return null;
        }
        if (!hasBuckets && !canShear(level, best)) {
            ItemStack rest = drone.storeInCargo(dock.withdraw(RancherJob::isBucket, BUCKETS_PER_TRIP));
            if (!rest.isEmpty()) {
                dock.deposit(rest);
            }
        }
        return new RancherJob(best);
    }

    private static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    private static boolean canShear(ServerLevel level, LivingEntity entity) {
        return entity instanceof IShearable shearable && shearable.isShearable(null, new ItemStack(Items.SHEARS), level, entity.blockPosition());
    }

    /** shortcut: only cows, mooshrooms and goats (and mod mobs extending them); upgrade to a mob tag if modded milkables need it. */
    private static boolean isMilkable(LivingEntity entity) {
        return (entity instanceof AbstractCow || entity instanceof Goat) && !entity.isBaby();
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!animal.isAlive() || animal.level() != level) {
            return Result.DONE;
        }
        // The animal wanders: chase the air just above it
        Vec3 hover = animal.position().add(0.0, animal.getBbHeight() + 0.8, 0.0);
        if (drone.position().distanceTo(hover) > REACH) {
            return travelTo(drone, animal.blockPosition(), hover) == Travel.FAILED ? Result.DONE : Result.CONTINUE;
        }
        drone.flyTowards(hover);
        if (canShear(level, animal)) {
            List<ItemStack> drops = ((IShearable) animal).onSheared(null, new ItemStack(Items.SHEARS), level, animal.blockPosition());
            drone.collect(level, animal.blockPosition(), drops);
        } else if (isMilkable(animal)) {
            if (!milk(drone, level)) {
                return Result.DONE;
            }
        } else {
            return Result.DONE;
        }
        drone.useJobEnergy(ENERGY_COST);
        return drone.isCargoFull() ? Result.RETURN : Result.DONE;
    }

    /** Milks with a bucket from the cargo, as a player would, so the animal decides what comes out. */
    private boolean milk(DroneEntity drone, ServerLevel level) {
        ItemStack bucket = drone.takeOneFromCargo(RancherJob::isBucket);
        if (bucket.isEmpty()) {
            return false;
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        animal.interact(player, InteractionHand.MAIN_HAND, animal.position());
        ItemStack result = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        drone.collect(level, animal.blockPosition(), List.of(result));
        if (result.is(Items.BUCKET)) {
            // Still empty: this one gives no milk, leave it alone until the drone goes home
            drone.markUnreachable(animal.blockPosition());
            return false;
        }
        level.playSound(null, animal.blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }
}
