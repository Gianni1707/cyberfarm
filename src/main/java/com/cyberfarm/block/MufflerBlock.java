package com.cyberfarm.block;

import com.cyberfarm.block.entity.MufflerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Right-click opens the GUI where the player picks what the Muffler silences. */
public class MufflerBlock extends MachineBlock {

    public MufflerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MufflerBlockEntity(pos, state);
    }
}
