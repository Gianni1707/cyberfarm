package com.cyberfarm.drone.job;

import java.util.UUID;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Taxi Module: carries the player hanging from the drone's hook to the dock's taxi destination.
 * The route climbs above the terrain first, flies straight, then descends onto the destination.
 */
public class TaxiJob extends DroneJob {
    public static final int TRIP_COST = 50;
    /** Blocks of air kept between the terrain and the passenger's feet while cruising. */
    private static final int CLEARANCE = 4;
    private static final int SLOW_FALL_TICKS = 200;

    private enum Phase { CLIMB, CRUISE, DESCEND }

    private final BlockPos destination;
    private final UUID passenger;
    private final double cruiseY;
    private Phase phase = Phase.CLIMB;

    public TaxiJob(BlockPos destination, UUID passenger, double cruiseY) {
        this.destination = destination;
        this.passenger = passenger;
        this.cruiseY = cruiseY;
    }

    /** Where the drone stops so that the hanging player's feet touch the destination block. */
    public static Vec3 dropOffPoint(BlockPos destination) {
        return Vec3.atBottomCenterOf(destination.above()).add(0.0, DroneEntity.PASSENGER_DROP, 0.0);
    }

    /**
     * Flight altitude between two points: above the highest terrain sampled along the straight
     * line, plus room for a hanging passenger, and never below either end.
     */
    public static double cruiseAltitude(ServerLevel level, Vec3 from, Vec3 to) {
        double highest = Math.max(from.y, to.y);
        double length = from.subtract(to).horizontalDistance();
        int samples = Math.max(1, (int) (length / 4));
        for (int i = 0; i <= samples; i++) {
            Vec3 point = from.lerp(to, (double) i / samples);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(point.x), (int) Math.floor(point.z));
            highest = Math.max(highest, ground + CLEARANCE + DroneEntity.PASSENGER_DROP);
        }
        return Math.min(highest, level.getMaxY() - 2);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        Player player = level.getPlayerByUUID(passenger);
        if (player == null || player.getVehicle() != drone) {
            // The passenger let go mid-flight: soften the landing
            if (player != null && !player.onGround()) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
            }
            return Result.DONE;
        }

        Vec3 dropOff = dropOffPoint(destination);
        switch (phase) {
            case CLIMB -> {
                drone.flyTowards(new Vec3(drone.getX(), cruiseY, drone.getZ()));
                if (Math.abs(drone.getY() - cruiseY) < 0.3) {
                    phase = Phase.CRUISE;
                }
            }
            case CRUISE -> {
                drone.flyTowards(new Vec3(dropOff.x, cruiseY, dropOff.z));
                if (new Vec3(dropOff.x, drone.getY(), dropOff.z).distanceTo(drone.position()) < 0.3) {
                    phase = Phase.DESCEND;
                }
            }
            case DESCEND -> {
                drone.flyTowards(dropOff);
                if (drone.position().distanceTo(dropOff) < 0.15) {
                    player.stopRiding();
                    player.dismountTo(destination.getX() + 0.5, destination.getY() + 1.0, destination.getZ() + 0.5);
                    level.playSound(null, destination.above(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 0.8F);
                    drone.useJobEnergy(TRIP_COST);
                    return Result.DONE;
                }
            }
        }
        return Result.CONTINUE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        // Abandoned with someone still hanging on (battery empty, dock gone...): let go gently
        Player player = level.getPlayerByUUID(passenger);
        if (player != null && player.getVehicle() == drone) {
            player.stopRiding();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
        }
    }
}
