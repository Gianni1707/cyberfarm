package com.cyberfarm.port.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The 26.x RenderTypes factory on 1.20.1, matched by how 26.x draws them rather than by name: 26.x entity cutout draws
 * both sides of a face (1.20.1's culls the back ones, which leaves the machines' custom shells open), and 26.x eyes blend
 * by alpha (1.20.1's add their colour, which turns a faint light cone into solid white).
 */
public final class RenderTypes {
    private RenderTypes() {}

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}
