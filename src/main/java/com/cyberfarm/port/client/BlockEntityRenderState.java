package com.cyberfarm.port.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What a 26.x block entity renderer reads in submit; the light comes from the 1.20.1 render call. */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public @Nullable BlockState blockState;
    public int lightCoords;

    public static void extractBase(BlockEntity entity, BlockEntityRenderState state, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = entity.getBlockPos();
        state.blockState = entity.getBlockState();
    }
}
