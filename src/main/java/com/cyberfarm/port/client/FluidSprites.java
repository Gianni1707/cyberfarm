package com.cyberfarm.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** A fluid's still texture, which 26.x reads from its fluid model and 1.20.1 from its fluid type. */
public final class FluidSprites {
    private FluidSprites() {}

    public static TextureAtlasSprite still(Fluid fluid) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(IClientFluidTypeExtensions.of(fluid).getStillTexture());
    }
}
