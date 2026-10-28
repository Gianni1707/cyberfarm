package com.cyberfarm.drone.job;

import java.util.Map;
import java.util.WeakHashMap;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Laser;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bomb Module (Mini Drones only): dives at a hostile mob of the work area (see {@link Laser#isTarget})
 * and blows itself up on it, a small TNT blast. The drone is gone: it is a one-way trip.
 */
public class BombJob extends DroneJob {
    /** A third of TNT's: kills a zombie, digs a small crater. */
    public static final float POWER = 1.5F;
    private static final double VERTICAL_REACH = 8.0;
    /** Close enough to the mob's middle to go off. */
    private static final double FUSE_DISTANCE = 1.2;
    /** A claim on a mob lasts this long after its drone last flew at it (a drone that was unloaded lets go). */
    private static final int CLAIM_TICKS = 20;

    // shortcut: claims are per server, in memory; fine as jobs are not saved either
    private static final Map<LivingEntity, BombJob> CLAIMS = new WeakHashMap<>();

    private final LivingEntity target;
    private long lastTick;
    private boolean armed;

    private BombJob(LivingEntity target, long now) {
        this.target = target;
        this.lastTick = now;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        long now = level.getGameTime();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area(dock), Laser::isTarget)) {
            double distance = entity.distanceToSqr(drone);
            if (!drone.isUnreachable(entity.blockPosition()) && !isClaimed(entity, now) && distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        if (best == null) {
            return null;
        }
        // One bomb per mob: the rest of the swarm picks other targets
        BombJob job = new BombJob(best, now);
        CLAIMS.put(best, job);
        return job;
    }

    private static boolean isClaimed(LivingEntity entity, long now) {
        BombJob claim = CLAIMS.get(entity);
        return claim != null && now - claim.lastTick <= CLAIM_TICKS;
    }

    private static AABB area(DroneDockBlockEntity dock) {
        return AABB.of(dock.getWorkArea()).inflate(0.0, VERTICAL_REACH, 0.0);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        lastTick = level.getGameTime();
        if (target.level() != level || !Laser.isTarget(target) || !area(dock).intersects(target.getBoundingBox())) {
            CLAIMS.remove(target, this);
            return Result.DONE;
        }
        if (!armed) {
            armed = true;
            level.playSound(null, drone.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
        Vec3 aim = target.getBoundingBox().getCenter();
        if (drone.position().distanceTo(aim) <= FUSE_DISTANCE) {
            CLAIMS.remove(target, this);
            drone.detonate(level, POWER);
            return Result.CONTINUE;
        }
        if (travelTo(drone, target.blockPosition(), aim) == Travel.FAILED) {
            CLAIMS.remove(target, this);
            return Result.DONE;
        }
        return Result.CONTINUE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        CLAIMS.remove(target, this);
    }
}
