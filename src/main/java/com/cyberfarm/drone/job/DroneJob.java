package com.cyberfarm.drone.job;

import java.util.List;
import java.util.function.Predicate;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** One piece of work a drone does away from its dock: harvest a crop, mine a block, carry a load. */
public abstract class DroneJob {
    public enum Result {
        /** Still working on it. */
        CONTINUE,
        /** Finished: the drone looks for the next job of the same kind. */
        DONE,
        /** The drone must go back to the dock (cargo full, nowhere to put items...). */
        RETURN
    }

    protected enum Travel { MOVING, ARRIVED, FAILED }

    /** Ticks without getting closer before the drone gives up on a block. */
    private static final int NO_PROGRESS_LIMIT = 100;
    private static final double ARRIVAL_DISTANCE = 0.3;

    private double bestDistance = Double.MAX_VALUE;
    private int noProgressTicks;

    public abstract Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock);

    /** Called when the job is abandoned before it finished. */
    public void stop(DroneEntity drone, ServerLevel level) {}

    /**
     * Flies to the air just above {@code pos} and keeps hovering there; call it every tick, also
     * while working on the block. FAILED when the drone stops getting closer for a while: the
     * block is then skipped until the drone goes home.
     */
    protected Travel travelTo(DroneEntity drone, BlockPos pos) {
        return travelTo(drone, pos, Vec3.atCenterOf(pos).add(0.0, 1.0, 0.0));
    }

    /** Like {@link #travelTo(DroneEntity, BlockPos)}, hovering at {@code hover} instead of above the block. */
    protected Travel travelTo(DroneEntity drone, BlockPos pos, Vec3 hover) {
        drone.flyTowards(hover);
        double distance = drone.position().distanceTo(hover);
        if (distance <= ARRIVAL_DISTANCE) {
            noProgressTicks = 0;
            return Travel.ARRIVED;
        }
        if (distance < bestDistance - 0.25) {
            bestDistance = distance;
            noProgressTicks = 0;
        } else if (++noProgressTicks > NO_PROGRESS_LIMIT) {
            drone.markUnreachable(pos);
            return Travel.FAILED;
        }
        return Travel.MOVING;
    }

    /** Every block of the work area. */
    protected static Iterable<BlockPos> blocksIn(BoundingBox area) {
        return BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
    }

    /** Whether the drone can load {@code supply} now: only while it sits on its dock, which has some. */
    protected static boolean canLoadFromDock(DroneEntity drone, DroneDockBlockEntity dock, Predicate<ItemStack> supply) {
        return drone.isAtDock() && dock.contains(supply);
    }

    /** Loads up to {@code max} items of {@code supply} from the dock; what does not fit in the cargo goes back. */
    protected static void loadFromDock(DroneEntity drone, DroneDockBlockEntity dock, Predicate<ItemStack> supply, int max) {
        dock.deposit(drone.cargo().store(dock.withdraw(supply, max)));
    }

    /** Breaks the block as if mined with the drone's tool (and its Fortune / Silk Touch addons); returns the drops. */
    protected static List<ItemStack> breakBlock(DroneEntity drone, ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, drone, drone.miningTool(level));
        Effects.beam(level, drone, pos);
        level.destroyBlock(pos, false, drone);
        return drops;
    }

    /** The position closest to the drone that passes the test, skipping blocks it could not reach. */
    protected static @Nullable BlockPos nearest(DroneEntity drone, Iterable<BlockPos> positions, Predicate<BlockPos> test) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : positions) {
            if (!drone.isUnreachable(pos) && test.test(pos)) {
                double distance = pos.distToCenterSqr(drone.position());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }
}
