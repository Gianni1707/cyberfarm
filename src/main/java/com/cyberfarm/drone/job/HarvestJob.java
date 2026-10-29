package com.cyberfarm.drone.job;

import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/** Harvester Module: harvests a mature crop (or melon/pumpkin) and replants it. */
public class HarvestJob extends DroneJob {
    public static final int ENERGY_COST = 40;

    private final BlockPos target;

    private HarvestJob(BlockPos target) {
        this.target = target;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (drone.cargo().isFull()) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        BlockPos target = first(drone, area, pos -> isHarvestable(level, pos));
        return target == null ? null : new HarvestJob(target);
    }

    private static boolean isHarvestable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!isHarvestable(level, target)) {
            return Result.DONE;
        }
        return switch (travelTo(drone, target)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> {
                harvest(drone, level);
                yield drone.cargo().isFull() ? Result.RETURN : Result.DONE;
            }
        };
    }

    private void harvest(DroneEntity drone, ServerLevel level) {
        BlockState state = level.getBlockState(target);
        // The tool carries the Fortune / Silk Touch addons into the crop's loot table.
        List<ItemStack> drops = Block.getDrops(state, level, target, null, drone, drone.miningTool(level));

        // Replant with one of the seeds the crop dropped (wheat seeds, carrots, potatoes...).
        BlockState replanted = null;
        if (state.getBlock() instanceof CropBlock crop) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == crop) {
                    drop.shrink(1);
                    replanted = crop.getStateForAge(0);
                    break;
                }
            }
        }

        Effects.beam(level, drone, target);
        level.destroyBlock(target, false, drone);
        if (replanted != null) {
            level.setBlock(target, replanted, Block.UPDATE_ALL);
        }
        drone.cargo().collect(level, target, drops);
        drone.useJobEnergy(ENERGY_COST);
    }
}
