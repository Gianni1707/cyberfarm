package com.cyberfarm.port.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The 26.x vertex chain (addVertex, setColor, setUv, setOverlay, setLight, setNormal) over a 1.20.1
 * consumer. Each call writes straight through; setNormal is always last, so it ends the vertex. A
 * format without some element (lines have no uv) skips it, as BufferBuilder does.
 */
public final class VertexConsumer {
    private com.mojang.blaze3d.vertex.VertexConsumer delegate;

    VertexConsumer wrap(com.mojang.blaze3d.vertex.VertexConsumer delegate) {
        this.delegate = delegate;
        return this;
    }

    public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        delegate.vertex(pose.pose(), x, y, z);
        return this;
    }

    public VertexConsumer setColor(int argb) {
        delegate.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
        return this;
    }

    public VertexConsumer setColor(float red, float green, float blue, float alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    public VertexConsumer setUv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    public VertexConsumer setOverlay(int overlay) {
        delegate.overlayCoords(overlay);
        return this;
    }

    public VertexConsumer setLight(int light) {
        delegate.uv2(light);
        return this;
    }

    public void setNormal(PoseStack.Pose pose, float x, float y, float z) {
        delegate.normal(pose.normal(), x, y, z).endVertex();
    }

    /** The 26.x putBakedQuad with a QuadInstance colour (ARGB) and light: the whole quad, tinted, in one call. */
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, int argb, int light) {
        delegate.putBulkData(pose, quad, (argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F,
                (argb >>> 24) / 255.0F, light, OverlayTexture.NO_OVERLAY, false);
    }
}
