package com.cyberfarm.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** A fluid's still texture and tint, which 26.x reads from its fluid model and 1.20.1 from its fluid type. */
public final class FluidSprites {
    private FluidSprites() {}

    public static TextureAtlasSprite still(Fluid fluid) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(IClientFluidTypeExtensions.of(fluid).getStillTexture());
    }

    /** The ARGB tint of {@code amount} of {@code fluid}, white for an untinted one. */
    public static int tint(Fluid fluid, int amount) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid, Math.max(1, amount)));
    }
}
