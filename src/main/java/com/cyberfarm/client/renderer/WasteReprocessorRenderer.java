package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
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
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Waste Reprocessor's carousel: a disc of four pockets turning a quarter at each quarter of the recipe, what each holds
 * changing as it goes round: rod waiting, molten at the top, glass, empty. It only ever turns forward, and stops at a pocket.
 * <p>
 * Up to 8 boxes, 48 quads per machine, always (the models leave the pockets out), within the default 64 blocks; during the 2.5 s of an
 * opening the moving parts' models too: the plate grows with the plaque on it, the disc moves up, the magazine comes out.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, waste_reprocessor_open(); alone, waste_reprocessor_parts()).
 */
public class WasteReprocessorRenderer implements BlockEntityRenderer<WasteReprocessorBlockEntity, WasteReprocessorRenderer.State> {
    private static final int STATIONS = 4;
    private static final float QUARTER = Mth.TWO_PI / STATIONS;
    private static final float CX = 8.0F, POCKET = 3.2F, CONTENT = 2.6F;
    /** The disc's centre height and its pockets' ring: alone, and on the assembled machine. */
    private static final float CY_ALONE = 9.0F, CY_OPEN = 10.6F, RING_ALONE = 3.6F, RING_OPEN = 4.0F;
    private static final float PLATE_SINK = 10.0F, MAGAZINE_SLIDE = 1.4F;
    private static final float POCKET_Z0 = 3.39F, POCKET_Z1 = 3.54F, CONTENT_Z0 = 3.09F;
    /** The share of each quarter of the recipe that the snap takes: 17 ticks of 50. */
    private static final float SNAP = 0.34F;
    /** Faster than a snap's peak, so it keeps up with the recipe and catches up after a reset. */
    private static final float MAX_TURN_PER_TICK = 0.2F;
    private static final int SLOT = 0xFF030909, MELT = 0xFFD1FEE7, MELT_OFF = 0xFF055B57, GLASS = 0xFF233E3B, ROD = 0xFF5EC2B8;
    /** What a pocket at each station shows: 0 the top, 1 the right, 2 the bottom, 3 the left, as the player sees the front. */
    private static final int[] AT_STATION = {MELT, GLASS, 0, ROD};

    public static class State extends BlockEntityRenderState {
        float t;
        /** How far the disc has gone from its place alone to its place assembled, 0 to 1. */
        float lift;
        MovingBlockRenderState @Nullable [] parts;
        Direction facing = Direction.NORTH;
        int light;
        boolean lit;
        float angle;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WasteReprocessorBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LevelRenderer.getLightCoords(level, entity.getBlockPos());
        state.lit = entity.getBlockState().getValue(LitMachineBlock.LIT);
        float time = level.getGameTime() + partialTick;
        int stage = entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        state.t = entity.shownOpening(stage, time);
        state.lift = stage == SmallMultiblockBlock.LOOSE ? 0.0F : ModelParts.smooth(state.t, 28.0F, 42.0F);
        state.parts = null;
        if (stage == SmallMultiblockBlock.OPENING || stage == SmallMultiblockBlock.CLOSING || stage == SmallMultiblockBlock.OPEN && entity.shownHolding(stage, time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.REPRO_MAGAZINE - MachinePartBlock.REPRO_TOP + 1];
            for (int i = 0; i < state.parts.length; i++) {
                state.parts[i] = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.REPRO_TOP + i, state.facing);
            }
        }
        float elapsed = entity.motionTime == 0.0F ? 0.0F : Mth.clamp(time - entity.motionTime, 0.0F, 5.0F);
        entity.motionTime = time;
        float target = state.lit
                ? recipeAngle(ModelParts.progress(entity, time, WasteReprocessorBlockEntity.PROCESS_TIME))
                : Mth.ceil(entity.motionPhase / QUARTER - 0.001F) * QUARTER;
        entity.motionPhase = turnedTowards(entity.motionPhase, target, elapsed * MAX_TURN_PER_TICK);
        state.angle = entity.motionPhase;
    }

    /** The disc's angle for a recipe's progress: a snap and a pause per quarter. */
    private static float recipeAngle(float progress) {
        float u = progress * STATIONS;
        int quarter = Math.min(Mth.floor(u), STATIONS - 1);
        return QUARTER * (quarter + ease((u - quarter) / SNAP));
    }

    /** Only forward: a target a little behind (the progress corrected by the server) is waited for, a reset is gone round to. */
    static float turnedTowards(float angle, float target, float maxTurn) {
        float ahead = Mth.positiveModulo(target - angle, Mth.TWO_PI);
        return ahead < 3.0F * QUARTER ? Mth.positiveModulo(angle + Math.min(ahead, maxTurn), Mth.TWO_PI) : angle;
    }

    private static float ease(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            float plate = -PLATE_SINK * (1.0F - ModelParts.settle(state.t, 2.0F, 30.0F));
            ModelParts.slid(poseStack, collector, parts[0], state.facing, 0.0F, plate, 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], state.facing, 0.0F, (CY_ALONE - CY_OPEN) * (1.0F - state.lift), 0.0F);
            ModelParts.slid(poseStack, collector, parts[2], state.facing, 0.0F, plate, MAGAZINE_SLIDE * (1.0F - ModelParts.smooth(state.t, 36.0F, 50.0F)));
        }
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        poseStack.translate(CX, Mth.lerp(state.lift, CY_ALONE, CY_OPEN), 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(state.angle));
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            for (int k = 0; k < STATIONS; k++) {
                pocket(pose, buffer, k, state);
            }
        });
        poseStack.popPose();
    }

    /** One pocket at the place it had before the disc turned; its contents go with where it now is. */
    private static void pocket(PoseStack.Pose pose, VertexConsumer buffer, int k, State state) {
        float th = Mth.HALF_PI - k * QUARTER;
        float ring = Mth.lerp(state.lift, RING_ALONE, RING_OPEN);
        float x = -ring * Mth.cos(th), y = ring * Mth.sin(th);
        square(pose, buffer, x, y, POCKET / 2.0F, POCKET_Z0, POCKET_Z1, SLOT, state.light);
        float station = Mth.positiveModulo(k + state.angle / QUARTER, (float) STATIONS);
        int leaving = Mth.floor(station) % STATIONS, entering = (leaving + 1) % STATIONS;
        float t = ease((station - Mth.floor(station)) / SNAP);
        int from = AT_STATION[leaving], to = AT_STATION[entering];
        float size = Mth.lerp(t, from != 0 ? 1.0F : 0.0F, to != 0 ? 1.0F : 0.0F);
        if (size > 0.02F) {
            int colour = ARGB.srgbLerp(t, from != 0 ? shown(from, state.lit) : shown(to, state.lit), to != 0 ? shown(to, state.lit) : shown(from, state.lit));
            boolean molten = (t < 0.5F ? from : to) == MELT;
            square(pose, buffer, x, y, CONTENT * size / 2.0F, CONTENT_Z0, POCKET_Z0, colour, molten && state.lit ? LightCoordsUtil.FULL_BRIGHT : state.light);
        }
    }

    private static int shown(int colour, boolean lit) {
        return colour == MELT && !lit ? MELT_OFF : colour;
    }

    private static void square(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float half, float z0, float z1, int colour, int light) {
        ModelParts.box(pose, buffer, x - half, y - half, z0, x + half, y + half, z1, colour, colour, light);
    }

    @Override
    public AABB getRenderBoundingBox(WasteReprocessorBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
