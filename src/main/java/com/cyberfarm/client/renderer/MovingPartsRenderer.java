package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The moving part of a small machine, turning while it works and winding down when it stops: the
 * Gas Turbine's fan behind its front grille, the Water Pump's piston going up and down on its top,
 * the Bio-Digester's paddle stirring behind its window.
 */
public class MovingPartsRenderer<T extends MachineBlockEntity> implements BlockEntityRenderer<T, MovingPartsRenderer.State> {
    private static final RenderType SOLID = RenderTypes.entityCutout(CyberFarm.id("textures/entity/hologram.png"));
    private static final RenderType SHADOW = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    /** Ticks to spin up from still to full speed, or down again. */
    private static final float SPIN_UP = 30.0F;
    private static final int STEEL = 0xFF8C99A5;
    private static final int STEEL_DARK = 0xFF4A5561;
    private static final int SLUDGE = 0x90253F14;

    public enum Part { FAN, PISTON, PADDLE }

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
        state.light = LevelRenderer.getLightCoords(level, part == Part.PISTON ? entity.getBlockPos().above() : entity.getBlockPos().relative(state.facing));
        float time = level.getGameTime() + partialTick;
        float elapsed = entity.motionTime == 0.0F ? 0.0F : Mth.clamp(time - entity.motionTime, 0.0F, 5.0F);
        entity.motionTime = time;
        float target = entity.status.working() ? 1.0F : 0.0F;
        entity.motionSpeed += Mth.clamp(target - entity.motionSpeed, -elapsed / SPIN_UP, elapsed / SPIN_UP);
        entity.motionPhase += entity.motionSpeed * elapsed;
        state.phase = entity.motionPhase;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // A frame where the front looks north (-z), as in the block's model
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        switch (part) {
            case FAN -> collector.submitCustomGeometry(poseStack, SOLID, (pose, buffer) -> fan(pose, buffer, state));
            case PISTON -> collector.submitCustomGeometry(poseStack, SOLID, (pose, buffer) -> piston(pose, buffer, state));
            case PADDLE -> collector.submitCustomGeometry(poseStack, SHADOW, (pose, buffer) -> paddle(pose, buffer, state));
        }
        poseStack.popPose();
    }

    /** Four blades turning round the burner in the middle of the front's disc. */
    private static void fan(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        float z = -0.5F - 0.002F;
        float y = 0.5F;
        for (int i = 0; i < 4; i++) {
            float angle = state.phase * 0.6F + i * Mth.HALF_PI;
            blade(pose, buffer, angle, 1.8F * PIXEL, 4.9F * PIXEL, 0.9F * PIXEL, y, z, i % 2 == 0 ? STEEL : STEEL_DARK, state.light);
        }
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

    /** A rod with a cap rising and sinking out of the middle of the pump's top. */
    private static void piston(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        float lift = (1.0F - Mth.cos(state.phase * 0.35F)) * 0.5F;
        float top = 1.0F + PIXEL + lift * 3.0F * PIXEL;
        box(pose, buffer, -PIXEL, 1.0F, -PIXEL, PIXEL, top, PIXEL, STEEL, state.light);
        box(pose, buffer, -2.5F * PIXEL, top, -2.5F * PIXEL, 2.5F * PIXEL, top + PIXEL, 2.5F * PIXEL, STEEL_DARK, state.light);
    }

    /** A two-armed paddle's shadow turning slowly behind the digester's window. */
    private static void paddle(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        float z = -0.5F - 0.002F;
        float y = 6.5F * PIXEL;
        for (int i = 0; i < 2; i++) {
            float angle = state.phase * 0.08F + i * Mth.PI;
            blade(pose, buffer, angle, 0.0F, 2.4F * PIXEL, 0.5F * PIXEL, y, z, SLUDGE, state.light);
        }
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int colour, int light) {
        quad(pose, buffer, colour, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, colour, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, colour, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(pose, buffer, colour, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, buffer, colour, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int colour, int light, float nx, float ny, float nz, float... c) {
        for (int i = 0; i < 4; i++) {
            vertex(pose, buffer, c[i * 3], c[i * 3 + 1], c[i * 3 + 2], colour, light, nx, ny, nz);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public AABB getRenderBoundingBox(T entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 0.5, 0.0);
    }
}
