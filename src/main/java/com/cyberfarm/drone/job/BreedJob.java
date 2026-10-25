package com.cyberfarm.drone.job;

import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Breeder Module: feeds the animals of the work area with their breeding food from the dock, two
 * of a kind, so they make babies. It stops at a herd of {@link #MAX_HERD} of a kind, so farms do
 * not grow without end. The food is whatever the animal eats, modded animals and foods included.
 */
public class BreedJob extends DroneJob {
    public static final int ENERGY_COST = 15;
    /** Animals of one kind in the work area, babies included, past which the drone stops breeding them. */
    public static final int MAX_HERD = 16;
    /** Food loaded from the dock per trip. */
    private static final int FOOD_PER_TRIP = 16;
    /** How close above the animal the drone must be to feed it. */
    private static final double REACH = 1.2;

    private final Animal animal;

    private BreedJob(Animal animal) {
        this.animal = animal;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        AABB area = AABB.of(dock.getWorkArea()).inflate(0.0, 2.0, 0.0);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area, Animal::isAlive);
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Animal candidate : animals) {
            if (!ready(candidate) || drone.isUnreachable(candidate.blockPosition()) || !hasFood(drone, dock, candidate)) {
                continue;
            }
            // A partner: another of its kind ready or already in love, and room in the herd
            List<Animal> kind = animals.stream().filter(other -> other.getType() == candidate.getType()).toList();
            boolean partner = kind.stream().anyMatch(other -> other != candidate && (ready(other) || other.isInLove()));
            double distance = candidate.distanceToSqr(drone);
            if (partner && kind.size() < MAX_HERD && distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        Animal target = best;
        if (!drone.cargo().contains(target::isFood)) {
            loadFromDock(drone, dock, target::isFood, FOOD_PER_TRIP);
        }
        return new BreedJob(target);
    }

    /** An adult that would fall in love if fed now. */
    private static boolean ready(Animal animal) {
        return !animal.isBaby() && animal.canFallInLove() && animal.getAge() == 0;
    }

    private static boolean hasFood(DroneEntity drone, DroneDockBlockEntity dock, Animal animal) {
        return drone.cargo().contains(animal::isFood) || canLoadFromDock(drone, dock, animal::isFood);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!animal.isAlive() || animal.level() != level || !ready(animal)) {
            return Result.DONE;
        }
        // The animal wanders: chase the air just above it
        Vec3 hover = animal.position().add(0.0, animal.getBbHeight() + 0.8, 0.0);
        if (drone.position().distanceTo(hover) > REACH) {
            return travelTo(drone, animal.blockPosition(), hover) == Travel.FAILED ? Result.DONE : Result.CONTINUE;
        }
        drone.flyTowards(hover);
        ItemStack food = drone.cargo().takeOne(animal::isFood);
        if (food.isEmpty()) {
            return Result.RETURN;
        }
        animal.setInLove(null);
        level.playSound(null, animal.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6F, 1.0F);
        level.sendParticles(ParticleTypes.HEART, animal.getX(), animal.getY() + animal.getBbHeight(), animal.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        drone.useJobEnergy(ENERGY_COST);
        return Result.DONE;
    }
}
