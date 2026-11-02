package com.cyberfarm.drone.job;

import net.minecraftforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.cyberfarm.port.Caps;
import com.cyberfarm.port.ItemMoves;
import com.cyberfarm.port.ItemResource;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Transport Module: carries items from the source container to the destination container of the
 * dock's route (set with the Survey Tool), only the items the dock filter lets through and, with a
 * limit set in the filter, only until the destination holds that many of each. Works with any
 * inventory from any mod.
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
        IItemHandler from = inventory(level, source);
        IItemHandler to = inventory(level, destination);
        // Only fly if at least one item could actually go from the source to the destination.
        if (ItemMoves.move(from, to, resource -> dock.filter().test(resource.toStack()) && room(to, resource, dock) > 0, 1, true) == 0) {
            return null;
        }
        return new TransportJob(source, destination);
    }

    private static @Nullable IItemHandler inventory(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) ? Caps.items(level, pos, null) : null;
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
        IItemHandler from = inventory(level, source);
        IItemHandler to = inventory(level, destination);
        // 16 items per trip, plus one stack per Stack Addon; with a limit, no more of each than the destination is short of
        int capacity = drone.transportCapacity();
        int moved = 0;
        for (ItemResource resource : accepted(from, to, dock)) {
            int amount = Math.min(capacity - moved, room(to, resource, dock));
            if (amount > 0) {
                moved += ItemMoves.move(from, drone.cargo().handler(), resource::equals, amount, false);
            }
        }
        if (moved == 0) {
            return Result.DONE;
        }
        phase = Phase.DROPOFF;
        return Result.CONTINUE;
    }

    /**
     * The items in the source that the filter lets through and the destination takes in now, so the
     * drone never loads what it would have to bring back (a Builder's dock wants only the missing blocks).
     */
    private static Set<ItemResource> accepted(@Nullable IItemHandler from, @Nullable IItemHandler to,
            DroneDockBlockEntity dock) {
        Set<ItemResource> accepted = new HashSet<>();
        if (from == null || to == null) {
            return accepted;
        }
        for (int i = 0; i < from.getSlots(); i++) {
            ItemResource resource = ItemResource.of(from.getStackInSlot(i));
            if (resource.isEmpty() || accepted.contains(resource) || !dock.filter().test(resource.toStack()) || room(to, resource, dock) <= 0) {
                continue;
            }
            if (ItemMoves.fits(to, resource)) {
                accepted.add(resource);
            }
        }
        return accepted;
    }

    /** How many more of {@code resource} the destination should get: no end without a limit in the filter. */
    private static int room(@Nullable IItemHandler to, ItemResource resource, DroneDockBlockEntity dock) {
        int limit = dock.filter().limit();
        if (limit <= 0 || to == null) {
            return Integer.MAX_VALUE;
        }
        long held = 0;
        for (int i = 0; i < to.getSlots(); i++) {
            ItemStack stack = to.getStackInSlot(i);
            if (resource.matches(stack)) {
                held += stack.getCount();
            }
        }
        return (int) Math.max(0, limit - held);
    }

    private Result dropOff(DroneEntity drone, ServerLevel level) {
        ItemMoves.move(drone.cargo().handler(), inventory(level, destination), resource -> true, Integer.MAX_VALUE, false);
        // Whatever did not fit goes back to the dock.
        return drone.cargo().isEmpty() ? Result.DONE : Result.RETURN;
    }
}
