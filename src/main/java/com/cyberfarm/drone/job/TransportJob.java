package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Transport Module: carries items from the source container to the destination container of the
 * dock's route (set with the Survey Tool), only the items the dock filter lets through. Works with
 * any inventory from any mod.
 */
public class TransportJob extends DroneJob {
    private enum Phase { PICKUP, DROPOFF }

    private final BlockPos source;
    private final BlockPos destination;
    private Phase phase = Phase.PICKUP;

    private TransportJob(BlockPos source, BlockPos destination) {
        this.source = source;
        this.destination = destination;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        BlockPos source = dock.getTransportSource();
        BlockPos destination = dock.getTransportDestination();
        if (source == null || destination == null || !drone.cargo().isEmpty()
                || drone.isUnreachable(source) || drone.isUnreachable(destination)) {
            return null;
        }
        ResourceHandler<ItemResource> from = inventory(level, source);
        ResourceHandler<ItemResource> to = inventory(level, destination);
        // Only fly if at least one item could actually go from the source to the destination.
        try (Transaction simulation = Transaction.openRoot()) {
            if (ResourceHandlerUtil.move(from, to, resource -> dock.filter().test(resource.toStack()), 1, simulation) == 0) {
                return null;
            }
        }
        return new TransportJob(source, destination);
    }

    private static @Nullable ResourceHandler<ItemResource> inventory(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.Item.BLOCK, pos, null) : null;
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        BlockPos stop = phase == Phase.PICKUP ? source : destination;
        return switch (travelTo(drone, stop)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> drone.cargo().isEmpty() ? Result.DONE : Result.RETURN;
            case ARRIVED -> phase == Phase.PICKUP ? pickUp(drone, level, dock) : dropOff(drone, level);
        };
    }

    private Result pickUp(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        // 16 items per trip, plus one stack per Stack Addon
        int moved = ResourceHandlerUtil.moveStacking(inventory(level, source), drone.cargo().handler(), resource -> dock.filter().test(resource.toStack()), drone.transportCapacity(), null);
        if (moved == 0) {
            return Result.DONE;
        }
        phase = Phase.DROPOFF;
        return Result.CONTINUE;
    }

    private Result dropOff(DroneEntity drone, ServerLevel level) {
        ResourceHandlerUtil.moveStacking(drone.cargo().handler(), inventory(level, destination), resource -> true, Integer.MAX_VALUE, null);
        // Whatever did not fit goes back to the dock.
        return drone.cargo().isEmpty() ? Result.DONE : Result.RETURN;
    }
}
