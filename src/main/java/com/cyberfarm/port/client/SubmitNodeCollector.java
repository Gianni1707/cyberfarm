package com.cyberfarm.port.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.util.FormattedCharSequence;

/**
 * The 26.x submit calls, drawn at once into the 1.20.1 buffer source. One per renderer, reused
 * every frame, so drawing allocates nothing.
 */
public final class SubmitNodeCollector {
    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }

    private final VertexConsumer vertices = new VertexConsumer();
    private MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public SubmitNodeCollector buffers(MultiBufferSource buffers) {
        this.buffers = buffers;
        return this;
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), vertices.wrap(buffers.getBuffer(type)));
    }

    /** A model posed for {@code state}, tinted by {@code color} (ARGB). */
    public <S extends LivingEntityRenderState> void submitModel(EntityModel<S> model, S state, PoseStack poseStack, RenderType type, int light, int overlay, int color,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        model.setupAnim(state);
        model.renderToBuffer(poseStack, buffers.getBuffer(type), light, overlay,
                (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
    }

    /** A block model drawn where the pose is, lit from its own place, as 1.20.1 draws a block a piston moves. */
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int outline) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ForgeHooksClient.renderPistonMovedBlocks(state.blockPos, state.blockState, poseStack, buffers, minecraft.level, false,
                    OverlayTexture.NO_OVERLAY, minecraft.getBlockRenderer());
        }
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
            int light, int color, int background, int outline) {
        Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), buffers, mode, background, light);
    }
}
