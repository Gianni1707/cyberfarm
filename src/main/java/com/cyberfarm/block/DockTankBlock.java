package com.cyberfarm.block;

import java.util.Map;

import com.cyberfarm.block.entity.DockTankBlockEntity;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dock Tank: placed against a Drone Dock it gives that dock a tank, see {@link DroneDockBlockEntity#dockTank}. Its FACING points
 * at the dock (the model's coupling is on that side); pipes plug in behind it and on top. Right-click to read what is in it.
 */
public class DockTankBlock extends MachineBlock {
    public static final MapCodec<DockTankBlock> CODEC = simpleCodec(DockTankBlock::new);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(1, 0, 0, 15, 13.4, 15), Direction.SOUTH, Block.box(1, 0, 1, 15, 13.4, 16),
            Direction.EAST, Block.box(1, 0, 1, 16, 13.4, 15), Direction.WEST, Block.box(0, 0, 1, 15, 13.4, 15));

    public DockTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DockTankBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    private static boolean isDock(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DroneDockBlockEntity;
    }

    /** Towards a dock beside it, first the one it was placed on; with none, away from the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction clicked = context.getClickedFace().getOpposite();
        BlockPos pos = context.getClickedPos();
        if (clicked.getAxis().isHorizontal() && isDock(context.getLevel(), pos.relative(clicked))) {
            return state.setValue(FACING, clicked);
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (isDock(context.getLevel(), pos.relative(side))) {
                return state.setValue(FACING, side);
            }
        }
        return state.setValue(FACING, context.getHorizontalDirection());
    }

    /** A dock placed beside it later: it turns to face that one, unless it already faces a dock. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction side, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, side, neighborPos, neighbor, random);
        return side.getAxis().isHorizontal() && updated.is(this) && !isDock(level, pos.relative(updated.getValue(FACING))) && isDock(level, neighborPos)
                ? updated.setValue(FACING, side) : updated;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DockTankBlockEntity entity) {
            player.sendOverlayMessage(Component.translatable("gui.cyberfarm.tank",
                    entity.tank().fluid() == Fluids.EMPTY ? Component.translatable("gui.cyberfarm.tank.empty") : entity.tank().fluid().getFluidType().getDescription(),
                    String.format("%,d", entity.tank().amount()), String.format("%,d", entity.tank().capacity())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DockTankBlockEntity(pos, state);
    }
}
