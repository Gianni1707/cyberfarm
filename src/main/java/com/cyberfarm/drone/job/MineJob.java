package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Quarry Module: digs the surveyed area layer by layer, from the top of the area down to bedrock.
 * The current layer is stored on the dock. Skips fluids, unbreakable blocks, blocks with an
 * inventory or other block entity, and the column under the dock.
 */
public class MineJob extends DroneJob {
    /** Empty layers skipped per search, to keep each tick cheap. */
    private static final int LAYERS_PER_SEARCH = 4;

    private final BlockPos target;
    private int progress;
    private int shownStage = -1;

    private MineJob(BlockPos target) {
        this.target = target;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        // Never dig the default area around the dock: the player must survey one on purpose.
        if (!dock.hasCustomArea() || drone.isCargoFull()) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        for (int i = 0; i < LAYERS_PER_SEARCH; i++) {
            int y = dock.getQuarryLayer();
            if (y < level.getMinY()) {
                return null;
            }
            BlockPos target = nearest(drone,
                    BlockPos.betweenClosed(area.minX(), y, area.minZ(), area.maxX(), y, area.maxZ()),
                    pos -> canMine(level, dock, pos));
            if (target != null) {
                return new MineJob(target);
            }
            dock.setQuarryLayer(y - 1);
        }
        return null;
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
            case ARRIVED -> dig(drone, level);
        };
    }

    private Result dig(DroneEntity drone, ServerLevel level) {
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
        // Drops as if mined with a diamond pickaxe, so ores and obsidian drop correctly.
        var drops = Block.getDrops(state, level, target, null, drone, new ItemStack(Items.DIAMOND_PICKAXE));
        level.destroyBlock(target, false, drone);
        drone.collect(level, target, drops);
        drone.useEnergy(20 + (int) (hardness * 10));
        return drone.isCargoFull() ? Result.RETURN : Result.DONE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        if (shownStage >= 0) {
            level.destroyBlockProgress(drone.getId(), target, -1);
            shownStage = -1;
        }
    }
}
