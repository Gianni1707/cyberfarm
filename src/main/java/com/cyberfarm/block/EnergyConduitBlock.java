package com.cyberfarm.block;

import com.cyberfarm.block.entity.EnergyConduitBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.cyberfarm.port.Caps;

/** Energy Conduit: carries FE between generators and machines of any mod. */
public class EnergyConduitBlock extends NetworkPipeBlock {

    public EnergyConduitBlock(Properties properties, int rate) {
        super(properties, rate);
    }


    @Override
    protected boolean canConnectToMachine(Level level, BlockPos pos, Direction side) {
        return Caps.energy(level, pos, side) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConduitBlockEntity(pos, state);
    }
}
