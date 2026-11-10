package com.cyberfarm.block;


import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** A small multiblock: with a Cyber Frame on its left, as the player sees its front, a long bath cell with a feeder punch (see tools/machinemodels.py; ElectrolyzerRenderer draws what moves). */
public class ElectrolyzerBlock extends SmallMultiblockBlock {

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(1.5, 1, 1.5, 14.5, 16, 14.5));

    public ElectrolyzerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(state.getValue(FACING).getClockWise());
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.electrolyzer.needs_frame";
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The punch shows it works: no particles. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyzerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.ELECTROLYZER.get());
    }
}
