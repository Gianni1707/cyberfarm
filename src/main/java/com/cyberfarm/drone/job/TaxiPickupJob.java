package com.cyberfarm.drone.job;

import com.cyberfarm.drone.DroneWarp;
import net.minecraft.network.chat.Component;
import java.util.UUID;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A Taxi drone called with the Neural Chip's Taxi Call: it flies to the player and waits beside them. Right-clicking it
 * starts the usual trip to its dock's destination (see DroneEntity#startTaxi); after a minute, or if the player is
 * gone, it flies home.
 */
public class TaxiPickupJob extends DroneJob {
    /** Ticks it waits once beside the caller, and the most it flies to reach them, before going home alone. */
    private static final int WAIT_LIMIT = 600, FLIGHT_LIMIT = 1200;
    /** Close enough to the waiting spot to count as there. */
    private static final double ARRIVED = 2.0;
    /** Where it waits: beside the player, a little above their head, out of the way of their view. */
    private static final Vec3 BESIDE = new Vec3(1.2, 2.2, 0.0);

    private final UUID caller;
    private int flying;
    private int waiting;

    public TaxiPickupJob(UUID caller) {
        this.caller = caller;
    }

    public UUID caller() {
        return caller;
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        Player player = level.getPlayerByUUID(caller);
        if (player == null || player.level() != level) {
            return Result.RETURN;
        }
        Vec3 spot = player.position().add(BESIDE);
        // A long way off: a little flight, then a warp to near the caller
        if (drone.position().distanceTo(spot) > DroneWarp.DISTANCE && flying > DroneWarp.FLY_FIRST) {
            DroneWarp.tryWarp(drone, level, spot, spot.y + 3.0);
        }
        if (drone.position().distanceTo(spot) < ARRIVED ? ++waiting > WAIT_LIMIT : ++flying > FLIGHT_LIMIT) {
            // Not taken in time: home without the caller
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi_call.went_home"));
            return Result.RETURN;
        }
        drone.flyTowards(spot);
        return Result.CONTINUE;
    }
}
