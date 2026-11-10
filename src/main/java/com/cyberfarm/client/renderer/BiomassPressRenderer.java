package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.BiomassPressBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.MovingBlockRenderState;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Biomass Press: the mash in its chamber, as high as what is inside it; the tower rising out of the low walls with its plate, then the
 * drive and funnel through the crown; at work, the plate pushing slowly down on its rod, squashing the mash a little, holding, and lifting.
 * <p>
 * The mash's model while there is any, 2 boxes, 12 quads only while lit (the lit model leaves them out, the unlit one has them at rest), and
 * during the 2.5 s of an opening the moving parts' models ({@link MachinePartBlock}). Coordinates are model pixels (tools/machinemodels.py, biomass_press_*()).
 */
public class BiomassPressRenderer extends BlockEntityRenderer<BiomassPressBlockEntity, BiomassPressRenderer.State> {
    /** Ticks: a press, its push and its hold; the rest is the lift. */
    private static final float CYCLE = BiomassPressBlockEntity.PROCESS_TIME, DOWN = 22.0F, HOLD = 6.0F;
    private static final float PLATE_UP = 22.5F, PLATE_HEIGHT = 2.0F, ROD_TOP = 26.0F;
    private static final float TOWER_SINK = 12.0F, HEAD_SINK = 16.0F;
    /** The mash's floor and the top of its model; the least it shows; how much the plate squashes it; under the lone press's plate, the most there is room for. */
    private static final float MASH_FLOOR = 5.2F, MASH_TOP = 13.0F, MASH_LEAST = 0.6F, SQUASH = 0.7F, LONE_TOP = 10.2F;
    /** A stack's worth fills it. */
    private static final float MASH_FULL = 64.0F;
    private static final float SPIN_UP = 10.0F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final int STEEL = 0xFFBDC6C5, STEEL_SIDE = 0xFFB1BEBC, ROD = 0xFF7C8989, ROD_SIDE = 0xFF5B7771;

    public static class State extends BlockEntityRenderState {
        int stage;
        float t;
        MovingBlockRenderState @Nullable [] parts;
        @Nullable MovingBlockRenderState mash;
        float mashTop;
        boolean lit;
        Direction facing = Direction.NORTH;
        int light;
        float plate;
    }

    private static float eased(float x) {
        return x * x * (3.0F - 2.0F * x);
    }

    /** From 0, the plate up, to 1, fully down. */
    private static float press(float tick) {
        float t = tick % CYCLE;
        return t < DOWN ? eased(t / DOWN) : t < DOWN + HOLD ? 1.0F : eased(1.0F - (t - DOWN - HOLD) / (CYCLE - DOWN - HOLD));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BiomassPressBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        state.lit = false;
        state.parts = null;
        state.mash = null;
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        int contents = entity.contents();
        float top = MASH_FLOOR + MASH_LEAST + (MASH_TOP - MASH_FLOOR - MASH_LEAST) * Math.min(contents / MASH_FULL, 1.0F);
        state.mashTop = state.stage == SmallMultiblockBlock.OPEN ? top : Math.min(top, LONE_TOP);
        if (contents > 0) {
            state.mash = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.PRESS_MASH, state.facing);
        }
        float time = level.getGameTime() + partialTick;
        state.t = entity.shownOpening(state.stage, time);
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, time)) {
            state.parts = new MovingBlockRenderState[] {ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.PRESS_TOWER, state.facing),
                    ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.PRESS_HEAD, state.facing)};
        }
        state.lit = state.stage == SmallMultiblockBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        if (!state.lit) {
            entity.motionSpeed = 0.0F;
            entity.motionPhase = 0.0F;
            entity.motionTime = 0.0F;
            return;
        }
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        float tick = ModelParts.advance(entity, time, 1.0F, SPIN_UP);
        // It comes down on the mash, whatever its height, and presses it
        float plateDown = Math.max(state.mashTop - SQUASH, MASH_FLOOR + MASH_LEAST - SQUASH);
        state.plate = PLATE_UP + (plateDown - PLATE_UP) * press(tick);
        state.mashTop = Math.min(state.mashTop, state.plate);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MovingBlockRenderState mash = state.mash;
        if (mash != null) {
            // Its model is at its fullest: squeezed down onto the floor, its flecks stay as they are
            poseStack.pushPose();
            poseStack.translate(0.0F, MASH_FLOOR * PIXEL, 0.0F);
            poseStack.scale(1.0F, (state.mashTop - MASH_FLOOR) / (MASH_TOP - MASH_FLOOR), 1.0F);
            poseStack.translate(0.0F, -MASH_FLOOR * PIXEL, 0.0F);
            collector.submitMovingBlock(poseStack, mash, 0);
            poseStack.popPose();
        }
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            float t = state.t;
            ModelParts.slid(poseStack, collector, parts[0], state.facing, 0.0F, -TOWER_SINK * (1.0F - ModelParts.settle(t, 4.0F, 30.0F)), 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], state.facing, 0.0F, -HEAD_SINK * (1.0F - ModelParts.smooth(t, 24.0F, 46.0F)), 0.0F);
        }
        if (!state.lit) {
            return;
        }
        int light = state.light;
        float y = state.plate;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 3.6F, y, 2.1F, 12.4F, y + PLATE_HEIGHT, 12.9F, STEEL, STEEL_SIDE, light);
            ModelParts.box(pose, buffer, 6.5F, y + PLATE_HEIGHT, 6.5F, 9.5F, ROD_TOP, 9.5F, ROD, ROD_SIDE, light);
        });
        poseStack.popPose();
    }

    /** Its cell and the frame above. */
    @Override
    public AABB getRenderBoundingBox(BiomassPressBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.0, 0.0);
    }
}
