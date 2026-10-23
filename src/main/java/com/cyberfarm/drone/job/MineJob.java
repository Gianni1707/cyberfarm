package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    /** Ticks spent getting over the block's column before falling back to plain travel. */
    private static final int CRUISE_LIMIT = 600;

    private final BlockPos target;
    private int progress;
    private int shownStage = -1;
    /** Altitude to cross at when the pit's rim blocks the straight line; NaN when flying straight. */
    private double cruiseY = Double.NaN;
    private boolean routeChecked;
    private int cruiseTicks;

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
            Layer layer = scanLayer(drone, level, dock, area, y);
            if (layer.target != null) {
                return new MineJob(layer.target);
            }
            if (layer.buried) {
                // The layer counter got ahead of the digging (older versions skipped layers they
                // could not reach): start again from the top, empty layers are skipped quickly.
                dock.setQuarryLayer(area.maxY());
                return null;
            }
            if (layer.waiting) {
                // Blocks left that the drone could not reach this trip: retry after it goes home, never skip them
                return null;
            }
            dock.setQuarryLayer(y - 1);
        }
        return null;
    }

    /**
     * What is left to dig in one layer. {@code waiting}: blocks the drone failed to reach this
     * trip. {@code buried}: blocks still covered by undug ground, so the layers above are not done.
     */
    private record Layer(@Nullable BlockPos target, boolean waiting, boolean buried) {
    }

    /**
     * Finds the next block of the layer in serpentine order: rows along X, alternating direction,
     * and the whole walk reversed on every other layer so the drone continues from where it stopped.
     * A block under something the drone never digs (a chest, bedrock) can never be reached and is
     * left behind; one under undug ground means the layers above are not finished.
     */
    private static Layer scanLayer(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock, BoundingBox area, int y) {
        int width = area.getXSpan();
        int total = width * area.getZSpan();
        boolean reversed = (y & 1) == 1;
        boolean waiting = false;
        boolean buried = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int step = 0; step < total; step++) {
            int index = reversed ? total - 1 - step : step;
            int row = index / width;
            int column = index % width;
            int x = row % 2 == 0 ? area.minX() + column : area.maxX() - column;
            pos.set(x, y, area.minZ() + row);
            if (!canMine(level, dock, pos)) {
                continue;
            }
            BlockPos above = pos.above();
            if (canMine(level, dock, above)) {
                buried = true;
            } else if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                continue;
            } else if (drone.isUnreachable(pos)) {
                waiting = true;
            } else {
                return new Layer(pos.immutable(), false, false);
            }
        }
        return new Layer(null, waiting, buried);
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
        if (overColumn(drone, level)) {
            return Result.CONTINUE;
        }
        return switch (travelTo(drone, target)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> dig(drone, level, dock);
        };
    }

    /**
     * Deep in the pit a straight line from the dock cuts through the pit's rim, and the
     * pathfinding fallback reaches only 16 blocks. Then the drone climbs out, crosses over the
     * block's column and drops straight down the dug-out shaft. True while it is on the way.
     */
    private boolean overColumn(DroneEntity drone, ServerLevel level) {
        Vec3 hover = Vec3.atCenterOf(target).add(0.0, 1.0, 0.0);
        if (!routeChecked) {
            routeChecked = true;
            if (!drone.canFlyStraightTo(hover)) {
                double altitude = Math.max(drone.getY(), TaxiJob.cruiseAltitude(level, drone.position(), hover) - DroneEntity.PASSENGER_DROP);
                // Only through open sky: both columns must be clear up to the crossing altitude
                if (drone.canFlyStraightTo(new Vec3(drone.getX(), altitude, drone.getZ()))
                        && level.clip(new ClipContext(hover, new Vec3(hover.x, altitude, hover.z), ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, drone)).getType() == HitResult.Type.MISS) {
                    cruiseY = altitude;
                }
            }
        }
        if (Double.isNaN(cruiseY)) {
            return false;
        }
        if (drone.position().subtract(hover).horizontalDistance() > 0.5 && ++cruiseTicks < CRUISE_LIMIT) {
            drone.flyTowards(drone.getY() < cruiseY - 0.5
                    ? new Vec3(drone.getX(), cruiseY, drone.getZ())
                    : new Vec3(hover.x, cruiseY, hover.z));
            return true;
        }
        cruiseY = Double.NaN;
        return false;
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
