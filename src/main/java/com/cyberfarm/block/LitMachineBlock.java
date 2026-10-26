package com.cyberfarm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A machine whose front lights up and animates while it works: {@link #LIT} follows the machine's
 * status (see {@code MachineBlockEntity.tickStatus}), and its model swaps to the working one.
 */
public abstract class LitMachineBlock extends MachineBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected LitMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** A working machine lights up its surroundings a little. */
    public static int lightLevel(BlockState state) {
        return state.getValue(LIT) ? 6 : 0;
    }
}
