package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.TransmitterDishBlock;
import com.cyberfarm.block.entity.TransmitterDishBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Transmitter Dish: on its 2x2 base, the turret turning about the base's centre and the stepped octagonal dish
 * tilting on its axle at 28 px, turned to the receiver it serves; the feed cap glows while it transmits, the network lamp
 * shows the network's colour. Opening, the dish rises out of the neck and unfolds from pointing at the sky to its rest.
 * <p>
 * About 40 boxes. Coordinates are model pixels, as in .claude/sketch/dishes/dishes_r2.py (proportion A).
 */
public class TransmitterDishRenderer implements BlockEntityRenderer<TransmitterDishBlockEntity, TransmitterDishRenderer.State> {
    static final int STEEL = 0xFFB1BEBC, STEEL_LIGHT = 0xFFBDC6C5, STEEL_DARK = 0xFF7C8989, GUN = 0xFF395853, DARK = 0xFF233E3B,
            DEEP = 0xFF132423, MINT = 0xFF5EC2B8, MINT_LIGHT = 0xFFA5EAD4, MINT_DIM = 0xFF488D91, GLOW = 0xFFD1FEE7, OFF = 0xFF055B57;
    static final float PIXEL = 1.0F / 16.0F;
    /** Most a dish turns per tick, in radians: a half turn in about the ticks the transmitter gives it. */
    private static final float TURN_SPEED = (float) Math.PI / (TransmitterDishBlockEntity.TURN_TICKS - 4);
    /** The share of what is left it covers per tick, so it eases onto its target instead of stopping dead. */
    private static final float EASE = 0.3F;
    static final float MIN_TILT = -10.0F * Mth.DEG_TO_RAD, SKY = 90.0F * Mth.DEG_TO_RAD;
    static final float AXLE = 28.0F;
    private static final float REST_TILT = 35.0F * Mth.DEG_TO_RAD;
    /** Opening: how far the turret rises, and the share of the opening it takes before the dish unfolds. */
    private static final float RISE = 6.0F, RISE_SHARE = 0.4F;
    /** The network lamp, a hair proud of the static model's dark lamp: alone, and on the 2x2 base. */
    private static final Box LONE_LAMP = Box.of(6.75F, 4.95F, 2.15F, 9.25F, 6.05F, 2.4F, 0);
    private static final Box BASE_LAMP = Box.of(-3.05F, 6.55F, 4.75F, 3.05F, 7.85F, 5.0F, 0);
    private static final List<Box> TURRET = new ArrayList<>(), DISH = new ArrayList<>(), FEED = new ArrayList<>();
    private static final Box FEED_CAP;

    /** What each dish shows now, {yaw, tilt, time}: the renderer turns it, the block entity doesn't need to know. */
    private static final Map<BlockEntity, float[]> SHOWN = new WeakHashMap<>();

    static {
        float h = 8.0F, turntable = AXLE - 13.0F;
        TURRET.add(Box.of(-6, turntable, -6, 6, turntable + 1.5F, 6, STEEL_DARK));
        TURRET.add(Box.of(-10.5F, turntable + 1.5F, -4.5F, 10.5F, turntable + 3.5F, 4.5F, GUN));
        for (int sg = -1; sg <= 1; sg += 2) {
            TURRET.add(Box.of(sg * h, turntable + 3.5F, -3, sg * (h + 2.5F), AXLE + 3, 3, DARK));
            TURRET.add(Box.of(sg * (h + 2.5F), AXLE - 2, -2, sg * (h + 3.4F), AXLE + 2, 2, MINT));
        }
        dish(DISH, h, 5.5F, 4.0F, 3.0F, 2.4F, 2.5F, 2.0F, 2.5F);
        octPlate(DISH, 3.0F, 1.0F, 0.0F, 1.0F, MINT);
        FEED_CAP = feed(FEED, h - 1.8F, 4.0F, 7.0F, 9.6F, 10.5F, 1.5F);
    }

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        int light;
        /** The network's colour as ARGB, or 0 for none. */
        int lamp;
        float pivotX, pivotZ, yaw, tilt, rise;
        boolean feedOn;
    }

    // ---------------------------------------------------------------- shared with ReceiverDishRenderer

    /** A box between two corners in any order, in one colour. */
    record Box(float x0, float y0, float z0, float x1, float y1, float z1, int colour) {
        static Box of(float ax, float ay, float az, float bx, float by, float bz, int colour) {
            return new Box(Math.min(ax, bx), Math.min(ay, by), Math.min(az, bz), Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz), colour);
        }

        void draw(PoseStack.Pose pose, VertexConsumer buffer, int colour, int light) {
            ModelParts.box(pose, buffer, x0, y0, z0, x1, y1, z1, colour, colour, light);
        }

        void draw(PoseStack.Pose pose, VertexConsumer buffer, int light) {
            draw(pose, buffer, colour, light);
        }
    }

    /** An octagonal ring facing +w, outer half h0, inner half h1: 4 bars and 4 stepped corners. */
    static void octBar(List<Box> out, float h0, float h1, float w0, float w1, int colour) {
        float e = h1, k = h0 - 0.45F * (h0 - h1);
        out.add(Box.of(-e, h1, w0, e, h0, w1, colour));
        out.add(Box.of(-e, -h0, w0, e, -h1, w1, colour));
        out.add(Box.of(h1, -e, w0, h0, e, w1, colour));
        out.add(Box.of(-h0, -e, w0, -h1, e, w1, colour));
        for (int su = -1; su <= 1; su += 2) {
            for (int sv = -1; sv <= 1; sv += 2) {
                out.add(Box.of(su * e, sv * e, w0, su * k, sv * k, w1, colour));
            }
        }
    }

    /** A solid octagon as a cross; the second box a hair thinner, so no two faces share a plane. */
    static void octPlate(List<Box> out, float h, float cut, float w0, float w1, int colour) {
        out.add(Box.of(-h, -(h - cut), w0, h, h - cut, w1, colour));
        out.add(Box.of(-(h - cut), -h, w0 + 0.04F, h - cut, h, w1 - 0.04F, colour));
    }

    /** The dish facing +w: back plate, hub, the rim ring and the second ring. The centre is the caller's. */
    static void dish(List<Box> out, float h, float ring2, float rimDepth, float inner, float ring2Depth, float backCut, float backDepth, float hub) {
        octPlate(out, h, backCut, -backDepth, 0.0F, STEEL_DARK);
        out.add(Box.of(-hub, -hub, -backDepth - 2.5F, hub, hub, -backDepth, DEEP));
        octBar(out, h, ring2, 0.0F, rimDepth, STEEL_LIGHT);
        octBar(out, ring2, inner, 0.0F, ring2Depth, STEEL);
    }

    /** The four struts and the feed in front of the dish; returns the feed's cap, which glows or not. */
    static Box feed(List<Box> out, float r, float strutFrom, float feed, float cap, float tip, float half) {
        for (int i = 0; i < 4; i++) {
            float du = i == 0 ? 1 : i == 1 ? -1 : 0, dv = i == 2 ? 1 : i == 3 ? -1 : 0;
            out.add(strut(du, dv, r - 0.5F, r + 0.5F, strutFrom, feed + 0.5F, 0.5F));
            out.add(strut(du, dv, half, r + 0.3F, feed, feed + 1.0F, 0.4F));
        }
        out.add(Box.of(-half, -half, feed, half, half, cap, DEEP));
        return Box.of(-half, -half, cap, half, half, tip, OFF);
    }

    private static Box strut(float du, float dv, float a0, float a1, float w0, float w1, float half) {
        return Box.of(du != 0 ? du * a0 : -half, dv != 0 ? dv * a0 : -half, w0, du != 0 ? du * a1 : half, dv != 0 ? dv * a1 : half, w1, STEEL_DARK);
    }

    /** Turns a dish toward (yaw, tilt) at a clamped rate, easing in at the end, and returns what it shows now: {yaw, tilt, time}. */
    static float[] turn(BlockEntity dish, float time, float wantYaw, float wantTilt) {
        float[] shown = SHOWN.get(dish);
        if (shown == null) {
            // First seen: already there
            shown = new float[] {wantYaw, wantTilt, time};
            SHOWN.put(dish, shown);
            return shown;
        }
        float elapsed = Mth.clamp(time - shown[2], 0.0F, 5.0F);
        shown[2] = time;
        float ease = Math.min(1.0F, EASE * elapsed), step = TURN_SPEED * elapsed;
        shown[0] += Mth.clamp(Mth.wrapDegrees((wantYaw - shown[0]) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * ease, -step, step);
        shown[1] += Mth.clamp((wantTilt - shown[1]) * ease, -step, step);
        return shown;
    }

    /** Puts the dish at a pose without turning, as when it unfolds. */
    static void hold(BlockEntity dish, float time, float yaw, float tilt) {
        SHOWN.put(dish, new float[] {yaw, tilt, time});
    }

    /** Yaw that turns the dish's +w toward this horizontal direction. */
    static float yawTo(double dx, double dz) {
        return (float) Math.atan2(dx, dz);
    }

    /**
     * The (yaw, tilt) from {@code from} to the other dish: {@code null} target = rest (the front, {@code restTilt}), another
     * dimension = the sky, keeping its yaw.
     */
    static float[] aimAt(Level level, BlockEntity dish, Vec3 from, Direction facing, GlobalPos target, Vec3 targetDish, float restTilt) {
        float front = yawTo(facing.getStepX(), facing.getStepZ());
        if (target == null) {
            return new float[] {front, restTilt};
        }
        if (target.dimension() != level.dimension()) {
            float[] shown = SHOWN.get(dish);
            return new float[] {shown != null ? shown[0] : front, SKY};
        }
        Vec3 d = targetDish.subtract(from);
        double flat = d.horizontalDistance();
        float yaw = flat < 1.0E-3 ? front : yawTo(d.x, d.z);
        return new float[] {yaw, Mth.clamp((float) Math.atan2(d.y, flat), MIN_TILT, SKY)};
    }

    /** Opening, at {@code t} of {@code open} ticks: {rise, tilt}. It rises out of the neck pointing at the sky, then unfolds to its rest. */
    static float[] unfolding(float t, float open, float restTilt) {
        return new float[] {-RISE * (1.0F - ModelParts.settle(t, 0.0F, RISE_SHARE * open)),
                SKY + (restTilt - SKY) * ModelParts.smooth(t, RISE_SHARE * open, open)};
    }

    /** Where a transmitter's dish turns: the corner its 2x2 shares, 28 px up. Its facing if it is loaded here, else its block's centre. */
    static Vec3 transmitterDish(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Vec3 pivot = state.getBlock() instanceof TransmitterDishBlock ? pivot(state.getValue(MachineBlock.FACING)) : new Vec3(0.5, 0.0, 0.5);
        return Vec3.atLowerCornerOf(pos).add(pivot.x, AXLE * PIXEL, pivot.z);
    }

    /** The centre of the 2x2, from the dish block's corner: the frames lie to its counter-clockwise side and behind it. */
    private static Vec3 pivot(Direction facing) {
        Direction side = facing.getCounterClockWise(), back = facing.getOpposite();
        return new Vec3(0.5 + 0.5 * (side.getStepX() + back.getStepX()), 0.0, 0.5 + 0.5 * (side.getStepZ() + back.getStepZ()));
    }

    // ---------------------------------------------------------------- the transmitter

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TransmitterDishBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        BlockState block = entity.getBlockState();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : block.getValue(SmallMultiblockBlock.STAGE);
        state.facing = block.getValue(MachineBlock.FACING);
        state.lamp = entity.colour() < 0 ? 0 : 0xFF000000 | entity.colour();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        float t = entity.shownOpening(state.stage, time);
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        BlockPos pos = entity.getBlockPos();
        Vec3 pivot = pivot(state.facing);
        state.pivotX = (float) pivot.x;
        state.pivotZ = (float) pivot.z;
        state.light = LightCoordsUtil.getLightCoords(level, pos.above());
        float front = yawTo(state.facing.getStepX(), state.facing.getStepZ());
        state.rise = 0.0F;
        state.feedOn = false;
        if (state.stage != SmallMultiblockBlock.OPEN) {
            // Rises out of the neck pointing at the sky, then unfolds to its rest
            float[] pose = unfolding(t, entity.openTicks(), REST_TILT);
            state.rise = pose[0];
            state.yaw = front;
            state.tilt = pose[1];
            hold(entity, time, state.yaw, state.tilt);
            return;
        }
        GlobalPos aim = entity.aim();
        Vec3 from = Vec3.atLowerCornerOf(pos).add(pivot.x, AXLE * PIXEL, pivot.z);
        Vec3 to = aim == null ? from : Vec3.atLowerCornerOf(aim.pos()).add(0.5, ReceiverDishRenderer.AXLE * PIXEL, 0.5);
        float[] want = aimAt(level, entity, from, state.facing, aim, to, REST_TILT);
        float[] shown = turn(entity, time, want[0], want[1]);
        state.yaw = shown[0];
        state.tilt = shown[1];
        state.feedOn = entity.transmitting();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int lamp = state.lamp;
        if (lamp != 0) {
            Box box = state.stage == SmallMultiblockBlock.LOOSE ? LONE_LAMP : BASE_LAMP;
            poseStack.pushPose();
            ModelParts.enterModel(poseStack, state.facing);
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> box.draw(pose, buffer, lamp, LightCoordsUtil.FULL_BRIGHT));
            poseStack.popPose();
        }
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        int light = state.light;
        boolean feedOn = state.feedOn;
        poseStack.pushPose();
        poseStack.translate(state.pivotX, state.rise * PIXEL, state.pivotZ);
        poseStack.mulPose(Axis.YP.rotation(state.yaw));
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> TURRET.forEach(b -> b.draw(pose, buffer, light)));
        poseStack.translate(0.0F, AXLE, 0.0F);
        poseStack.mulPose(Axis.XP.rotation(-state.tilt));
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            DISH.forEach(b -> b.draw(pose, buffer, light));
            FEED.forEach(b -> b.draw(pose, buffer, light));
            FEED_CAP.draw(pose, buffer, feedOn ? GLOW : OFF, feedOn ? LightCoordsUtil.FULL_BRIGHT : light);
        });
        poseStack.popPose();
    }

    /** The 2x2 and the dish above it, pointing at the sky. */
    @Override
    public AABB getRenderBoundingBox(TransmitterDishBlockEntity entity) {
        Direction facing = entity.getBlockState().getValue(MachineBlock.FACING);
        Direction side = facing.getCounterClockWise(), back = facing.getOpposite();
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 1.6, side.getStepZ()).expandTowards(back.getStepX(), 0.0, back.getStepZ());
    }
}
