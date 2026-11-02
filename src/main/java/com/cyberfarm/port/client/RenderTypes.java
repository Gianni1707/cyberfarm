package com.cyberfarm.port.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The 26.x RenderTypes factory: on 1.20.1 the same render types are static methods of RenderType. */
public final class RenderTypes {
    private RenderTypes() {}

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}
