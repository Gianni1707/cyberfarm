package com.cyberfarm.port.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/** The 26.x light helpers, from LevelRenderer and LightTexture. */
public final class LightCoordsUtil {
    public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private LightCoordsUtil() {}

    public static int getLightCoords(BlockAndTintGetter level, BlockPos pos) {
        return LevelRenderer.getLightColor(level, pos);
    }
}
