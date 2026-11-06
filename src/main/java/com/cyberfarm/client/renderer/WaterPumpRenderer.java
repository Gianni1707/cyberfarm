package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.WaterPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Water Pump's pumpjack at work: the crank turns its counterweight, the connecting rod pulls the beam's tail, the
 * beam rocks and the polished rod slides in and out of the wellhead. When the pumping stops it slows down and creeps to
 * the pose of the unlit model, before LIT drops.
 * <p>
 * 9 boxes, 54 quads per pump, only while lit (the lit model leaves these parts out), within the default 64 blocks.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, water_pump()).
 */
public class WaterPumpRenderer implements BlockEntityRenderer<WaterPumpBlockEntity, WaterPumpRenderer.State> {
    /** Beam pivot, its tail pin's and head's reach; crank centre and pin radius; the connecting rod's length, upright at rest. */
    private static final float PX = 8.0F, PY = 11.75F, TAIL_R = 6.0F, HEAD_R = 5.0F;
    private static final float CX = 12.0F, CY = 5.5F, PIN_R = 2.0F, ROD_L = 6.25F;
    /** The polished rod comes out of the stuffing box here. */
    private static final float ROD_Y0 = 7.56F;
    /** Ticks a turn at full speed, and for the speed's whole range. */
    private static final float TURN = 36.0F, SPIN_UP = 30.0F;
    /** Slow enough to stop in the rest pose without a visible jump. */
    private static final float SETTLE = 0.4F;
    /** The status comes every 20 ticks and a slow consumer leaves gaps: no pumping for this long means it has stopped. */
    private static final float STOP_AFTER = 30.0F;
    private static final int STEEL = 0xFFBDC6C5, STEEL_SIDE = 0xFFB1BEBC;
    private static final int BEAM_TOP = 0xFF7C8989, BEAM_SIDE = 0xFF5B7771;
    private static final int MINT_TOP = 0xFFA5EAD4, MINT_SIDE = 0xFF5EC2B8;

    public static class State extends BlockEntityRenderState {
        boolean lit;
        Direction facing = Direction.NORTH;
        int light;
        float crank;
        float beam;
        float pinX;
        float pinY;
        float conn;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WaterPumpBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.lit = level != null && entity.getBlockState().getValue(LitMachineBlock.LIT);
        if (!state.lit) {
            entity.motionSpeed = 0.0F;
            entity.motionPhase = 0.0F;
            entity.motionTime = 0.0F;
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        float time = level.getGameTime() + partialTick;
        state.crank = phase(entity, time) / TURN * Mth.TWO_PI;
        state.pinX = CX + PIN_R * Mth.cos(state.crank);
        state.pinY = CY + PIN_R * Mth.sin(state.crank);
        // The tail pin is where the connecting rod meets the beam's circle, the upper one: the beam never flips over
        float dx = state.pinX - PX, dy = state.pinY - PY;
        float d = Mth.sqrt(dx * dx + dy * dy);
        float a = (TAIL_R * TAIL_R - ROD_L * ROD_L + d * d) / (2.0F * d);
        float h = Mth.sqrt(TAIL_R * TAIL_R - a * a);
        float tailX = PX + (a * dx - h * dy) / d;
        float tailY = PY + (a * dy + h * dx) / d;
        state.beam = (float) Mth.atan2(tailY - PY, tailX - PX);
        state.conn = (float) Mth.atan2(tailY - state.pinY, tailX - state.pinX);
    }

    /** How far the crank has run. It keeps turning between a slow consumer's sips, and winds down to a whole turn once they end. */
    private static float phase(WaterPumpBlockEntity entity, float time) {
        if (entity.status.used() > 0.0F) {
            entity.lastPumpTime = time;
        }
        boolean pumping = time - entity.lastPumpTime < STOP_AFTER;
        if (!pumping && entity.motionSpeed == 0.0F) {
            entity.motionTime = time;   // at rest
            return entity.motionPhase;
        }
        float before = entity.motionPhase;
        float phase = ModelParts.advance(entity, time, pumping ? 1.0F : SETTLE, SPIN_UP);
        float turns = Mth.floor(phase / TURN);
        if (!pumping && turns > Mth.floor(before / TURN)) {
            entity.motionSpeed = 0.0F;
            entity.motionPhase = turns * TURN;
        }
        return entity.motionPhase;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.lit) {
            return;
        }
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        // A horse head keeps the polished rod straight: it hangs off the head's arc
        float rodTop = PY - HEAD_R * state.beam;
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 2.5F, ROD_Y0, 7.5F, 3.5F, rodTop, 8.5F, STEEL, STEEL, light);
            ModelParts.box(pose, buffer, 2.0F, rodTop - 2.2F, 7.25F, 4.0F, rodTop - 1.8F, 8.75F, STEEL, STEEL_SIDE, light);
        });
        turned(poseStack, PX, PY, state.beam);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 4.75F, PY - 0.75F, 6.9F, 14.25F, PY + 0.75F, 9.1F, BEAM_TOP, BEAM_SIDE, light);
            ModelParts.box(pose, buffer, 2.75F, PY - 1.6F, 6.6F, 5.25F, PY + 1.6F, 9.4F, MINT_TOP, MINT_SIDE, light);
            ModelParts.box(pose, buffer, 2.3F, PY - 1.05F, 6.65F, 2.75F, PY + 1.05F, 9.35F, MINT_TOP, MINT_SIDE, light);
            ModelParts.box(pose, buffer, 13.25F, PY - 0.8F, 1.95F, 14.75F, PY + 0.8F, 9.05F, BEAM_TOP, BEAM_SIDE, light);
        });
        poseStack.popPose();
        turned(poseStack, CX, CY, state.crank);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, CX - 1.0F, CY - 0.75F, 2.7F, CX + 2.4F, CY + 0.75F, 3.7F, STEEL, STEEL_SIDE, light);
            ModelParts.box(pose, buffer, CX - 1.25F, CY - 3.5F, 2.7F, CX + 1.25F, CY - 0.75F, 3.7F, MINT_TOP, MINT_SIDE, light);
        });
        poseStack.popPose();
        float pinX = state.pinX, pinY = state.pinY;
        turned(poseStack, pinX, pinY, state.conn);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) ->
                ModelParts.box(pose, buffer, pinX, pinY - 0.5F, 2.0F, pinX + ROD_L, pinY + 0.5F, 2.6F, STEEL, STEEL, light));
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Pushes a pose turned round (x, y), the front-to-back axis; pop it after use. */
    private static void turned(PoseStack poseStack, float x, float y, float angle) {
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(angle));
        poseStack.translate(-x, -y, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(WaterPumpBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
