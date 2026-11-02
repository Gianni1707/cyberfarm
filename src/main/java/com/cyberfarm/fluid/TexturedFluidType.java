package com.cyberfarm.fluid;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.cyberfarm.CyberFarm;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

/**
 * A fluid type that names its own textures and tint, which 1.20.1 asks the type for (26.x registers
 * them in a client event). Tanks and recipe viewers of any mod draw the fluid with these.
 */
public class TexturedFluidType extends FluidType {
    private final ResourceLocation still;
    private final ResourceLocation flowing;
    private final @Nullable ResourceLocation overlay;
    private final int tint;

    public TexturedFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing, @Nullable ResourceLocation overlay, int tint) {
        super(properties);
        this.still = still;
        this.flowing = flowing;
        this.overlay = overlay;
        this.tint = tint;
    }

    /** Gases never appear in the world, but are drawn in tanks with their own textures. */
    public static TexturedFluidType gas(Properties properties, String name) {
        return new TexturedFluidType(properties, CyberFarm.id("block/" + name + "_still"), CyberFarm.id("block/" + name + "_flow"), null, 0xFFFFFFFF);
    }

    /** Coolants and acid look like water, animated and see-through, tinted in their own colour. */
    public static TexturedFluidType liquid(Properties properties, int tint) {
        return new TexturedFluidType(properties, new ResourceLocation("block/water_still"), new ResourceLocation("block/water_flow"),
                new ResourceLocation("block/water_overlay"), tint);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public @Nullable ResourceLocation getOverlayTexture() {
                return overlay;
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        });
    }
}
