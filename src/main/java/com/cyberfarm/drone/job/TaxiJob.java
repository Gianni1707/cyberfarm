package com.cyberfarm.drone.job;

import java.util.UUID;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.DroneFlight;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Taxi Module: carries the player hanging from the drone's hook to the dock's taxi destination.
 * The route climbs above the terrain first, flies straight, then descends onto the destination.
 */
public class TaxiJob extends DroneJob {
    public static final int TRIP_COST = 50;
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
     * A player clicked the waiting drone: checks there is a destination and enough energy for the
     * trip there and back. Returns the job, or null after telling the player why not.
     */
    public static @Nullable TaxiJob plan(DroneEntity drone, ServerLevel level, Player player, @Nullable BlockPos destination,
            int energy, int flightCost) {
        if (destination == null) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.no_destination"));
            return null;
        }
        Vec3 dropOff = dropOffPoint(destination);
        double cruiseY = DroneFlight.cruiseAltitude(level, drone.position(), dropOff);
        // Up, across and down, there and back again
        double route = (cruiseY - drone.getY()) + dropOff.subtract(drone.position()).horizontalDistance() + (cruiseY - dropOff.y);
        int needed = (int) Math.ceil(2 * route / (drone.cruiseSpeed() * 0.9) * flightCost) + TRIP_COST;
        if (energy < needed) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.low_energy", String.format("%,d", needed)));
            return null;
        }
        return new TaxiJob(destination, player.getUUID(), cruiseY);
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
