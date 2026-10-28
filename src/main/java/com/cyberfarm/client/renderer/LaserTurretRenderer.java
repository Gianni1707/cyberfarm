package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.LaserTurretBlockEntity;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Laser Turret's head: a steel housing with cooling fins and a barrel, turning toward the mob
 * it aims at and slowly sweeping round when there is none. The lens at the barrel's tip glows red.
 */
public class LaserTurretRenderer implements BlockEntityRenderer<LaserTurretBlockEntity, LaserTurretRenderer.State> {
    private static final RenderType SOLID = RenderTypes.entityCutout(CyberFarm.id("textures/entity/hologram.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    /** Most the head turns per tick, in radians, so it swings onto a new mob instead of snapping. */
    private static final float TURN_SPEED = 0.3F;
    private static final float SWEEP_SPEED = 0.015F;
    private static final int STEEL = 0xFFC9D3DC;
    private static final int STEEL_MID = 0xFF7D8A96;
    private static final int STEEL_DARK = 0xFF2C333B;
    private static final int LENS_ARMED = 0xFFFF2A1F;
    private static final int LENS_IDLE = 0xFF6A1A14;
    private static final int SENSOR = 0xFF2FE0FF;

    public static class State extends BlockEntityRenderState {
        float yaw;
        float pitch;
        int light;
        boolean armed;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LaserTurretBlockEntity turret, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(turret, state, breakProgress);
        Level level = turret.getLevel();
        if (level == null) {
            return;
        }
        state.light = LevelRenderer.getLightCoords(level, turret.getBlockPos().above());
        float time = level.getGameTime() + partialTick;
        float elapsed = turret.headTime == 0.0F ? 0.0F : Mth.clamp(time - turret.headTime, 0.0F, 5.0F);
        turret.headTime = time;
        Entity target = turret.getTargetId() >= 0 ? level.getEntity(turret.getTargetId()) : null;
        float wantYaw;
        float wantPitch;
        if (target != null) {
            Vec3 at = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.5, 0.0)
                    .subtract(Vec3.atBottomCenterOf(turret.getBlockPos()).add(0.0, LaserTurretBlockEntity.MUZZLE_HEIGHT, 0.0));
            // The barrel looks down -z: turned by yaw round y, then raised by pitch
            wantYaw = (float) Mth.atan2(-at.x, -at.z);
            wantPitch = (float) Mth.atan2(at.y, at.horizontalDistance());
        } else {
            wantYaw = turret.headYaw + SWEEP_SPEED * elapsed;
            wantPitch = 0.0F;
        }
        float step = TURN_SPEED * elapsed;
        turret.headYaw += Mth.clamp(Mth.wrapDegrees((wantYaw - turret.headYaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD, -step, step);
        turret.headPitch += Mth.clamp(wantPitch - turret.headPitch, -step, step);
        state.yaw = turret.headYaw;
        state.pitch = turret.headPitch;
        state.armed = target != null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, (float) LaserTurretBlockEntity.MUZZLE_HEIGHT, 0.5F);
        poseStack.mulPose(Axis.YP.rotation(state.yaw));
        poseStack.mulPose(Axis.XP.rotation(state.pitch));
        collector.submitCustomGeometry(poseStack, SOLID, (pose, buffer) -> head(pose, buffer, state));
        poseStack.popPose();
    }

    private static void head(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        int light = state.light;
        // Housing, fins on both sides, a sensor eye on top
        box(pose, buffer, -3.5F, -2.5F, -3.5F, 3.5F, 2.5F, 3.5F, STEEL, light);
        box(pose, buffer, -4.5F, -1.5F, -2.0F, -3.5F, 1.5F, 2.0F, STEEL_DARK, light);
        box(pose, buffer, 3.5F, -1.5F, -2.0F, 4.5F, 1.5F, 2.0F, STEEL_DARK, light);
        box(pose, buffer, -1.0F, 2.5F, 0.5F, 1.0F, 3.5F, 2.5F, STEEL_MID, light);
        box(pose, buffer, -0.5F, 2.6F, 0.3F, 0.5F, 3.2F, 0.6F, SENSOR, LightCoordsUtil.FULL_BRIGHT);
        // Barrel with a collar, and the lens at its tip
        box(pose, buffer, -1.0F, -1.0F, -9.0F, 1.0F, 1.0F, -3.5F, STEEL_MID, light);
        box(pose, buffer, -1.5F, -1.5F, -5.5F, 1.5F, 1.5F, -4.5F, STEEL_DARK, light);
        box(pose, buffer, -0.7F, -0.7F, -9.4F, 0.7F, 0.7F, -9.0F, state.armed ? LENS_ARMED : LENS_IDLE, LightCoordsUtil.FULL_BRIGHT);
    }

    /** A box between two corners given in pixels. */
    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float px0, float py0, float pz0, float px1, float py1, float pz1, int colour, int light) {
        float x0 = px0 * PIXEL, y0 = py0 * PIXEL, z0 = pz0 * PIXEL, x1 = px1 * PIXEL, y1 = py1 * PIXEL, z1 = pz1 * PIXEL;
        quad(pose, buffer, colour, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, colour, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(pose, buffer, colour, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, colour, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(pose, buffer, colour, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, buffer, colour, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int colour, int light, float nx, float ny, float nz, float... c) {
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(colour).setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }

    @Override
    public AABB getRenderBoundingBox(LaserTurretBlockEntity turret) {
        return new AABB(turret.getBlockPos()).inflate(0.2);
    }
}
