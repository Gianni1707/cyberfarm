package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.LightCoordsUtil;
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
 * and two clamps that come down onto a parked mech's shoulders. A gantry rides on the arms: a
 * bridge, a trolley and a telescopic gripper that carries modules to the mech's mounts and back.
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
    /** Where the gripper takes each module, by MechModule order (w, y, d): the back unit, beside each forearm, before the chest. */
    private static final float[][] MOUNTS = {{2.0F, 3.8F, 2.8F}, {0.98F, 3.0F, 2.0F}, {3.02F, 3.0F, 2.0F}, {2.0F, 3.75F, 1.45F}};
    /** The gantry at rest: the bridge back by the beam, the trolley in the middle, the gripper drawn up. */
    private static final float HOME_W = 2.0F;
    private static final float HOME_D = 3.55F;
    private static final float GANTRY_Y = 5.95F;
    private static final float GRIPPER_UP = 5.35F;
    /** How far below the gripper a carried module hangs. */
    private static final float MODULE_DROP = 0.2F;

    public static class State extends BlockEntityRenderState {
        boolean formed;
        /** The rack's across and into directions in the world, as x and z steps. */
        int acrossX;
        int acrossZ;
        int inX;
        int inZ;
        float clamp;
        int fitModule = -1;
        float fit;
        boolean fitInstall;
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
        float time = entity.getLevel().getGameTime() + partialTick;
        state.clamp = entity.clampAmount(time);
        state.fitModule = entity.fitModule();
        state.fit = entity.fitProgress(time);
        state.fitInstall = entity.fitInstall();
        state.light = LightCoordsUtil.getLightCoords(entity.getLevel(), entity.getBlockPos().above());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        int light = state.light;
        float clampY = Mth.lerp(smooth(state.clamp), 5.0F, 4.38F);
        Gantry gantry = gantry(state);
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
                rack.beam(w, 5.55F, 4.0F, w, 5.55F, 1.3F, 0.24F, SHELL);
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
            // The gantry: a bridge riding on the arms, a trolley on it, the telescopic gripper hanging from that
            float d = gantry.d;
            for (float w : SHOULDERS) {
                rack.beam(w, 5.84F, d - 0.2F, w, 5.84F, d + 0.2F, 0.08F, DARK);
            }
            rack.beam(0.8F, GANTRY_Y, d, 3.2F, GANTRY_Y, d, 0.1F, SHELL);
            rack.beam(0.78F, GANTRY_Y, d, 0.82F, GANTRY_Y, d, 0.12F, BLUE);
            rack.beam(3.18F, GANTRY_Y, d, 3.22F, GANTRY_Y, d, 0.12F, BLUE);
            float w = gantry.w;
            float y = gantry.y;
            rack.beam(w, GANTRY_Y - 0.12F, d, w, GANTRY_Y + 0.14F, d, 0.17F, DARK);
            rack.beam(w, GANTRY_Y - 0.1F, d, w, GRIPPER_UP - 0.1F, d, 0.08F, STEEL);
            rack.beam(w, GRIPPER_UP - 0.1F, d, w, y + 0.05F, d, 0.05F, SHELL);
            rack.beam(w, y, d, w, y + 0.12F, d, 0.13F, DARK);
            rack.beam(w - 0.1F, y - 0.16F, d, w - 0.1F, y, d, 0.035F, DARK);
            rack.beam(w + 0.1F, y - 0.16F, d, w + 0.1F, y, d, 0.035F, DARK);
            if (gantry.carrying) {
                float m = y - MODULE_DROP;
                rack.beam(w, m - 0.13F, d, w, m + 0.13F, d, 0.14F, SHELL);
                rack.beam(w, m - 0.13F, d, w, m - 0.07F, d, 0.145F, BLUE);
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
            rack.beam(1.0F, GANTRY_Y + 0.1F, gantry.d, 3.0F, GANTRY_Y + 0.1F, gantry.d, 0.025F, CYAN);
            if (gantry.carrying) {
                float m = gantry.y - MODULE_DROP;
                rack.beam(gantry.w, m + 0.02F, gantry.d, gantry.w, m + 0.06F, gantry.d, 0.15F, CYAN);
            }
            // A flash as the module locks onto its mount
            if (state.fitModule >= 0 && state.fit > 0.47F && state.fit < 0.56F) {
                float[] mount = MOUNTS[state.fitModule];
                float size = 0.08F + 0.1F * Mth.sin((state.fit - 0.47F) / 0.09F * Mth.PI);
                rack.beam(mount[0], mount[1] - size, mount[2], mount[0], mount[1] + size, mount[2], size, CYAN);
            }
        });
        poseStack.popPose();
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float range(float t, float from, float to) {
        return Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
    }

    /** Where the gripper is, in the rack's frame, and whether it holds a module. */
    private record Gantry(float w, float y, float d, boolean carrying) {}

    /**
     * The arm's run to a mount: across to above it, the gripper down onto it, the module on (or
     * off) halfway, the gripper up and back home. A module comes in held and leaves held.
     */
    private static Gantry gantry(State state) {
        if (state.fitModule < 0 || state.fit >= 1.0F) {
            return new Gantry(HOME_W, GRIPPER_UP, HOME_D, false);
        }
        float t = state.fit;
        float[] mount = MOUNTS[state.fitModule];
        float travel = t < 0.5F ? smooth(range(t, 0.0F, 0.3F)) : 1.0F - smooth(range(t, 0.7F, 1.0F));
        float reach = t < 0.5F ? smooth(range(t, 0.3F, 0.5F)) : 1.0F - smooth(range(t, 0.5F, 0.7F));
        boolean carrying = state.fitInstall == t < 0.5F;
        return new Gantry(Mth.lerp(travel, HOME_W, mount[0]), Mth.lerp(reach, GRIPPER_UP, mount[1] + MODULE_DROP),
                Mth.lerp(travel, HOME_D, mount[2]), carrying);
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
