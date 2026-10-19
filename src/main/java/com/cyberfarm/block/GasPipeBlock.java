package com.cyberfarm.block;

import com.cyberfarm.block.entity.GasPipeBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Gas Pipe: carries biogas, methane and any other fluid between tanks and machines of any mod. */
public class GasPipeBlock extends NetworkPipeBlock {
    public static final MapCodec<GasPipeBlock> CODEC = simpleCodec(GasPipeBlock::new);

    public GasPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GasPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canConnectToMachine(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, side) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasPipeBlockEntity(pos, state);
    }
}
