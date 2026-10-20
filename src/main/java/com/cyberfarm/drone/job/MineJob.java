package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Quarry Module: digs the surveyed area layer by layer, from the top of the area down to bedrock,
 * keeping only the drops the dock filter lets through.
 * The current layer is stored on the dock. Skips fluids, unbreakable blocks, blocks with an
 * inventory or other block entity, and the column under the dock.
 */
public class MineJob extends DroneJob {
    /** Empty layers skipped per search, to keep each tick cheap. */
    private static final int LAYERS_PER_SEARCH = 4;
    /** Items the drone carries before flying back to unload (three stacks). */
    public static final int LOAD_LIMIT = 192;

    private final BlockPos target;
    private int progress;
    private int shownStage = -1;

    private MineJob(BlockPos target) {
        this.target = target;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        // Never dig the default area around the dock: the player must survey one on purpose.
        if (!dock.hasCustomArea() || isLoadFull(drone)) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        for (int i = 0; i < LAYERS_PER_SEARCH; i++) {
            int y = dock.getQuarryLayer();
            if (y < level.getMinY()) {
                return null;
            }
            BlockPos target = firstInLayer(drone, level, dock, area, y);
            if (target != null) {
                return new MineJob(target);
            }
            dock.setQuarryLayer(y - 1);
        }
        return null;
    }

    /**
     * The next block of the layer in serpentine order: rows along X, alternating direction, and
     * the whole walk reversed on every other layer so the drone continues from where it stopped.
     */
    private static @Nullable BlockPos firstInLayer(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock, BoundingBox area, int y) {
        int width = area.getXSpan();
        int total = width * area.getZSpan();
        boolean reversed = (y & 1) == 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int step = 0; step < total; step++) {
            int index = reversed ? total - 1 - step : step;
            int row = index / width;
            int column = index % width;
            int x = row % 2 == 0 ? area.minX() + column : area.maxX() - column;
            pos.set(x, y, area.minZ() + row);
            if (!drone.isUnreachable(pos) && canMine(level, dock, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean isLoadFull(DroneEntity drone) {
        return drone.cargo().count() >= LOAD_LIMIT || drone.cargo().isFull();
    }

    private static boolean canMine(ServerLevel level, DroneDockBlockEntity dock, BlockPos pos) {
        BlockPos dockPos = dock.getBlockPos();
        if (pos.getX() == dockPos.getX() && pos.getZ() == dockPos.getZ()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0;
    }

    private static int miningTicks(float hardness) {
        return Mth.clamp((int) (5 + hardness * 6), 5, 80);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!canMine(level, dock, target)) {
            stop(drone, level);
            return Result.DONE;
        }
        return switch (travelTo(drone, target)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> dig(drone, level, dock);
        };
    }

    private Result dig(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        int needed = miningTicks(hardness);
        progress++;

        int stage = Math.min(9, progress * 10 / needed);
        if (stage != shownStage) {
            level.destroyBlockProgress(drone.getId(), target, stage);
            shownStage = stage;
        }
        if (progress < needed) {
            return Result.CONTINUE;
        }

        stop(drone, level);
        // A diamond pickaxe, so ores and obsidian drop; the dock filter decides what is kept, the rest is thrown away
        drone.cargo().collect(level, target, breakBlock(drone, level, target, state).stream().filter(dock.filter()::test).toList());
        drone.useJobEnergy(20 + (int) (hardness * 10));
        return isLoadFull(drone) ? Result.RETURN : Result.DONE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        if (shownStage >= 0) {
            level.destroyBlockProgress(drone.getId(), target, -1);
            shownStage = -1;
        }
    }
}
