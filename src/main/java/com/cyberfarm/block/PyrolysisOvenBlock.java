package com.cyberfarm.block;

import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PyrolysisOvenBlock extends LitMachineBlock {
    public static final MapCodec<PyrolysisOvenBlock> CODEC = simpleCodec(PyrolysisOvenBlock::new);

    public PyrolysisOvenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PyrolysisOvenBlock> codec() {
        return CODEC;
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
