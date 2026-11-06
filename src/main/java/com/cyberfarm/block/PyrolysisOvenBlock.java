package com.cyberfarm.block;

import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
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

/** A retort tube on a low body (the model, see tools/machinemodels.py; PyrolysisOvenRenderer draws what moves). */
public class PyrolysisOvenBlock extends LitMachineBlock {
    public static final MapCodec<PyrolysisOvenBlock> CODEC = simpleCodec(PyrolysisOvenBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(1, 1, 1, 15, 16, 15));

    public PyrolysisOvenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PyrolysisOvenBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The shuttle shows it works: no particles. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PyrolysisOvenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.PYROLYSIS_OVEN.get());
    }
}
