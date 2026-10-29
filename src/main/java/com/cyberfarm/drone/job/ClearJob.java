package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.multiblock.BuildPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Builder Module, before building: breaks one block in the way of the structure (see
 * {@link BuildPlan#inTheWay}), cutting it like the Quarry does. What it drops falls where the block
 * was.
 */
public class ClearJob extends DroneJob {
    private final BuildPlan plan;
    private final BlockPos target;
    private int progress;
    private int shownStage = -1;

    ClearJob(BuildPlan plan, BlockPos target) {
        this.plan = plan;
        this.target = target;
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!plan.inTheWay(level, target)) {
            stop(drone, level);
            return Result.DONE;
        }
        return switch (travelTo(drone, target, Vec3.atCenterOf(target).add(0.0, 1.0, 0.0), BuildJob.REACH)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> {
                stop(drone, level);
                yield Result.DONE;
            }
            case ARRIVED -> cut(drone, level);
        };
    }

    private Result cut(DroneEntity drone, ServerLevel level) {
        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        int needed = Mth.clamp((int) (5 + hardness * 6), 5, 80);
        if (++progress % 6 == 1) {
            Effects.beam(level, drone, target);
        }
        int stage = Math.min(9, progress * 10 / needed);
        if (stage != shownStage) {
            level.destroyBlockProgress(drone.getId(), target, stage);
            shownStage = stage;
        }
        if (progress < needed) {
            return Result.CONTINUE;
        }
        stop(drone, level);
        breakBlock(drone, level, target, state).forEach(stack -> Block.popResource(level, target, stack));
        drone.useJobEnergy(20 + (int) (hardness * 10));
        return Result.DONE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        plan.release(target);
        if (shownStage >= 0) {
            level.destroyBlockProgress(drone.getId(), target, -1);
            shownStage = -1;
        }
    }
}
