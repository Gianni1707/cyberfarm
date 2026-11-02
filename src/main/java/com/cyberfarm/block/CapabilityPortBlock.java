package com.cyberfarm.block;

import com.cyberfarm.block.entity.CapabilityPortBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A multiblock frame block that pipes and cables can connect to (see {@link CapabilityPortBlockEntity}). */
public class CapabilityPortBlock extends CyberFrameBlock implements EntityBlock {
    public CapabilityPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CapabilityPortBlockEntity(pos, state);
    }
}
