package com.cyberfarm.block;


import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * A small multiblock: with a Cyber Frame in front and one behind it assembles into a 1x3 engine, its duct and nozzle sliding
 * out and the rotor blooming (see tools/machinemodels.py; GasTurbineRenderer draws what moves).
 */
public class GasTurbineBlock extends SmallMultiblockBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(0.5, 1, 0.5, 15.5, 16, 15.5));

    public GasTurbineBlock(Properties properties) {
        super(properties);
    }


    @Override
    public List<Direction> frameSides(BlockState state) {
        Direction front = state.getValue(FACING);
        return List.of(front, front.getOpposite());
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.gas_turbine.needs_frames";
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTurbineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.GAS_TURBINE.get());
    }
}
