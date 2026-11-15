package com.cyberfarm.trinket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.drone.DroneWarp;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

/**
 * The Neural Chip's Taxi Call. With a dock remembered on the chip it calls that dock's Taxi drone; without, the nearest
 * one in {@link #RANGE}. A drone farther than DroneWarp.DISTANCE warps to a little way off and flies the rest; one not
 * loaded (its dock far away) is woken first, its dock's chunk loaded until it answers.
 */
public final class TaxiCall {
    static final double RANGE = 128.0;
    /** Ticks a far dock gets to load its drone before the call gives up. */
    private static final int ANSWER_TICKS = 100;
    /** Where the drone waits for the caller, as TaxiPickupJob: a little above their head. */
    private static final double WAIT_ABOVE = 2.2;

    /** Calls waiting for a far drone to load: who called, which dock, until when. */
    private record Waiting(UUID player, GlobalPos dock, long until) {}

    private static final List<Waiting> WAITING = new ArrayList<>();

    private TaxiCall() {}

    static void call(Player player) {
        if (!NeuralChipItem.runs(player, ChipProgram.TAXI_CALL) || !(player instanceof ServerPlayer server)) {
            return;
        }
        GlobalPos home = ModDataComponents.CHIP_DOCK.get(NeuralChipItem.worn(player));
        if (home == null) {
            nearest(player);
            return;
        }
        ServerLevel level = server.serverLevel();
        if (!home.dimension().equals(level.dimension())) {
            say(player, "other_dimension");
            return;
        }
        BlockPos pos = home.pos();
        // Its block entity knows its drone's modules even when the drone is not loaded
        level.getChunk(pos);
        if (!(level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock)) {
            say(player, "dock_gone");
            return;
        }
        if ((dock.droneTasks() & DroneTask.TAXI.bit()) == 0) {
            say(player, "no_taxi");
            return;
        }
        if (dock.isPaused()) {
            say(player, "paused");
            return;
        }
        DroneEntity drone = dock.loadedDrone(level);
        if (drone != null) {
            answer(server, level, drone);
            return;
        }
        // Far away and not loaded: keep its dock loaded until the drone is there to answer
        WAITING.removeIf(waiting -> waiting.player().equals(player.getUUID()));
        WAITING.add(new Waiting(player.getUUID(), home, level.getGameTime() + ANSWER_TICKS));
        DroneWarp.keep(level, pos);
        say(player, "waking");
    }

    /** The drone sets off: from far away it warps to a little way off first, then flies to the caller. */
    private static void answer(ServerPlayer player, ServerLevel level, DroneEntity drone) {
        String why = drone.taxiCallRefusal();
        if (why != null || !drone.callTaxi(player)) {
            say(player, why != null ? why : "busy");
            return;
        }
        if (drone.distanceTo(player) > DroneWarp.DISTANCE) {
            Vec3 spot = player.position().add(0.0, WAIT_ABOVE, 0.0);
            // If the caller's chunk is not ready yet the pickup job warps a moment later
            DroneWarp.tryWarp(drone, level, spot, spot.y + 3.0);
        }
        say(player, "coming");
    }

    /** Each tick: the far calls whose drone has loaded are answered; those that waited too long give up. */
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WAITING.isEmpty()) {
            return;
        }
        WAITING.removeIf(waiting -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(waiting.player());
            ServerLevel level = event.getServer().getLevel(waiting.dock().dimension());
            if (player == null || level == null) {
                return true;
            }
            DroneEntity drone = level.getBlockEntity(waiting.dock().pos()) instanceof DroneDockBlockEntity dock ? dock.loadedDrone(level) : null;
            if (drone != null) {
                answer(player, level, drone);
                return true;
            }
            if (level.getGameTime() > waiting.until()) {
                say(player, "not_answering");
                return true;
            }
            DroneWarp.keep(level, waiting.dock().pos());
            return false;
        });
    }

    /** Without a chip dock: the nearest Taxi drone in range that can come. */
    private static void nearest(Player player) {
        List<DroneEntity> drones = player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(RANGE));
        drones.sort(Comparator.comparingDouble(player::distanceToSqr));
        if (drones.stream().anyMatch(drone -> drone.callTaxi(player))) {
            say(player, "coming");
            return;
        }
        // None came: say why the nearest Taxi drone could not
        say(player, drones.stream().map(DroneEntity::taxiCallRefusal).filter(reason -> reason != null && !reason.equals("none"))
                .findFirst().orElse("none"));
    }

    private static void say(Player player, String what) {
        player.displayClientMessage(Component.translatable("message.cyberfarm.taxi_call." + what), true);
    }
}
