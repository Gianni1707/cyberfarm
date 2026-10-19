package com.cyberfarm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Structural block of multiblocks; lights up while it is part of a formed structure. */
public class CyberFrameBlock extends Block {
    public static final BooleanProperty FORMED = MethaneExtractorBlock.FORMED;

    public CyberFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
