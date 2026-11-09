package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sponge Module: flies to the highest source of liquid in the work area and sucks up the 3x3 around
 * it. Alone it throws the liquid away on the spot. With a Tank Module on the drone it keeps it, one kind at a
 * time: holding one, it goes for no other until it has emptied at the dock. A pool is followed down under the area to its floor, one layer at a time. Only sources are looked
 * for: currents dry up by themselves once theirs is gone, so a stream running in from outside the area
 * does not keep the drone busy for ever.
 */
public class SpongeJob extends DroneJob {
    public static final int ENERGY_COST = 40;
    private static final int RADIUS = 1;
    private static final int SUCK_TICKS = 24;
    /** How far under the area a pool is followed. */
    private static final int MAX_DEPTH = 32;
    private static final int SOURCE = 1000;
    /** How long a drop of liquid takes to rise to the hood. */
    private static final float RISE_TICKS = 8.0F;

    private final BlockPos target;
    private final Fluid liquid;
    private int ticks;

    private SpongeJob(BlockPos target, Fluid liquid) {
        this.target = target;
        this.liquid = liquid;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        // Never the default area around the dock: draining a lake must be asked for on purpose
        if (!dock.hasCustomArea() || drone.liquidRoom() < SOURCE) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        int bottom = Math.max(level.getMinY(), area.minY() - MAX_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        Fluid bestLiquid = null;
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                // Down each column to the first source it may take or the first thing that is solid and dry
                for (int y = area.maxY(); y >= bottom && (best == null || y > best.getY()); y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    FluidState fluid = state.getFluidState();
                    if (fluid.isEmpty() && state.blocksMotion()) {
                        break;
                    }
                    if (fluid.isSource() && !drone.isUnreachable(pos) && drone.canHoldLiquid(fluid.getType())) {
                        best = pos.immutable();
                        bestLiquid = fluid.getType();
                        break;
                    }
                }
            }
        }
        return best != null ? new SpongeJob(best, bestLiquid) : null;
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!level.getFluidState(target).getType().isSame(liquid)) {
            return Result.DONE;
        }
        return switch (travelTo(drone, target)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> suck(drone, level);
        };
    }

    private Result suck(DroneEntity drone, ServerLevel level) {
        ticks++;
        rise(drone, level);
        if (ticks < SUCK_TICKS) {
            return Result.CONTINUE;
        }
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-RADIUS, 0, -RADIUS), target.offset(RADIUS, 0, RADIUS))) {
            drain(drone, level, pos);
        }
        boolean lava = liquid.is(FluidTags.LAVA);
        level.playSound(null, target, lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.SPONGE_ABSORB, SoundSource.NEUTRAL, 0.7F, 1.0F);
        drone.useJobEnergy(ENERGY_COST);
        return drone.liquidRoom() < SOURCE ? Result.RETURN : Result.DONE;
    }

    /** Drops of the liquid spiralling up from the surface into the hood. Dust moves at a tenth of what it is told. */
    private void rise(DroneEntity drone, ServerLevel level) {
        DustParticleOptions drop = new DustParticleOptions(liquid.defaultFluidState().createLegacyBlock().getMapColor(level, target).col, 1.0F);
        Vec3 hood = drone.position();
        for (int i = 0; i < 2; i++) {
            float angle = ticks * 0.7F + i * Mth.PI;
            Vec3 from = new Vec3(target.getX() + 0.5 + Mth.cos(angle) * 1.2, target.getY() + 1.0, target.getZ() + 0.5 + Mth.sin(angle) * 1.2);
            Vec3 move = hood.subtract(from).scale(10.0 / RISE_TICKS);
            level.sendParticles(drop, from.x, from.y, from.z, 0, move.x, move.y, move.z, 1.0);
        }
    }

    /** As the vanilla Sponge does it, for any liquid: water plants break and drop, waterlogged blocks just dry. Currents are free, sources fill the Tank Module. */
    private void drain(DroneEntity drone, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!fluid.getType().isSame(liquid) || fluid.isSource() && drone.liquidRoom() < SOURCE) {
            return;
        }
        boolean source = fluid.isSource();
        if (!(state.getBlock() instanceof BucketPickup pickup) || pickup.pickupBlock(null, level, pos, state).isEmpty()) {
            if (!(state.getBlock() instanceof LiquidBlock)) {
                if (!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT) && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS)) {
                    return;
                }
                Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (source) {
            drone.holdLiquid(liquid, SOURCE);
        }
    }
}
