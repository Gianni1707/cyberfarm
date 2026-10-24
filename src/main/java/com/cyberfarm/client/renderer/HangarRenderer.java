package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a complete Mech Hangar as one rack, its frames hidden: white floor rails with dark treads
 * and a cyan light line, two posts with blue bands and light strips braced to the rails, a beam
 * across their tops with two arms reaching forward, hydraulic struts from the posts to the arms,
 * and two clamps that come down onto a parked mech's shoulders.
 *
 * <p>Works in the rack's own frame: w across it (0 at the console's rail, 4 at the other), d into
 * it (0 at the front, 4 at the posts), y up from the ground; whole numbers are block centres.
 */
public class HangarRenderer implements BlockEntityRenderer<MechHangarBlockEntity, HangarRenderer.State> {
    private static final RenderType METAL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/methane_chamber.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));
    private static final int SHELL = 0xFFE3E9ED;
    private static final int DARK = 0xFF2B3036;
    private static final int STEEL = 0xFF8A96A2;
    private static final int BLUE = 0xFF1F8FD0;
    private static final int CYAN = 0xFF2FE0FF;
    /** The arms' and clamps' place across the rack: over the parked mech's shoulders. */
    private static final float[] SHOULDERS = {1.34F, 2.66F};

    public static class State extends BlockEntityRenderState {
        boolean formed;
        /** The rack's across and into directions in the world, as x and z steps. */
        int acrossX;
        int acrossZ;
        int inX;
        int inZ;
        float clamp;
        int light;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MechHangarBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.formed = entity.getBlockState().getValue(MechHangarBlock.FORMED) && entity.getLevel() != null;
        if (!state.formed) {
            return;
        }
        Direction facing = entity.getBlockState().getValue(MachineBlock.FACING);
        Direction across = facing.getCounterClockWise();
        Direction in = facing.getOpposite();
        state.acrossX = across.getStepX();
        state.acrossZ = across.getStepZ();
        state.inX = in.getStepX();
        state.inZ = in.getStepZ();
        state.clamp = entity.clampAmount(entity.getLevel().getGameTime() + partialTick);
        state.light = LevelRenderer.getLightCoords(entity.getLevel(), entity.getBlockPos().above());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        int light = state.light;
        float clampY = Mth.lerp(smooth(state.clamp), 5.0F, 4.38F);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            Rack rack = new Rack(pose, buffer, state, light);
            for (int w : new int[] {0, 4}) {
                // Floor rail as high as the pad, treads across its top, and its post braced to it
                rack.beam(w, 0.5F, -0.48F, w, 0.5F, 4.48F, 0.48F, SHELL);
                rack.beam(w, 0.62F, -0.49F, w, 0.62F, 4.49F, 0.485F, BLUE);
                for (float t = -0.2F; t <= 4.2F; t += 0.5F) {
                    rack.beam(w - 0.34F, 1.0F, t, w + 0.34F, 1.0F, t, 0.05F, DARK);
                }
                rack.beam(w, 0.0F, 4.0F, w, 5.9F, 4.0F, 0.36F, SHELL);
                rack.beam(w, 1.35F, 4.0F, w, 1.7F, 4.0F, 0.4F, BLUE);
                rack.beam(w, 4.15F, 4.0F, w, 4.5F, 4.0F, 0.4F, BLUE);
                rack.beam(w, 1.0F, 2.3F, w, 2.6F, 4.0F, 0.14F, SHELL);
            }
            // The beam across the top, capped dark, and the arms forward over the pad
            rack.beam(-0.45F, 5.55F, 4.0F, 4.45F, 5.55F, 4.0F, 0.4F, SHELL);
            rack.beam(-0.5F, 5.55F, 4.0F, -0.42F, 5.55F, 4.0F, 0.43F, DARK);
            rack.beam(4.42F, 5.55F, 4.0F, 4.5F, 5.55F, 4.0F, 0.43F, DARK);
            for (int i = 0; i < 2; i++) {
                float w = SHOULDERS[i];
                float post = i == 0 ? 0.0F : 4.0F;
                rack.beam(w, 5.55F, 4.0F, w, 5.55F, 1.85F, 0.24F, SHELL);
                // Hydraulic strut: a dark cylinder from the post, a steel rod into the arm
                float midW = Mth.lerp(0.55F, post, w);
                rack.beam(post, 3.0F, 4.0F, midW, Mth.lerp(0.55F, 3.0F, 5.3F), Mth.lerp(0.55F, 4.0F, 2.6F), 0.12F, DARK);
                rack.beam(midW, Mth.lerp(0.55F, 3.0F, 5.3F), Mth.lerp(0.55F, 4.0F, 2.6F), w, 5.3F, 2.6F, 0.06F, STEEL);
                // The clamp: a rod down from the arm's tip and a claw at its end
                rack.beam(w, 5.3F, 2.0F, w, clampY, 2.0F, 0.07F, STEEL);
                rack.beam(w, clampY, 1.7F, w, clampY, 2.3F, 0.11F, DARK);
                rack.beam(w - 0.18F, clampY - 0.2F, 1.75F, w - 0.18F, clampY, 1.75F, 0.05F, DARK);
                rack.beam(w + 0.18F, clampY - 0.2F, 2.25F, w + 0.18F, clampY, 2.25F, 0.05F, DARK);
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            Rack rack = new Rack(pose, buffer, state, LightCoordsUtil.FULL_BRIGHT);
            for (int w : new int[] {0, 4}) {
                float out = w == 0 ? -0.33F : 0.33F;
                rack.beam(w + out * 1.48F, 0.62F, -0.45F, w + out * 1.48F, 0.62F, 4.45F, 0.03F, CYAN);
                rack.beam(w, 0.6F, 3.62F, w, 5.4F, 3.62F, 0.04F, CYAN);
            }
            rack.beam(0.2F, 5.55F, 3.59F, 3.8F, 5.55F, 3.59F, 0.03F, CYAN);
            if (state.clamp > 0.9F) {
                for (float w : SHOULDERS) {
                    rack.beam(w, clampY + 0.12F, 1.75F, w, clampY + 0.12F, 2.25F, 0.03F, CYAN);
                }
            }
        });
        poseStack.popPose();
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** Draws square beams between two points of the rack's frame. */
    private record Rack(PoseStack.Pose pose, VertexConsumer buffer, State state, int light) {
        private Vec3 at(float w, float y, float d) {
            return new Vec3(state.acrossX * w + state.inX * d, y, state.acrossZ * w + state.inZ * d);
        }

        /** A beam from (w0, y0, d0) to (w1, y1, d1), {@code half} its half-width. */
        void beam(float w0, float y0, float d0, float w1, float y1, float d1, float half, int color) {
            Vec3 a = at(w0, y0, d0);
            Vec3 b = at(w1, y1, d1);
            Vec3 dir = b.subtract(a);
            if (dir.lengthSqr() < 1.0E-6) {
                return;
            }
            dir = dir.normalize();
            Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 u = dir.cross(up).normalize().scale(half);
            Vec3 v = dir.cross(u).normalize().scale(half);
            Vec3[] ends = {a, b};
            Vec3[][] corners = new Vec3[2][4];
            for (int e = 0; e < 2; e++) {
                corners[e][0] = ends[e].add(u).add(v);
                corners[e][1] = ends[e].subtract(u).add(v);
                corners[e][2] = ends[e].subtract(u).subtract(v);
                corners[e][3] = ends[e].add(u).subtract(v);
            }
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                Vec3 normal = corners[0][i].add(corners[0][j]).scale(0.5).subtract(a).normalize();
                quad(normal, corners[0][i], corners[0][j], corners[1][j], corners[1][i], color);
            }
            quad(dir.reverse(), corners[0][3], corners[0][2], corners[0][1], corners[0][0], color);
            quad(dir, corners[1][0], corners[1][1], corners[1][2], corners[1][3], color);
        }

        private void quad(Vec3 normal, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
            Vec3[] points = {p0, p1, p2, p3};
            float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(pose, (float) points[i].x, (float) points[i].y, (float) points[i].z).setColor(color)
                        .setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            }
        }
    }

    // The rack spans 5x5 blocks and 6 up from the console at its corner
    @Override
    public AABB getRenderBoundingBox(MechHangarBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(6.0);
    }
}
