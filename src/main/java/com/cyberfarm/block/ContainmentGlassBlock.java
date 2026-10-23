package com.cyberfarm.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Wall of the Methane Extractor's chamber: see-through glass, hidden while the chamber is drawn whole. */
public class ContainmentGlassBlock extends TransparentBlock {
    public static final MapCodec<ContainmentGlassBlock> CODEC = simpleCodec(ContainmentGlassBlock::new);
    public static final BooleanProperty FORMED = MethaneExtractorBlock.FORMED;

    public ContainmentGlassBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public MapCodec<ContainmentGlassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
