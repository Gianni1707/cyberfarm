package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Laser;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Guard Module: hunts the hostile mobs of the work area (see {@link Laser#isTarget}) and shoots
 * them with its laser from above, out of reach of their swords, then goes after the next one.
 */
public class GuardJob extends DroneJob {
    public static final float DAMAGE = 6.0F;
    public static final int ENERGY_COST = (int) DAMAGE * Laser.ENERGY_PER_DAMAGE;
    public static final int SHOT_INTERVAL = 20;
    /** How far above the work area (and below) it looks for mobs, so flyers and cave openings count too. */
    private static final double VERTICAL_REACH = 8.0;
    /** Hovers this high over the mob's head, and fires from up to {@link #RANGE} away. */
    private static final double HOVER_HEIGHT = 3.0;
    private static final double RANGE = 8.0;

    private final LivingEntity target;
    private int cooldown;

    private GuardJob(LivingEntity target) {
        this.target = target;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area(dock), Laser::isTarget)) {
            double distance = entity.distanceToSqr(drone);
            if (!drone.isUnreachable(entity.blockPosition()) && distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best != null ? new GuardJob(best) : null;
    }

    private static AABB area(DroneDockBlockEntity dock) {
        return AABB.of(dock.getWorkArea()).inflate(0.0, VERTICAL_REACH, 0.0);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        // Dead, gone, or fled the area: on to the next one
        if (target.level() != level || !Laser.isTarget(target) || !area(dock).intersects(target.getBoundingBox())) {
            return Result.DONE;
        }
        Vec3 hover = target.position().add(0.0, target.getBbHeight() + HOVER_HEIGHT, 0.0);
        if (drone.position().distanceTo(target.getEyePosition()) > RANGE || !drone.hasLineOfSight(target)) {
            return travelTo(drone, target.blockPosition(), hover) == Travel.FAILED ? Result.DONE : Result.CONTINUE;
        }
        drone.flyTowards(hover);
        if (--cooldown <= 0) {
            cooldown = SHOT_INTERVAL;
            Laser.fire(level, target, DAMAGE, drone, drone.blockPosition());
            drone.useJobEnergy(ENERGY_COST);
        }
        return Result.CONTINUE;
    }
}
