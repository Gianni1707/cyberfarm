package com.cyberfarm.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.EnergyConduitBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finds the machines reached by a group of connected Energy Conduits. Results are cached by each
 * conduit and thrown away whenever any conduit, or a block next to one, changes.
 */
public final class ConduitNetwork {
    /** Bigger networks are cut off, to keep a single search cheap. */
    public static final int MAX_CONDUITS = 1024;

    private static int version;

    private ConduitNetwork() {}

    /** A machine side that receives energy from the network. */
    public record Endpoint(BlockPos pos, Direction side) {}

    public static int version() {
        return version;
    }

    public static void invalidate() {
        version++;
    }

    /** Walks the conduits connected to {@code start} and lists the machine sides they touch. */
    public static List<Endpoint> findEndpoints(Level level, BlockPos start) {
        List<Endpoint> endpoints = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_CONDUITS) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof EnergyConduitBlock)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (!state.getValue(EnergyConduitBlock.CONNECTIONS.get(direction))) {
                    continue;
                }
                BlockPos neighbor = pos.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    continue;
                }
                if (level.getBlockState(neighbor).getBlock() instanceof EnergyConduitBlock) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                } else {
                    endpoints.add(new Endpoint(neighbor, direction.getOpposite()));
                }
            }
        }
        return endpoints;
    }
}
