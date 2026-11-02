package com.cyberfarm.block;

import com.cyberfarm.block.entity.GasPipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.cyberfarm.port.Caps;
import com.cyberfarm.port.FluidResource;

/** Gas Pipe: carries biogas, methane and any other fluid between tanks and machines of any mod. */
public class GasPipeBlock extends NetworkPipeBlock {

    public GasPipeBlock(Properties properties, int rate) {
        super(properties, rate);
    }


    /** Gas Pipes carry what is lighter than air (gases, Steam); Liquid Pipes the rest. */
    public boolean carries(FluidResource resource) {
        return resource.getFluidType().isLighterThanAir();
    }

    @Override
    protected boolean canConnectToMachine(Level level, BlockPos pos, Direction side) {
        return Caps.fluids(level, pos, side) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasPipeBlockEntity(pos, state);
    }
}
