package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;
import com.mojang.math.Axis;

import com.cyberfarm.port.client.LightCoordsUtil;
import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The moving part of a small machine, turning while it works and winding down when it stops: the
 * Bio-Digester's paddle stirring behind its window.
 */
public class MovingPartsRenderer<T extends MachineBlockEntity> extends BlockEntityRenderer<T, MovingPartsRenderer.State> {
    private static final RenderType SHADOW = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    /** Ticks to spin up from still to full speed, or down again. */
    private static final float SPIN_UP = 30.0F;
    private static final int SLUDGE = 0x90253F14;

    public enum Part { PADDLE }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int light;
        /** How far the part has turned, in ticks at full speed. */
        float phase;
    }

    private final Part part;

    public MovingPartsRenderer(Part part) {
        this.part = part;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().relative(state.facing));
        float time = level.getGameTime() + partialTick;
        state.phase = ModelParts.advance(entity, time, entity.status.working() ? 1.0F : 0.0F, SPIN_UP);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // A frame where the front looks north (-z), as in the block's model
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        switch (part) {
            case PADDLE -> collector.submitCustomGeometry(poseStack, SHADOW, (pose, buffer) -> paddle(pose, buffer, state));
        }
        poseStack.popPose();
    }

    /** One fan blade, a strip from {@code inner} to {@code outer} out from the middle, swept back a little. */
    private static void blade(PoseStack.Pose pose, VertexConsumer buffer, float angle, float inner, float outer, float half, float y, float z,
            int colour, int light) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float sweep = angle + 0.35F;
        float[][] corners = {
                {cos * inner - sin * half, sin * inner + cos * half},
                {cos * inner + sin * half, sin * inner - cos * half},
                {Mth.cos(sweep) * outer + Mth.sin(sweep) * half, Mth.sin(sweep) * outer - Mth.cos(sweep) * half},
                {Mth.cos(sweep) * outer - Mth.sin(sweep) * half, Mth.sin(sweep) * outer + Mth.cos(sweep) * half}};
        for (float[] corner : corners) {
            vertex(pose, buffer, corner[0], y + corner[1], z, colour, light, 0.0F, 0.0F, -1.0F);
        }
    }

    /** A two-armed paddle's shadow turning slowly in the sludge behind the tank's porthole. */
    private static void paddle(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        float z = -0.5F + 1.25F * PIXEL - 0.002F;
        float y = 7.0F * PIXEL;
        for (int i = 0; i < 2; i++) {
            float angle = state.phase * 0.08F + i * Mth.PI;
            blade(pose, buffer, angle, 0.0F, 1.4F * PIXEL, 0.4F * PIXEL, y, z, SLUDGE, state.light);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public AABB getRenderBoundingBox(T entity) {
        return new AABB(entity.getBlockPos());
    }
}
