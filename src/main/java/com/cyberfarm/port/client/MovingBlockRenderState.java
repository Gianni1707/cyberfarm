package com.cyberfarm.port.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;

/**
 * The 26.x state of a block drawn away from its place, as a piston moves it. On 1.20.1 the model is lit from the
 * client level at {@link #blockPos} (see {@link SubmitNodeCollector#submitMovingBlock}); biome and light engine
 * are kept only so the 26.x code that fills them in reads the same.
 */
public class MovingBlockRenderState {
    public BlockPos randomSeedPos = BlockPos.ZERO;
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public Holder<Biome> biome;
    public LevelLightEngine lightEngine;
}
