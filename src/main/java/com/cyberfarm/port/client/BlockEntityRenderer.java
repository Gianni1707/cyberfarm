package com.cyberfarm.port.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A 26.x block entity renderer (extract a state, then submit it) on the 1.20.1 render call. Each
 * renderer keeps one state, one collector and one camera and refills them for every block entity
 * it draws: the client renders one at a time, so nothing is allocated per frame.
 */
public abstract class BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
        implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {
    private @Nullable S state;
    private final SubmitNodeCollector collector = new SubmitNodeCollector(null);
    private final CameraRenderState camera = new CameraRenderState();

    public abstract S createRenderState();

    public abstract void extractRenderState(T entity, S state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress);

    public abstract void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);

    /** The box that must be on screen for this to draw; registered for its block entity type. */
    public AABB getRenderBoundingBox(T entity) {
        return new AABB(entity.getBlockPos());
    }

    @Override
    public final void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        S current = state;
        if (current == null) {
            current = state = createRenderState();
        }
        camera.set(Minecraft.getInstance().gameRenderer.getMainCamera());
        current.lightCoords = light;
        extractRenderState(entity, current, partialTick, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), null);
        submit(current, poseStack, collector.buffers(buffers), camera);
    }
}
