package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.HumanDockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Human Dock: the arch rising out of its stumps like a tripod's telescopic legs while it opens, its middle section
 * first and then the top one (folding back the same way), then its lights. With
 * power and nobody on it the footprints, strips and windows breathe; charging they are lit, the windows blink and two
 * rings of light climb each post. Model pixels as in tools/machinemodels.py, human_dock_*(): front to the south there,
 * the arch at the back (z 0.2..3.8 there, 12.2..15.8 here after the mirror). The clamps, the display and the mast are
 * clampsDisplayMast's.
 */
public class HumanDockRenderer implements BlockEntityRenderer<HumanDockBlockEntity, HumanDockRenderer.State> {
    /** Each telescopic section slides this far out of the one below. */
    private static final float STAGE = 6.0F;
    private static final int LIGHT_FULL = 0xF000F0;
    private static final int OFF = 0x055B57, GLOW = 0xD1FEE7;
    private static final float[][] FOOTPRINTS = {{4.5F, 6.5F, 7.5F, 11.5F}, {8.5F, 6.5F, 11.5F, 11.5F}};
    /** The light strips on the inner faces of the posts: x from, x to, y from, y to; the stumps', the middle and top sections'. */
    private static final float[][] STRIPS = {{4.22F, 4.27F, 6.5F, 11.5F}, {11.73F, 11.78F, 6.5F, 11.5F},
            {4.12F, 4.17F, 13.0F, 17.0F}, {11.83F, 11.88F, 13.0F, 17.0F}, {4.02F, 4.07F, 19.0F, 23.0F}, {11.93F, 11.98F, 19.0F, 23.0F}};
    private static final float[][] WINDOWS = {{1.2F, 4.0F}, {12.0F, 14.8F}};

    public static class State extends BlockEntityRenderState {
        int stage;
        float t;
        @Nullable MovingBlockRenderState arch;
        @Nullable MovingBlockRenderState mid;
        Direction facing = Direction.NORTH;
        /** 0 dark, 1 powered and waiting (breathing), 2 charging. */
        int mode;
        float time;
        /** The dock's buffer, 0..1, for the display's six segments. */
        float level;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HumanDockBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.arch = null;
        state.mid = null;
        state.mode = 0;
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        if (level == null || state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.time = ModelParts.time(level, partialTick);
        state.t = entity.shownOpening(state.stage, state.time);
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, state.time)) {
            state.arch = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.DOCK_ARCH, state.facing);
            state.mid = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.DOCK_ARCH_MID, state.facing);
        }
        if (state.stage == SmallMultiblockBlock.OPEN) {
            state.mode = entity.getBlockState().getValue(LitMachineBlock.LIT) ? 2 : entity.status.energy() > 0 ? 1 : 0;
        }
        state.level = Mth.clamp(entity.status.energy() / (float) HumanDockBlockEntity.CAPACITY, 0.0F, 1.0F);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        // Like a tripod's leg: the middle section slides out of the stumps, then the top one out of it, each settling
        float open = HumanDockBlockEntity.OPEN_TICKS;
        float first = ModelParts.settle(state.t, 0.0F, open * 0.55F), second = ModelParts.settle(state.t, open * 0.45F, open);
        float sinkMid = Math.max(0.0F, STAGE * (1.0F - first)), sinkArch = Math.max(0.0F, STAGE * (2.0F - first - second));
        MovingBlockRenderState arch = state.arch, mid = state.mid;
        if (arch != null && mid != null) {
            ModelParts.slid(poseStack, collector, mid, state.facing, 0.0F, -STAGE / 16.0F * (1.0F - first), 0.0F);
            ModelParts.slid(poseStack, collector, arch, state.facing, 0.0F, -STAGE / 16.0F * (2.0F - first - second), 0.0F);
        }
        // The clamps, the display and the mast: there through the opening, the closing and the open dock, lit or not
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> clampsDisplayMast(pose, buffer, state, sinkMid, sinkArch));
        poseStack.popPose();
        if (state.mode == 0) {
            return;
        }
        float t = state.time;
        boolean charging = state.mode == 2;
        float glow = charging ? 1.0F : 0.3F + 0.2F * Mth.sin(t * Mth.TWO_PI / 60.0F);
        int lit = blend(glow);
        int window = charging ? ((int) t / 4 % 2 == 0 ? GLOW | 0xFF000000 : blend(0.35F)) : lit;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            for (float[] f : FOOTPRINTS) {
                ModelParts.box(pose, buffer, f[0], 3.8F, mirror(f[3]), f[2], 3.85F, mirror(f[1]), lit, lit, LIGHT_FULL);
            }
            for (float[] s : STRIPS) {
                ModelParts.box(pose, buffer, s[0], s[2], mirror(2.6F), s[1], s[3], mirror(1.2F), lit, lit, LIGHT_FULL);
            }
            for (float[] w : WINDOWS) {
                ModelParts.box(pose, buffer, w[0], 25.4F, mirror(4.02F), w[1], 27.0F, mirror(3.97F), window, window, LIGHT_FULL);
            }
        });
        poseStack.popPose();
    }

    private static final int STEEL = 0xFFBDC6C5, TEAL = 0xFF2F6468, DEEP = 0xFF132423, SCREEN = 0xFF071314;
    /** The clamps' x by post, closed and open, and their bottom on the stumps' and the middle section's tops. */
    private static final float[][] CLAMP_X = {{0.6F, 4.4F, 0.2F, 4.8F}, {11.6F, 15.4F, 11.2F, 15.8F}};
    private static final float CLAMP_LOW = 12.0F, CLAMP_HIGH = 18.0F, LINTEL_TOP = 27.9F;

    /**
     * The user's pick (B + C's display, polished by the Designer): a clamp at each joint of the tripod, open while its
     * section travels and snapping shut with a flash as it lands; then a display slides out of the lintel and a mast with
     * a beacon rises on it. Charging, light climbs pad, clamp, clamp, beacon; the display's six segments show the buffer.
     * Closing runs it all backwards. Boxes in model pixels, z given as in the design and mirrored.
     */
    private static void clampsDisplayMast(PoseStack.Pose pose, VertexConsumer buffer, State state, float sinkMid, float sinkArch) {
        float t = state.t, time = state.time;
        int light = state.lightCoords;
        boolean charging = state.mode == 2;
        int step = (int) (time / 6.0F) % 4;
        // Clamps: openness, the snap (settle's overshoot), the flash after it
        float[] openness = {Math.min(Mth.clamp(t / 4.0F, 0.0F, 1.0F), 1.0F - ModelParts.settle(t, 16.0F, 20.0F)),
                Math.min(Mth.clamp(t / 4.0F, 0.0F, 1.0F), 1.0F - ModelParts.settle(t, 30.0F, 34.0F))};
        float[] flashFrom = {20.0F, 34.0F};
        for (int c = 0; c < 2; c++) {
            float o = Math.max(0.0F, openness[c]);
            float base = (c == 0 ? CLAMP_LOW : CLAMP_HIGH - sinkMid) + 0.8F * o;
            int colour = o > 0.05F ? TEAL : STEEL;
            int lit = light;
            float k = (t - flashFrom[c]) / 6.0F;
            if (t >= flashFrom[c] && k < 1.0F && state.stage != SmallMultiblockBlock.OPEN) {
                colour = mix(GLOW | 0xFF000000, STEEL, Mth.clamp((k - 0.3F) / 0.7F, 0.0F, 1.0F));
                lit = LIGHT_FULL;
            } else if (charging && step == c + 1) {
                colour = GLOW | 0xFF000000;
                lit = LIGHT_FULL;
            }
            for (float[] x : CLAMP_X) {
                float x0 = Mth.lerp(o, x[0], x[2]), x1 = Mth.lerp(o, x[1], x[3]), z1 = 4.0F + 0.3F * o;
                ModelParts.box(pose, buffer, x0, base, mirror(z1), x1, base + 0.7F, mirror(0.0F), colour, colour, lit);
            }
        }
        // The mast and its beacon, after the arch lands
        float e = 2.2F * ModelParts.settle(t, 34.0F, 40.0F);
        if (e > 0.05F) {
            float top = LINTEL_TOP - sinkArch;
            ModelParts.box(pose, buffer, 6.0F, top, mirror(3.0F), 10.0F, top + e, mirror(1.0F), TEAL, TEAL, light);
            int beacon = STEEL;
            int beaconLight = light;
            if (state.mode == 1) {
                beacon = blend(0.3F + 0.2F * Mth.sin(time * Mth.TWO_PI / 60.0F));
                beaconLight = LIGHT_FULL;
            } else if (charging) {
                beacon = step == 3 ? GLOW | 0xFF000000 : blend(0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / 20.0F));
                beaconLight = LIGHT_FULL;
            }
            ModelParts.box(pose, buffer, 7.0F, top + e, mirror(2.6F), 9.0F, top + e + 0.7F, mirror(1.4F), beacon, beacon, beaconLight);
        }
        // The display slides out of the lintel's front; its face shows the buffer in six segments
        float d = ModelParts.settle(t, 30.0F, 40.0F);
        if (d <= 0.02F) {
            return;
        }
        float lift = -sinkArch;
        ModelParts.box(pose, buffer, 4.4F, 24.7F + lift, mirror(1.6F + 3.0F * d), 11.6F, 27.3F + lift, mirror(0.4F + 3.0F * d), DEEP, DEEP, light);
        if (d <= 0.85F) {
            return;
        }
        float fz = 1.6F + 3.0F * d;
        ModelParts.box(pose, buffer, 4.9F, 25.1F + lift, mirror(fz + 0.1F), 11.1F, 26.9F + lift, mirror(fz), SCREEN, SCREEN, light);
        int lit = state.mode == 0 ? 0 : Math.min(6, (int) (state.level * 6.0F));
        int breathing = state.mode == 1 ? blend(0.3F + 0.2F * Mth.sin(time * Mth.TWO_PI / 60.0F)) : GLOW | 0xFF000000;
        for (int i = 0; i < 6; i++) {
            boolean on = i < lit || charging && i == lit && (int) (time / 8.0F) % 2 == 0;
            int colour = on ? breathing : OFF | 0xFF000000;
            ModelParts.box(pose, buffer, 5.2F + 0.95F * i, 25.4F + lift, mirror(fz + 0.2F), 5.9F + 0.95F * i, 26.3F + 0.12F * i + lift,
                    mirror(fz + 0.1F), colour, colour, on ? LIGHT_FULL : light);
        }
    }

    private static int mix(int a, int b, float k) {
        int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255);
        int g = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255);
        int bl = (int) Mth.lerp(k, a & 255, b & 255);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** z from the design (front to the south, arch at the back) into the model's, mirrored as machinemodels.py builds it. */
    private static float mirror(float z) {
        return 16.0F - z;
    }

    private static int blend(float k) {
        int r = (int) Mth.lerp(k, OFF >> 16 & 255, GLOW >> 16 & 255);
        int g = (int) Mth.lerp(k, OFF >> 8 & 255, GLOW >> 8 & 255);
        int b = (int) Mth.lerp(k, OFF & 255, GLOW & 255);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }


    /** Its cell and the frame's above, where the arch stands. */
    @Override
    public AABB getRenderBoundingBox(HumanDockBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
