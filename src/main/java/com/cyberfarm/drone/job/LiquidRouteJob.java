package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.fluid.GasTank;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Transport Module with a Tank Module: carries a liquid along the dock's route, from the source's tank to the
 * destination's, any liquid and any tank from any mod. What the destination cannot take stays in the drone's
 * tank, and it brings that first on its next trip, once there is room.
 */
public class LiquidRouteJob extends DroneJob {
    private final BlockPos source;
    private final BlockPos destination;
    private boolean loaded;

    private LiquidRouteJob(BlockPos source, BlockPos destination, boolean loaded) {
        this.source = source;
        this.destination = destination;
        this.loaded = loaded;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        BlockPos source = dock.getTransportSource();
        BlockPos destination = dock.getTransportDestination();
        if (!drone.hasTankModule() || source == null || destination == null || drone.isUnreachable(source) || drone.isUnreachable(destination)) {
            return null;
        }
        ResourceHandler<FluidResource> to = tank(level, destination);
        if (to == null) {
            return null;
        }
        GasTank held = drone.liquidTank();
        boolean loaded = held.amount() > 0;
        ResourceHandler<FluidResource> from = loaded ? held : tank(level, source);
        if (from == null) {
            return null;
        }
        // Only fly if some could actually go across
        try (Transaction simulation = Transaction.openRoot()) {
            if (ResourceHandlerUtil.move(from, to, resource -> true, 1, simulation) == 0) {
                return null;
            }
        }
        return new LiquidRouteJob(source, destination, loaded);
    }

    private static @Nullable ResourceHandler<FluidResource> tank(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getCapability(Capabilities.Fluid.BLOCK, pos, null) : null;
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        return switch (travelTo(drone, loaded ? destination : source)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> loaded ? dropOff(drone, level) : pickUp(drone, level);
        };
    }

    private Result pickUp(DroneEntity drone, ServerLevel level) {
        ResourceHandler<FluidResource> from = tank(level, source);
        if (from == null || ResourceHandlerUtil.move(from, drone.liquidTank(), resource -> true, drone.liquidRoom(), null) == 0) {
            return Result.DONE;
        }
        loaded = true;
        return Result.CONTINUE;
    }

    private Result dropOff(DroneEntity drone, ServerLevel level) {
        ResourceHandler<FluidResource> to = tank(level, destination);
        if (to != null) {
            ResourceHandlerUtil.move(drone.liquidTank(), to, resource -> true, Integer.MAX_VALUE, null);
        }
        return drone.liquidTank().amount() == 0 ? Result.DONE : Result.RETURN;
    }
}
