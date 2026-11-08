package com.cyberfarm.drone.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Harvester Module: harvests a mature crop (or melon/pumpkin) and picks ripe plants that aren't crops. It does not replant:
 * that takes a Planter Module, on the same drone or on another.
 */
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
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || isRipePlant(state);
    }

    /**
     * A plant that isn't a crop but ripens, at its last age: a berry bush, or a mod's bush or fruit. It is picked, never broken.
     * Crops and stems age and take bone meal too, but a click does nothing to them: a crop is cut, and a stem's fruit is what ripens.
     */
    private static boolean isRipePlant(BlockState state) {
        return state.getBlock() instanceof BonemealableBlock && !(state.getBlock() instanceof CropBlock) && !(state.getBlock() instanceof StemBlock)
                && state.getBlock().getStateDefinition().getProperty("age") instanceof IntegerProperty age
                && state.getValue(age) == Collections.max(age.getPossibleValues());
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
        if (isRipePlant(state)) {
            pick(drone, level, state);
            return;
        }
        // The tool carries the Fortune / Silk Touch addons into the crop's loot table.
        List<ItemStack> drops = Block.getDrops(state, level, target, null, drone, drone.miningTool(level));
        Effects.beam(level, drone, target);
        level.destroyBlock(target, false, drone);
        drone.cargo().collect(level, target, drops);
        drone.useJobEnergy(ENERGY_COST);
    }

    /** Picks a ripe plant as a player does, by right-clicking it, and gathers what it gave. One that gives nothing is left alone. */
    private void pick(DroneEntity drone, ServerLevel level, BlockState state) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setPos(Vec3.atCenterOf(target));
        InteractionResult result = state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        if (!result.consumesAction() || level.getBlockState(target) == state) {
            drone.markUnreachable(target);
            return;
        }
        List<ItemStack> picked = new ArrayList<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(1.5), item -> item.getAge() == 0)) {
            picked.add(item.getItem());
            item.discard();
        }
        // Some mods hand the harvest straight to the player
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                picked.add(stack);
            }
        }
        Effects.beam(level, drone, target);
        drone.cargo().collect(level, target, picked);
        drone.useJobEnergy(ENERGY_COST);
    }
}
