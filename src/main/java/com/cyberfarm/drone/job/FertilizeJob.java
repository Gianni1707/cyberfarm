package com.cyberfarm.drone.job;

import java.util.List;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Fertilizer Module: grows the crops, stems and saplings of the work area with fertilizer from the
 * dock. Any fertilizer works, from any mod (the {@code c:fertilizers} tag, plus bone meal): the
 * drone uses it on the plant as a player would.
 */
public class FertilizeJob extends DroneJob {
    public static final int ENERGY_COST = 10;
    /** Fertilizer loaded from the dock per trip. */
    private static final int FERTILIZER_PER_TRIP = 64;

    private final BlockPos plant;

    private FertilizeJob(BlockPos plant) {
        this.plant = plant;
    }

    public static boolean isFertilizer(ItemStack stack) {
        return stack.is(Tags.Items.FERTILIZERS) || stack.getItem() instanceof BoneMealItem;
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        boolean hasFertilizer = drone.cargo().contains(FertilizeJob::isFertilizer);
        if (!hasFertilizer && !canLoadFromDock(drone, dock, FertilizeJob::isFertilizer)) {
            return null;
        }
        BoundingBox area = dock.getWorkArea();
        BlockPos target = first(drone, area, pos -> canGrow(level, pos));
        if (target == null) {
            return null;
        }
        if (!hasFertilizer) {
            loadFromDock(drone, dock, FertilizeJob::isFertilizer, FERTILIZER_PER_TRIP);
        }
        return new FertilizeJob(target);
    }

    /** Plants that can still grow; grass and moss are left alone, they would only spread flowers. */
    private static boolean canGrow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean plant = state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock || state.getBlock() instanceof SaplingBlock
                || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS);
        return plant && state.getBlock() instanceof BonemealableBlock growable && growable.isValidBonemealTarget(level, pos, state);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (!canGrow(level, plant)) {
            return Result.DONE;
        }
        return switch (travelTo(drone, plant)) {
            case MOVING -> Result.CONTINUE;
            case FAILED -> Result.DONE;
            case ARRIVED -> fertilize(drone, level);
        };
    }

    private Result fertilize(DroneEntity drone, ServerLevel level) {
        ItemStack fertilizer = drone.cargo().takeOne(FertilizeJob::isFertilizer);
        if (fertilizer.isEmpty()) {
            return Result.RETURN;
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, fertilizer);
        fertilizer.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, fertilizer,
                new BlockHitResult(Vec3.atCenterOf(plant), Direction.UP, plant, false)));
        ItemStack left = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!left.isEmpty()) {
            // Not used up: this fertilizer does not work on this plant, skip it until the drone goes home
            drone.cargo().collect(level, plant, List.of(left));
            drone.markUnreachable(plant);
            return Result.DONE;
        }
        drone.useJobEnergy(ENERGY_COST);
        return Result.DONE;
    }
}
