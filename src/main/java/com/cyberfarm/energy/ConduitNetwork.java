package com.cyberfarm.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.NetworkPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Finds the machines reached by a group of connected pipes of one kind (Energy Conduits or Gas Pipes,
 * of any tier). Results are cached by each pipe and thrown away whenever any pipe, or a block next to
 * one, changes.
 */
public final class ConduitNetwork {
    /** Bigger networks are cut off, to keep a single search cheap. */
    public static final int MAX_CONDUITS = 1024;

    private static int version;

    private ConduitNetwork() {}

    /**
     * A machine side the network touches. {@code source}: a pipe arm pulls from it, so the network
     * never sends anything back into it.
     */
    public record Endpoint(BlockPos pos, Direction side, boolean source) {}

    public static int version() {
        return version;
    }

    public static void invalidate() {
        version++;
    }

    /** Walks the pipes of the same block as {@code start} and lists the machine sides they touch. */
    public static List<Endpoint> findEndpoints(Level level, BlockPos start) {
        if (!(level.getBlockState(start).getBlock() instanceof NetworkPipeBlock pipe)) {
            return List.of();
        }
        List<Endpoint> endpoints = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_CONDUITS) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!pipe.sameKind(state)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (!state.getValue(NetworkPipeBlock.CONNECTIONS.get(direction))) {
                    continue;
                }
                BlockPos neighbor = pos.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    continue;
                }
                if (pipe.sameKind(level.getBlockState(neighbor))) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                } else {
                    endpoints.add(new Endpoint(neighbor, direction.getOpposite(), state.getValue(NetworkPipeBlock.EXTRACT.get(direction))));
                }
            }
        }
        return endpoints;
    }
}
