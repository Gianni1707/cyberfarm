package com.cyberfarm.drone.job;

import java.util.function.Predicate;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

    private static final int TRAVEL_TIMEOUT = 200;
    private static final double REACH = 1.8;

    private int travelTicks;

    public abstract Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock);

    /** Called when the job is abandoned before it finished. */
    public void stop(DroneEntity drone, ServerLevel level) {}

    /**
     * Flies to the air just above {@code pos}. ARRIVED once the block is within reach;
     * FAILED after a timeout, and the block is then skipped until the drone goes home.
     */
    protected Travel travelTo(DroneEntity drone, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (drone.position().distanceTo(center) <= REACH) {
            travelTicks = 0;
            return Travel.ARRIVED;
        }
        if (++travelTicks > TRAVEL_TIMEOUT) {
            travelTicks = 0;
            drone.markUnreachable(pos);
            return Travel.FAILED;
        }
        if (travelTicks % 10 == 1) {
            drone.flyTo(center.add(0.0, 1.0, 0.0));
        }
        return Travel.MOVING;
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
