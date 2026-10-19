package com.cyberfarm.block;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Pipe for liquids: water, lava, coolants, any mod's liquid. Same network logic as the Gas Pipe,
 * but a block of its own, so a water line and a steam line can run side by side without joining.
 */
public class LiquidPipeBlock extends GasPipeBlock {
    private final MapCodec<LiquidPipeBlock> codec = simpleCodec(properties -> new LiquidPipeBlock(properties, rate()));

    public LiquidPipeBlock(Properties properties, int rate) {
        super(properties, rate);
    }

    @Override
    protected MapCodec<LiquidPipeBlock> codec() {
        return codec;
    }

    @Override
    public boolean carries(FluidResource resource) {
        return !resource.getFluidType().isLighterThanAir();
    }
}
