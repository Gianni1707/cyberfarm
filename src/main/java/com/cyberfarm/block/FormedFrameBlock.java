package com.cyberfarm.block;

import com.cyberfarm.block.entity.CapabilityPortBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Cyber Frame. On Forge 1.20.1 only block entities expose capabilities, so a frame gets one once it is part of a
 * formed structure: a formed Crystallizer's taps (floor edges and roof middle) pass things on through it, as on 26.x.
 * Loose frames stay plain blocks.
 */
public class FormedFrameBlock extends CyberFrameBlock implements EntityBlock {
    public FormedFrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(FORMED) ? new CapabilityPortBlockEntity(pos, state) : null;
    }
}
