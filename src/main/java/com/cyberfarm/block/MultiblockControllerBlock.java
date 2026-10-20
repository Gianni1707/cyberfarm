package com.cyberfarm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Controller of a multiblock in three sizes (Fission Reactor, Steam Turbine). */
public abstract class MultiblockControllerBlock extends MachineBlock {
    public static final BooleanProperty FORMED = CyberFrameBlock.FORMED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Size while formed: 0 small, 1 medium, 2 large. */
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 2);

    protected MultiblockControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(LIT, false).setValue(SIZE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, LIT, SIZE);
    }
}
