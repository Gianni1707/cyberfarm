package com.cyberfarm.block;

import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Right-click opens the GUI where the player picks what the Muffler silences. */
public class MufflerBlock extends MachineBlock {
    public static final MapCodec<MufflerBlock> CODEC = simpleCodec(MufflerBlock::new);

    public MufflerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MufflerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MufflerBlockEntity(pos, state);
    }
}
