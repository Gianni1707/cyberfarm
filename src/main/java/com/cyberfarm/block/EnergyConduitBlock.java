package com.cyberfarm.block;

import com.cyberfarm.block.entity.EnergyConduitBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Energy Conduit: carries FE between generators and machines of any mod. */
public class EnergyConduitBlock extends NetworkPipeBlock {
    private final MapCodec<EnergyConduitBlock> codec = simpleCodec(properties -> new EnergyConduitBlock(properties, rate()));

    public EnergyConduitBlock(Properties properties, int rate) {
        super(properties, rate);
    }

    @Override
    protected MapCodec<EnergyConduitBlock> codec() {
        return codec;
    }

    @Override
    protected boolean canConnectToMachine(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.Energy.BLOCK, pos, side) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConduitBlockEntity(pos, state);
    }
}
