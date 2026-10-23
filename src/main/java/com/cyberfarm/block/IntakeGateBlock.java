package com.cyberfarm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Door in the lower wall of the Methane Extractor's chamber: while the chamber is empty, the
 * extractor pulls in the first monster that comes up to it. OPEN shows the hatch sliding up for a
 * second, then it closes by itself.
 */
public class IntakeGateBlock extends CyberFrameBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final int OPEN_TICKS = 20;

    public IntakeGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    /** Opens the hatch; it closes again after {@link #OPEN_TICKS}. */
    public static void open(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, state.getBlock(), OPEN_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_CLIENTS);
        }
    }
}
