package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Planter Module: plants seeds on empty farmland in the work area. The seeds come from the dock
 * (where the Harvester drops them), loaded into the cargo before each trip. Never tills soil.
 */
public class PlantJob extends DroneJob {
    public static final int ENERGY_COST = 10;
    /** Seeds loaded from the dock per trip. */
    private static final int SEEDS_PER_TRIP = 64;

    private final BlockPos farmland;

    private PlantJob(BlockPos farmland) {
        this.farmland = farmland;
    }

    /** Seeds, carrots, potatoes... any item that places a crop or a stem. */
    public static boolean isPlantable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item
                && (item.getBlock() instanceof CropBlock || item.getBlock() instanceof StemBlock);
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        boolean hasSeeds = drone.cargo().contains(PlantJob::isPlantable);
        if (!hasSeeds && !canLoadFromDock(drone, dock, PlantJob::isPlantable)) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        BlockPos target = nearest(drone, blocksIn(area), pos -> isEmptyFarmland(level, pos));
        if (target == null) {
            return null;
        }
        if (!hasSeeds) {
            loadFromDock(drone, dock, PlantJob::isPlantable, SEEDS_PER_TRIP);
        }
        return new PlantJob(target);
    }

    private static boolean isEmptyFarmland(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir();
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!isEmptyFarmland(level, farmland)) {
            return Result.DONE;
        }
        return switch (travelTo(drone, farmland)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> plant(drone, level) ? Result.DONE : Result.RETURN;
        };
    }

    /** Plants the first seed in the cargo that can grow here. False when the drone is out of seeds. */
    private boolean plant(DroneEntity drone, ServerLevel level) {
        BlockPos cropPos = farmland.above();
        ItemStack seed = drone.cargo().takeOne(stack -> isPlantable(stack)
                && ((BlockItem) stack.getItem()).getBlock().defaultBlockState().canSurvive(level, cropPos));
        if (seed.isEmpty()) {
            return false;
        }
        Block crop = ((BlockItem) seed.getItem()).getBlock();
        level.setBlock(cropPos, crop.defaultBlockState(), Block.UPDATE_ALL);
        Effects.beam(level, drone, cropPos);
        level.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        drone.useJobEnergy(ENERGY_COST);
        return true;
    }
}
