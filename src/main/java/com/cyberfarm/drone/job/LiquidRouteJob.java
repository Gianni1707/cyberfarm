package com.cyberfarm.drone.job;

import net.minecraftforge.fluids.FluidUtil;

import net.minecraftforge.fluids.capability.IFluidHandler;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.fluid.GasTank;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.cyberfarm.port.Caps;
import com.cyberfarm.port.ItemMoves;
import com.cyberfarm.port.FluidResource;
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
        IFluidHandler to = tank(level, destination);
        if (to == null) {
            return null;
        }
        GasTank held = drone.liquidTank();
        boolean loaded = held.amount() > 0;
        IFluidHandler from = loaded ? held : tank(level, source);
        if (from == null) {
            return null;
        }
        // Only fly if some could actually go across
        if (FluidUtil.tryFluidTransfer(to, from, 1, false).isEmpty()) {
            return null;
        }
        return new LiquidRouteJob(source, destination, loaded);
    }

    private static @Nullable IFluidHandler tank(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? Caps.fluids(level, pos, null) : null;
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
        IFluidHandler from = tank(level, source);
        if (from == null || FluidUtil.tryFluidTransfer(drone.liquidTank(), from, drone.liquidRoom(), true).isEmpty()) {
            return Result.DONE;
        }
        loaded = true;
        return Result.CONTINUE;
    }

    private Result dropOff(DroneEntity drone, ServerLevel level) {
        IFluidHandler to = tank(level, destination);
        if (to != null) {
            FluidUtil.tryFluidTransfer(to, drone.liquidTank(), Integer.MAX_VALUE, true);
        }
        return drone.liquidTank().amount() == 0 ? Result.DONE : Result.RETURN;
    }
}
