package com.cyberfarm.drone.job;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Logger Module: fells a whole tree (logs and its natural leaves, so saplings and apples are
 * collected) from the top down, then replants a sapling at the base.
 */
public class LogJob extends DroneJob {
    public static final int LOG_COST = 20;
    public static final int LEAF_COST = 5;
    private static final int MAX_LOGS = 256;
    private static final int MAX_LEAVES = 512;
    /** Leaves further than this from the trunk belong to another tree. */
    private static final int CANOPY_RADIUS = 7;
    private static final int TICKS_PER_BLOCK = 3;

    private final BlockPos base;
    private @Nullable List<BlockPos> remaining;
    /** Where the drone waits while cutting; picked once so it does not drift as leaves go. */
    private @Nullable Vec3 hover;
    private int cooldown;

    private LogJob(BlockPos base) {
        this.base = base;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (drone.isCargoFull()) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        BlockPos base = nearest(drone,
                BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ()),
                pos -> isTreeBase(level, pos));
        return base == null ? null : new LogJob(base);
    }

    /** A log standing on soil: the bottom of a tree (also catches 2x2 trees, one corner at a time). */
    private static boolean isTreeBase(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS) && level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (remaining == null) {
            if (!isTreeBase(level, base)) {
                return Result.DONE;
            }
            if (hover == null) {
                hover = hoverPoint(level);
            }
            return switch (travelTo(drone, base, hover)) {
                case MOVING -> Result.CONTINUE;
                case FAILED -> Result.DONE;
                case ARRIVED -> {
                    remaining = findTree(level);
                    yield Result.CONTINUE;
                }
            };
        }

        // Keep hovering next to the trunk while cutting
        travelTo(drone, base, hover);
        if (--cooldown > 0) {
            return Result.CONTINUE;
        }
        cooldown = TICKS_PER_BLOCK;
        while (!remaining.isEmpty()) {
            BlockPos pos = remaining.removeFirst();
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                cut(drone, level, pos, state);
                return Result.CONTINUE;
            }
        }
        replant(drone, level);
        return drone.isCargoFull() ? Result.RETURN : Result.DONE;
    }

    /**
     * The free spot closest to the trunk, near its base: right beside it if possible, a bit
     * further out when low leaves (spruce, dark oak) are in the way.
     */
    private Vec3 hoverPoint(ServerLevel level) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, 1, -2), base.offset(2, 3, 2))) {
            if ((pos.getX() != base.getX() || pos.getZ() != base.getZ())
                    && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
                double distance = pos.distSqr(base);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best != null ? Vec3.atBottomCenterOf(best).add(0.0, 0.2, 0.0) : Vec3.atCenterOf(base).add(0.0, 1.0, 0.0);
    }

    /** Logs connected to the base (diagonals too, for acacia branches), then their natural leaves; top first. */
    private List<BlockPos> findTree(ServerLevel level) {
        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        logs.add(base);
        queue.add(base);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                if (next.getY() >= base.getY() && !logs.contains(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
                    BlockPos found = next.immutable();
                    logs.add(found);
                    queue.add(found);
                }
            }
        }

        Set<BlockPos> leaves = new HashSet<>();
        queue.addAll(logs);
        while (!queue.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (leaves.contains(next) || logs.contains(next)
                        || Math.abs(next.getX() - base.getX()) > CANOPY_RADIUS || Math.abs(next.getZ() - base.getZ()) > CANOPY_RADIUS) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                // Player-placed leaves are persistent: leave them alone
                if (state.is(BlockTags.LEAVES) && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT))) {
                    leaves.add(next);
                    queue.add(next);
                }
            }
        }

        List<BlockPos> tree = new ArrayList<>(logs);
        tree.addAll(leaves);
        tree.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
        return tree;
    }

    private void cut(DroneEntity drone, ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, drone, drone.miningTool(level));
        level.destroyBlock(pos, false, drone);
        drone.collect(level, pos, drops);
        drone.useJobEnergy(state.is(BlockTags.LOGS) ? LOG_COST : LEAF_COST);
    }

    /** Plants a sapling from the cargo (the tree just dropped some) where the trunk stood. */
    private void replant(DroneEntity drone, ServerLevel level) {
        if (!level.getBlockState(base).isAir()) {
            return;
        }
        ItemStack sapling = drone.takeOneFromCargo(stack -> stack.is(ItemTags.SAPLINGS)
                && stack.getItem() instanceof BlockItem item
                && item.getBlock().defaultBlockState().canSurvive(level, base));
        if (!sapling.isEmpty()) {
            level.setBlock(base, ((BlockItem) sapling.getItem()).getBlock().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, base, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
