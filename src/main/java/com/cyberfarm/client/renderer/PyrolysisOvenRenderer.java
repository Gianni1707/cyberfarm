package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
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
import org.jspecify.annotations.Nullable;

/**
 * The Pyrolysis Oven drawing its fibre: a shuttle runs along the retort's glowing slit once per Carbon Fiber, the fibre
 * growing behind it, and comes back quickly for the next. It follows the recipe's progress, so it stops when the machine does.
 * <p>
 * 2 boxes, 12 quads per oven, only while lit (the lit model leaves them out), within the default 64 blocks; during the 3 s of an
 * opening the moving parts' models too: the base and tube slide out, then the long tank rises.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, pyrolysis_oven_open()).
 */
public class PyrolysisOvenRenderer implements BlockEntityRenderer<PyrolysisOvenBlockEntity, PyrolysisOvenRenderer.State> {
    /** The slow pass takes this share of the recipe, the eased return the rest. */
    private static final float PASS = 0.85F;
    /** The shuttle's left edge as the model has it, at rest and at the end of the pass, and its width. */
    private static final float X_REST = 26.0F, X_END = 3.0F, WIDTH = 3.0F;
    /** The fibre's fixed end, and how much of it stays behind the shuttle's edge. */
    private static final float FIBRE_END = 29.1F, FIBRE_STUB = 1.2F;
    private static final float BASE_SLIDE = 15.0F, TUBE_SLIDE = 15.0F, TANK_SINK = 8.0F;
    private static final int MINT_TOP = 0xFFA5EAD4, MINT_SIDE = 0xFF5EC2B8, STEEL = 0xFFBDC6C5;

    public static class State extends BlockEntityRenderState {
        int stage;
        float t;
        MovingBlockRenderState @Nullable [] parts;
        boolean lit;
        Direction facing = Direction.NORTH;
        int light;
        float x;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PyrolysisOvenBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.lit = level != null && entity.getBlockState().getValue(LitMachineBlock.LIT);
        state.parts = null;
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.stage = entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        float time = level.getGameTime() + partialTick;
        state.t = entity.shownOpening(state.stage, time);
        if (state.stage == SmallMultiblockBlock.OPENING || state.stage == SmallMultiblockBlock.CLOSING
                || state.stage == SmallMultiblockBlock.OPEN && entity.shownHolding(state.stage, time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.OVEN_TANK - MachinePartBlock.OVEN_BASE + 1];
            for (int i = 0; i < state.parts.length; i++) {
                state.parts[i] = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.OVEN_BASE + i, state.facing);
            }
        }
        if (!state.lit) {
            return;
        }
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        float f = ModelParts.progress(entity, level.getGameTime() + partialTick, PyrolysisOvenBlockEntity.PROCESS_TIME);
        float pass = f < PASS ? f / PASS : (1.0F + Mth.cos((f - PASS) / (1.0F - PASS) * Mth.PI)) / 2.0F;
        state.x = Mth.lerp(pass, X_REST, X_END);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            float t = state.t;
            ModelParts.slid(poseStack, collector, parts[0], state.facing, -BASE_SLIDE * (1.0F - ModelParts.smooth(t, 0.0F, 24.0F)), 0.0F, 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], state.facing, -TUBE_SLIDE * (1.0F - ModelParts.smooth(t, 16.0F, 40.0F)), 0.0F, 0.0F);
            ModelParts.slid(poseStack, collector, parts[2], state.facing, 0.0F, -TANK_SINK * (1.0F - ModelParts.settle(t, 36.0F, 54.0F)), 0.0F);
        }
        if (!state.lit) {
            return;
        }
        float x = state.x;
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, x, 10.6F, 0.3F, x + WIDTH, 13.4F, 1.0F, MINT_TOP, MINT_SIDE, light);
            ModelParts.box(pose, buffer, x + WIDTH - FIBRE_STUB, 11.5F, 0.8F, FIBRE_END, 12.7F, 1.1F, STEEL, STEEL, light);
        });
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PyrolysisOvenBlockEntity entity) {
        Direction side = entity.getBlockState().getValue(MachineBlock.FACING).getClockWise();
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 0.0, side.getStepZ());
    }
}
