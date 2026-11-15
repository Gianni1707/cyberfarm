package com.cyberfarm.client.renderer;

import java.util.Map;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.TrinketTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Trinket Table: the gantry's beam rising as it opens; then the two arms hung from it, at rest folded under the
 * beam, at work in a 100-tick loop (the right one takes a part from the tray and sets it on the cradle, twice; the left
 * one welds after each, with sparks), the trinket growing on the cradle, a flash at the end of the loop, the readout on
 * the drawer running with the loop, and the work lights. Something made at the table starts one loop.
 * <p>
 * Coordinates are the sketch's (tools: table_polish3/tp3.py; the static parts in machinemodels.py, trinket_table_*()):
 * front to -z, x from left to right as the player sees the front (the frames' cell is x 16..32). {@link #mx} turns x
 * into the model's, where the frames' cell is x -16..0. Arm angles are absolute, in degrees from straight down,
 * measured towards the middle of the bench; "u" runs along x from each arm's own post.
 */
public class TrinketTableRenderer implements BlockEntityRenderer<TrinketTableBlockEntity, TrinketTableRenderer.State> {
    private static final float BASE = 11.0F, CR = 16.9F, LEDGE = 15.0F, BH = 3.0F, CX = 14.0F, SLOT = 19.5F;
    private static final float SU = 5.0F, SY = 26.0F, L1 = 8.0F, L2 = 8.0F, L3 = 3.5F, DROP = 4.0F;
    private static final int CYCLE = 50, LOOP = 100;
    private static final float[] REST = {8.0F, 172.0F, 90.0F};
    /** Each arm's z bands: upper arm, forearm, tool. */
    /** Kept apart by at least 0.3 px, so no two parts share a plane and flicker at a distance. */
    private static final float[][] LEFT_Z = {{4.4F, 6.3F}, {6.6F, 8.4F}, {7.0F, 8.1F}}, RIGHT_Z = {{9.7F, 11.6F}, {7.6F, 9.4F}, {7.9F, 9.0F}};
    /** How far a joint or band stands out of what it wraps. */
    private static final float CLEAR = 0.35F;
    private static final int LIGHT_FULL = 0xF000F0;
    private static final int OFF = 0xFF055B57, GLOW = 0xFFD1FEE7, MINT = 0xFF5EC2B8;
    private static final int ARM = 0xFF5B7771, ARM_TOP = 0xFF6F8A84, STEEL_DARK = 0xFF7C8989, STEEL = 0xFFB1BEBC, STEEL_LIGHT = 0xFFBDC6C5;

    /** The key poses, worked out once from where the tool tip has to be. */
    private static final Map<String, float[]> LEFT_POSES, RIGHT_POSES;
    /** The arms' timelines over one 50-tick cycle: the tick a pose is reached, and which; the second cycle sets the chip. */
    private static final Object[][][] LEFT_KEYS = {
            {{0, "ready"}, {34, "ready"}, {37, "preA"}, {39, "wA0"}, {46, "wA1"}, {48, "preA"}, {50, "ready"}},
            {{0, "ready"}, {34, "ready"}, {37, "preB"}, {39, "wB0"}, {46, "wB1"}, {48, "preB"}, {50, "ready"}}};
    private static final Object[][][] RIGHT_KEYS = {
            {{0, "ready"}, {2, "rPre"}, {7, "trayB_hi"}, {10, "trayB"}, {12, "trayB"}, {14, "trayB_hi"}, {21, "cr_hi"}, {25, "cr_B"},
                    {27, "cr_B"}, {31, "cr_hi"}, {34, "ready"}, {50, "ready"}},
            {{0, "ready"}, {2, "rPre"}, {7, "trayC_hi"}, {10, "trayC"}, {12, "trayC"}, {14, "trayC_hi"}, {21, "cr_hi"}, {25, "cr_C"},
                    {27, "cr_C"}, {31, "cr_hi"}, {34, "ready"}, {50, "ready"}}};

    static {
        float hi = 23.0F, pre = 22.5F, tray = 32.0F - SLOT, cradle = 32.0F - CX;
        LEFT_POSES = Map.of("ready", REST, "preA", ik(CX - 3.3F, pre), "preB", ik(CX - 2.5F, pre), "wA0", ik(CX - 3.1F, 17.5F),
                "wA1", ik(CX - 3.1F, 19.9F), "wB0", ik(CX - 1.9F, 20.4F), "wB1", ik(CX - 1.9F, 21.0F));
        RIGHT_POSES = Map.of("ready", REST, "rPre", ik(8.5F, pre), "trayB_hi", ik(tray, hi), "trayB", ik(tray, LEDGE + BH),
                "trayC_hi", ik(tray, hi), "trayC", ik(tray, LEDGE + 1.2F), "cr_hi", ik(cradle, hi), "cr_B", ik(cradle, CR + BH),
                "cr_C", ik(cradle, CR + BH + 1.2F));
    }

    public static class State extends BlockEntityRenderState {
        int stage;
        float opening;
        @Nullable MovingBlockRenderState beam;
        Direction facing = Direction.NORTH;
        boolean assembled;
        boolean working;
        float progress;
        float loop;
        int light;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TrinketTableBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.beam = null;
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        state.assembled = state.stage != SmallMultiblockBlock.LOOSE;
        if (level == null || !state.assembled) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        float time = ModelParts.time(level, partialTick);
        state.opening = Mth.clamp(entity.shownOpening(state.stage, time) / entity.openTicks(), 0.0F, 1.0F);
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, time)) {
            state.beam = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.TABLE_BEAM, state.facing);
        }
        state.working = state.stage == SmallMultiblockBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        state.loop = state.working ? ModelParts.advance(entity, time, 1.0F, 12.0F) % LOOP : 0.0F;
        // The drawer's readout runs with the loop: the thing being made, from nothing to done
        state.progress = state.loop / LOOP;
        state.light = LevelRenderer.getLightCoords(level, entity.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.assembled) {
            return;
        }
        float rise = -DROP * (1.0F - ModelParts.smooth(state.opening, 0.0F, 1.0F));
        MovingBlockRenderState beam = state.beam;
        if (beam != null) {
            ModelParts.slid(poseStack, collector, beam, state.facing, 0.0F, rise / 16.0F, 0.0F);
        }
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        int light = state.light;
        boolean working = state.working;
        float c = state.loop % CYCLE;
        int cycle = (int) (state.loop / CYCLE);
        float[] left = working ? pose(LEFT_KEYS[cycle], LEFT_POSES, c) : REST;
        float[] right = working ? pose(RIGHT_KEYS[cycle], RIGHT_POSES, c) : REST;
        float sy = SY + rise;
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            lights(pose, buffer, state, working);
            arm(pose, buffer, left, false, sy, LEFT_Z, light, working && c >= 39 && c < 46, c, state.loop);
            arm(pose, buffer, right, true, sy, RIGHT_Z, light, false, c, state.loop);
            if (working) {
                work(pose, buffer, right, sy, c, cycle, light);
            }
        });
        poseStack.popPose();
    }

    /** The cradle pad, the work lights and the drawer's readout: lit while it works, the readout as far as it has got. */
    private static void lights(PoseStack.Pose pose, VertexConsumer buffer, State state, boolean working) {
        int lamp = working ? GLOW : OFF;
        int lit = working ? LIGHT_FULL : state.light;
        box(pose, buffer, CX - 3.4F, CR + 0.1F, 5.6F, CX + 3.4F, CR + 0.25F, 10.4F, lamp, lit);
        box(pose, buffer, 8.1F, BASE + 0.7F, 13.7F, 23.9F, BASE + 0.85F, 14.7F, lamp, lit);
        float top = 30.0F - DROP * (1.0F - state.opening);
        box(pose, buffer, 6.1F, top - 1.25F, 5.6F, 25.9F, top - 1.1F, 6.9F, lamp, lit);
        for (int i = 0; i < 12; i++) {
            boolean on = working && state.progress * 12 > i + 0.01F;
            box(pose, buffer, 9.9F + i * 1.03F, 5.7F, 1.1F, 10.8F + i * 1.03F, 7.3F, 1.25F, on ? GLOW : OFF, on ? LIGHT_FULL : state.light);
        }
    }

    /** One arm: upper arm, elbow, forearm with its mint band, wrist, tool; the welder's tip lit, the gripper's jaws. */
    private static void arm(PoseStack.Pose pose, VertexConsumer buffer, float[] angles, boolean right, float sy,
            float[][] z, int light, boolean welding, float c, float loop) {
        float[][] p = points(angles, sy);
        segment(pose, buffer, p[0], p[1], 2.2F, z[0], ARM, ARM_TOP, right, light);
        joint(pose, buffer, p[1], 1.25F, Math.min(z[0][0], z[1][0]) - CLEAR, Math.max(z[0][1], z[1][1]) + CLEAR, right, light);
        segment(pose, buffer, p[1], p[2], 1.8F, z[1], STEEL_DARK, STEEL, right, light);
        float[] m0 = {Mth.lerp(0.4F, p[1][0], p[2][0]), Mth.lerp(0.4F, p[1][1], p[2][1])};
        float[] m1 = {Mth.lerp(0.6F, p[1][0], p[2][0]), Mth.lerp(0.6F, p[1][1], p[2][1])};
        segment(pose, buffer, m0, m1, 2.3F, new float[] {z[1][0] - CLEAR, z[1][1] + CLEAR}, MINT, MINT, right, light);
        joint(pose, buffer, p[2], 1.05F, Math.min(z[1][0], z[2][0]) - CLEAR, Math.max(z[1][1], z[2][1]) + CLEAR, right, light);
        segment(pose, buffer, p[2], p[3], 1.2F, z[2], right ? STEEL_LIGHT : STEEL, STEEL_LIGHT, right, light);
        float tx = x(p[3][0], right), ty = p[3][1];
        if (right) {
            box(pose, buffer, tx - 1.4F, ty, z[2][0] - CLEAR, tx + 1.4F, ty + 0.9F, z[2][1] + CLEAR, STEEL_LIGHT, light);
        } else {
            box(pose, buffer, tx - 0.8F, ty - 0.5F, z[2][0] - CLEAR, tx + 0.8F, ty + 0.9F, z[2][1] + CLEAR, welding ? GLOW : OFF, welding ? LIGHT_FULL : light);
            if (welding) {
                // Three sparks jumping about the tip, a new spread every tick
                int seed = (int) loop * 7 + 3;
                for (int i = 0; i < 3; i++) {
                    float sx = tx - 3.0F + 3.5F * hash(seed, i, 0), sy2 = ty - 1.0F + 3.5F * hash(seed, i, 1), sz = 6.8F + 1.6F * hash(seed, i, 2);
                    box(pose, buffer, sx, sy2, sz, sx + 0.8F, sy2 + 0.8F, sz + 0.8F, GLOW, LIGHT_FULL);
                }
            }
        }
    }

    /** The parts: in the tray, carried by the right arm, growing on the cradle; and the flash when the loop ends. */
    private static void work(PoseStack.Pose pose, VertexConsumer buffer, float[] right, float sy, float c, int cycle, int light) {
        boolean body = cycle >= 1 || c >= 27;
        boolean chip = cycle == 1 && c >= 27;
        boolean seamA = cycle >= 1 || c >= 42;
        boolean seamB = cycle == 1 && c >= 46;
        // The part waiting in the tray, then in the gripper
        if (c < 12) {
            if (cycle == 0) {
                part(pose, buffer, SLOT, LEDGE, light);
            } else {
                box(pose, buffer, SLOT - 0.8F, LEDGE, 7.2F, SLOT + 0.8F, LEDGE + 1.2F, 8.8F, MINT, light);
            }
        } else if (c < 27) {
            float[][] p = points(right, sy);
            float tx = x(p[3][0], true), ty = p[3][1];
            if (cycle == 0) {
                part(pose, buffer, tx, ty - BH, light);
            } else {
                box(pose, buffer, tx - 0.8F, ty - 1.2F, 7.2F, tx + 0.8F, ty, 8.8F, MINT, light);
            }
        }
        if (body) {
            part(pose, buffer, CX, CR, light);
            box(pose, buffer, CX - 1.2F, CR + 0.3F, 6.4F, CX + 1.2F, CR + 1.2F, 6.5F, MINT, light);
            if (seamA) {
                box(pose, buffer, CX - 1.7F, CR + 1.7F, 6.35F, CX + 1.7F, CR + 2.2F, 6.5F, GLOW, LIGHT_FULL);
            }
        }
        if (chip) {
            box(pose, buffer, CX - 0.8F, CR + BH, 7.2F, CX + 0.8F, CR + 4.7F, 8.8F, MINT, light);
            if (seamB) {
                box(pose, buffer, CX - 0.5F, CR + 4.7F, 7.5F, CX + 0.5F, CR + 4.9F, 8.5F, GLOW, LIGHT_FULL);
            }
        }
        if (cycle == 1 && c >= 46) {
            // The finished trinket flashes: a ring of light spreading on the cradle
            float f = Math.min(1.0F, (c - 46) / 4.0F);
            float r = 3.0F + f * 4.0F;
            box(pose, buffer, CX - r, CR, 8 - r, CX + r, CR + 0.4F, 8 - r + 0.8F, GLOW, LIGHT_FULL);
            box(pose, buffer, CX - r, CR, 8 + r - 0.8F, CX + r, CR + 0.4F, 8 + r, GLOW, LIGHT_FULL);
            box(pose, buffer, CX - r, CR, 8 - r, CX - r + 0.8F, CR + 0.4F, 8 + r, GLOW, LIGHT_FULL);
            box(pose, buffer, CX + r - 0.8F, CR, 8 - r, CX + r, CR + 0.4F, 8 + r, GLOW, LIGHT_FULL);
        }
    }

    /** The trinket's body: a dark block with a steel top, its bottom at {@code y}, centred on {@code cx}. */
    private static void part(PoseStack.Pose pose, VertexConsumer buffer, float cx, float y, int light) {
        box(pose, buffer, cx - 2.0F, y, 6.5F, cx + 2.0F, y + BH - 0.4F, 9.5F, ARM, light);
        box(pose, buffer, cx - 2.0F, y + BH - 0.4F, 6.5F, cx + 2.0F, y + BH, 9.5F, STEEL, light);
    }

    // ---------------------------------------------------------------- kinematics

    /** Joint angles that put the tool's tip at (u, y), the tool pointing straight down. */
    static float[] ik(float u, float y) {
        float wu = u, wy = y + L3;
        float dx = wu - SU, dy = wy - SY;
        float dist = (float) Math.hypot(dx, dy);
        float al = (float) Math.atan2(dy, dx);
        float g = (float) Math.acos((L1 * L1 + dist * dist - L2 * L2) / (2 * L1 * dist));
        float ex = SU + L1 * Mth.cos(al - g), ey = SY + L1 * Mth.sin(al - g);
        float a1 = (float) Math.toDegrees(Math.atan2(ex - SU, -(ey - SY)));
        float f2 = (float) Math.toDegrees(Math.atan2(wu - ex, -(wy - ey)));
        return new float[] {a1, f2 < -90 ? f2 + 360 : f2, 0.0F};
    }

    /** Shoulder, elbow, wrist and tip along u, from the angles, the shoulder at height {@code sy}. */
    static float[][] points(float[] angles, float sy) {
        float[][] p = new float[4][];
        p[0] = new float[] {SU, sy};
        float[] lengths = {L1, L2, L3};
        for (int i = 0; i < 3; i++) {
            float r = angles[i] * Mth.DEG_TO_RAD;
            p[i + 1] = new float[] {p[i][0] + lengths[i] * Mth.sin(r), p[i][1] - lengths[i] * Mth.cos(r)};
        }
        return p;
    }

    /** Where the timeline puts the arm at tick {@code c} of the cycle: eased between its key poses. */
    static float[] pose(Object[][] keys, Map<String, float[]> poses, float c) {
        for (int i = 0; i + 1 < keys.length; i++) {
            int c0 = (int) keys[i][0], c1 = (int) keys[i + 1][0];
            if (c >= c0 && c <= c1) {
                float q = c1 > c0 ? ModelParts.smooth(c, c0, c1) : 1.0F;
                float[] a = poses.get((String) keys[i][1]), b = poses.get((String) keys[i + 1][1]);
                return new float[] {Mth.lerp(q, a[0], b[0]), Mth.lerp(q, a[1], b[1]), Mth.lerp(q, a[2], b[2])};
            }
        }
        return REST;
    }

    // ---------------------------------------------------------------- drawing in the sketch's coordinates

    /** u along an arm into the sketch's x: the left arm hangs at x = u, the right one mirrored at 32 - u. */
    private static float x(float u, boolean right) {
        return right ? 32.0F - u : u;
    }

    /** The sketch's x into the model's: the bench's own cell mirrored, the frames' cell on the model's -x. */
    private static float mx(float x) {
        return 16.0F - x;
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int colour, int light) {
        ModelParts.box(pose, buffer, mx(x1), y0, z0, mx(x0), y1, z1, colour, colour, light);
    }

    /** A bar of width {@code w} from a to b (in u, y), between z bands, turned in the arm's plane. */
    private static void segment(PoseStack.Pose pose, VertexConsumer buffer, float[] a, float[] b, float w, float[] z, int colour, int top,
            boolean right, int light) {
        float ax = mx(x(a[0], right)), ay = a[1], bx = mx(x(b[0], right)), by = b[1];
        float length = (float) Math.hypot(bx - ax, by - ay);
        if (length < 1.0E-3F) {
            return;
        }
        PoseStack.Pose turned = pose.copy();
        turned.translate(ax, ay, 0.0F);
        turned.rotate(Axis.ZP.rotation((float) Math.atan2(-(bx - ax), by - ay)));
        ModelParts.box(turned, buffer, -w / 2, 0.0F, z[0], w / 2, length, z[1], top, colour, light);
    }

    private static void joint(PoseStack.Pose pose, VertexConsumer buffer, float[] at, float half, float z0, float z1, boolean right, int light) {
        float cx = x(at[0], right);
        box(pose, buffer, cx - half, at[1] - half, z0, cx + half, at[1] + half, z1, STEEL_DARK, light);
    }

    /** A steady pseudo-random 0..1 for the sparks. */
    private static float hash(int seed, int i, int axis) {
        int h = seed * 73856093 ^ i * 19349663 ^ axis * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h >>> 8 & 0xFFFF) / 65535.0F;
    }

    /** The 2x2: its cell, the frame beside it and the two above. */
    @Override
    public AABB getRenderBoundingBox(TrinketTableBlockEntity entity) {
        Direction side = entity.getBlockState().getValue(MachineBlock.FACING).getCounterClockWise();
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 1.0, side.getStepZ());
    }
}
