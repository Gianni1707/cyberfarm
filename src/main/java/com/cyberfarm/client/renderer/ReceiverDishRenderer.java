package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.ReceiverDishBlockEntity;
import com.cyberfarm.client.renderer.TransmitterDishRenderer.Box;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.cyberfarm.client.renderer.TransmitterDishRenderer.*;

/**
 * The Receiver Dish: on its cabinet, with its Cyber Frame hidden above, the turret (from 18.4 px) and small stepped dish
 * (12 px, axle at 26.5 px) turned to its transmitter, or to the sky when that is in another dimension. While FE arrives a
 * wave lights the rings' inner edges, rim first, then the second ring, the centre and the feed cap, then pauses; the
 * network lamp shows the network's colour. Opening, the dish rises out of the column and unfolds from the sky to its rest.
 * <p>
 * About 45 boxes. Coordinates are model pixels: .claude/sketch/dishes/dishes_r2.py's receiver (variant 1), raised 8 px
 * onto the column of tools/machinemodels.py's receiver_dish_base().
 */
public class ReceiverDishRenderer implements BlockEntityRenderer<ReceiverDishBlockEntity, ReceiverDishRenderer.State> {
    static final float AXLE = 26.5F;
    private static final float REST_TILT = 15.0F * Mth.DEG_TO_RAD;
    /** Ticks per step of the wave; it runs over five steps, the last a pause. */
    private static final float WAVE_STEP = 6.0F;
    /** The network lamp, a hair proud of the static models' dark lamp. */
    private static final Box LAMP = Box.of(5.95F, 11.45F, 1.75F, 10.05F, 12.75F, 2.0F, 0);
    private static final List<Box> TURRET = new ArrayList<>(), DISH = new ArrayList<>(), FEED = new ArrayList<>(), CENTRE = new ArrayList<>();
    /** The inner edges of the rim ring and the second ring, as 4 strips each. */
    private static final List<List<Box>> EDGES = List.of(new ArrayList<>(), new ArrayList<>());
    private static final Box FEED_CAP;

    static {
        float h = 6.0F, turntable = AXLE - 8.1F;
        TURRET.add(Box.of(-5, turntable, -5, 5, turntable + 1, 5, STEEL_DARK));
        TURRET.add(Box.of(-7.6F, turntable + 1, -3, 7.6F, turntable + 2.6F, 3, GUN));
        for (int sg = -1; sg <= 1; sg += 2) {
            TURRET.add(Box.of(sg * h, turntable + 2.6F, -2.5F, sg * (h + 1.6F), AXLE + 2.5F, 2.5F, DARK));
            TURRET.add(Box.of(sg * (h + 1.6F), AXLE - 1.6F, -1.6F, sg * (h + 2.2F), AXLE + 1.6F, 1.6F, MINT));
        }
        dish(DISH, h, 4.0F, 3.0F, 2.2F, 1.8F, 2.0F, 1.5F, 1.8F);
        octPlate(CENTRE, 2.2F, 1.0F, 0.0F, 0.9F, MINT);
        edge(EDGES.get(0), 4.0F, 1.8F, 3.0F);
        edge(EDGES.get(1), 2.2F, 0.9F, 1.8F);
        FEED_CAP = feed(FEED, h - 1.4F, 3.0F, 5.5F, 7.0F, 7.8F, 1.0F);
    }

    private static void edge(List<Box> out, float in, float w0, float w1) {
        float t = 0.45F;
        out.add(Box.of(-in, in - t, w0, in, in, w1, MINT_DIM));
        out.add(Box.of(-in, -in, w0, in, -in + t, w1, MINT_DIM));
        out.add(Box.of(in - t, -in + t, w0, in, in - t, w1, MINT_DIM));
        out.add(Box.of(-in, -in + t, w0, -in + t, in - t, w1, MINT_DIM));
    }

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        int light;
        int lamp;
        float yaw, tilt, rise;
        /** The wave on the rim's edge, the second ring's edge, the centre and the feed cap: 0 off, 1 warm, 2 lit. */
        final int[] wave = new int[4];
    }

    /** dishes_r2.wave4(): rim, second ring, centre, feed cap, then a pause. */
    static void wave(float tick, int[] out) {
        float at = tick / WAVE_STEP % 5.0F - 0.5F;
        for (int i = 0; i < 4; i++) {
            float d = Math.abs(i - at);
            out[i] = d < 0.6F ? 2 : d < 1.4F ? 1 : 0;
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ReceiverDishBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        BlockState block = entity.getBlockState();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : block.getValue(SmallMultiblockBlock.STAGE);
        state.facing = block.getValue(MachineBlock.FACING);
        state.lamp = entity.colour() < 0 ? 0 : 0xFF000000 | entity.colour();
        Arrays.fill(state.wave, 0);
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        // Called every frame, so it sees each change of stage; the dish is never in a static model, so nothing to hold
        float t = entity.shownOpening(state.stage, time);
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().above());
        state.rise = 0.0F;
        if (state.stage != SmallMultiblockBlock.OPEN) {
            float[] pose = unfolding(t, entity.openTicks(), REST_TILT);
            state.rise = pose[0];
            state.yaw = yawTo(state.facing.getStepX(), state.facing.getStepZ());
            state.tilt = pose[1];
            hold(entity, time, state.yaw, state.tilt);
            return;
        }
        GlobalPos transmitter = entity.transmitter();
        Vec3 from = Vec3.atLowerCornerOf(entity.getBlockPos()).add(0.5, AXLE * PIXEL, 0.5);
        Vec3 to = transmitter == null || transmitter.dimension() != level.dimension() ? from : transmitterDish(level, transmitter.pos());
        float[] want = aimAt(level, entity, from, state.facing, transmitter, to, REST_TILT);
        float[] shown = turn(entity, time, want[0], want[1]);
        state.yaw = shown[0];
        state.tilt = shown[1];
        if (entity.receiving()) {
            wave(time, state.wave);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int lamp = state.lamp;
        if (lamp != 0) {
            poseStack.pushPose();
            ModelParts.enterModel(poseStack, state.facing);
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> LAMP.draw(pose, buffer, lamp, LightCoordsUtil.FULL_BRIGHT));
            poseStack.popPose();
        }
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        int light = state.light;
        int[] wave = state.wave.clone();
        poseStack.pushPose();
        poseStack.translate(0.5F, state.rise * PIXEL, 0.5F);
        poseStack.mulPose(Axis.YP.rotation(state.yaw));
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> TURRET.forEach(b -> b.draw(pose, buffer, light)));
        poseStack.translate(0.0F, AXLE, 0.0F);
        poseStack.mulPose(Axis.XP.rotation(-state.tilt));
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            DISH.forEach(b -> b.draw(pose, buffer, light));
            FEED.forEach(b -> b.draw(pose, buffer, light));
            for (int i = 0; i < 2; i++) {
                int level = wave[i];
                EDGES.get(i).forEach(b -> b.draw(pose, buffer, lit(level, MINT_DIM), level > 0 ? LightCoordsUtil.FULL_BRIGHT : light));
            }
            CENTRE.forEach(b -> b.draw(pose, buffer, lit(wave[2], MINT), wave[2] > 0 ? LightCoordsUtil.FULL_BRIGHT : light));
            FEED_CAP.draw(pose, buffer, wave[3] > 0 ? GLOW : OFF, wave[3] > 0 ? LightCoordsUtil.FULL_BRIGHT : light);
        });
        poseStack.popPose();
    }

    private static int lit(int level, int off) {
        return level == 2 ? GLOW : level == 1 ? MINT_LIGHT : off;
    }

    /** Its block and the frame's above, and the dish pointing at the sky (34.3 px). */
    @Override
    public AABB getRenderBoundingBox(ReceiverDishBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.25, 0.0).inflate(0.1);
    }
}
