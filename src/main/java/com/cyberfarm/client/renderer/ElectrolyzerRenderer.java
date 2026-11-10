package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
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
 * The Electrolyzer's feeder punch: four strokes per ingot into the bath, each followed by a grain of alumina falling
 * from the spout. It follows the recipe's progress, so it stops when the machine does.
 * <p>
 * 3 boxes, 18 quads per machine, only while lit (the lit model leaves the punch out), within the default 64 blocks; during the 3 s of an
 * opening the moving parts' models too: the base slides out, the bath and the anode rise, the bus bar drops on.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, electrolyzer_open()).
 */
public class ElectrolyzerRenderer implements BlockEntityRenderer<ElectrolyzerBlockEntity, ElectrolyzerRenderer.State> {
    private static final int STROKES = 4;
    /** The head's underside at rest and at the bottom of the stroke. */
    private static final float HEAD_REST = 13.9F, HEAD_LOW = 10.8F, HEAD_H = 1.0F, ROD_TOP = 15.0F;
    /** The grain's underside at the spout and on the bath, and the stroke phases it falls between. */
    private static final float GRAIN_TOP = 13.45F, GRAIN_LAND = 10.8F, GRAIN_START = 0.55F, GRAIN_END = 0.8F;
    private static final int STEEL = 0xFFBDC6C5, ROD = 0xFFB1BEBC;
    private static final float BASE_SLIDE = 12.0F, RISE = 7.0F, BUS_DROP = 5.0F, BUS_FROM = 44.0F;

    public static class State extends BlockEntityRenderState {
        float t;
        MovingBlockRenderState @Nullable [] parts;
        boolean lit;
        Direction facing = Direction.NORTH;
        int light;
        float stroke;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElectrolyzerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.lit = level != null && entity.getBlockState().getValue(LitMachineBlock.LIT);
        state.parts = null;
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        int stage = entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        float time = level.getGameTime() + partialTick;
        state.t = entity.shownOpening(stage, time);
        if (stage == SmallMultiblockBlock.OPENING || stage == SmallMultiblockBlock.CLOSING || stage == SmallMultiblockBlock.OPEN && entity.shownHolding(stage, time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.ELEC_BUS - MachinePartBlock.ELEC_BASE + 1];
            for (int i = 0; i < state.parts.length; i++) {
                state.parts[i] = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.ELEC_BASE + i, state.facing);
            }
        }
        if (!state.lit) {
            return;
        }
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        state.stroke = Mth.frac(ModelParts.progress(entity, level.getGameTime() + partialTick, ElectrolyzerBlockEntity.PROCESS_TIME) * STROKES);
    }

    /** Down fast, a moment on the bath, back up, then at rest for the second half. */
    private static float headY(float f) {
        if (f < 0.2F) {
            float t = f / 0.2F;
            return HEAD_REST - (HEAD_REST - HEAD_LOW) * t * t;
        }
        if (f < 0.3F) {
            return HEAD_LOW;
        }
        return f < 0.5F ? Mth.lerp((f - 0.3F) / 0.2F, HEAD_LOW, HEAD_REST) : HEAD_REST;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            float t = state.t;
            float slide = -BASE_SLIDE * (1.0F - ModelParts.smooth(t, 0.0F, 24.0F));
            ModelParts.slid(poseStack, collector, parts[0], state.facing, slide, 0.0F, 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], state.facing, slide, -RISE * (1.0F - ModelParts.settle(t, 20.0F, 36.0F)), 0.0F);
            ModelParts.slid(poseStack, collector, parts[2], state.facing, slide, -RISE * (1.0F - ModelParts.settle(t, 30.0F, 46.0F)), 0.0F);
            if (t > BUS_FROM) {
                ModelParts.slid(poseStack, collector, parts[3], state.facing, 0.0F, BUS_DROP * (1.0F - ModelParts.smooth(t, BUS_FROM, 58.0F)), 0.0F);
            }
        }
        if (!state.lit) {
            return;
        }
        float f = state.stroke;
        float head = headY(f);
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 5.8F, head, 1.45F, 8.3F, head + HEAD_H, 1.74F, STEEL, STEEL, light);
            ModelParts.box(pose, buffer, 6.65F, head + HEAD_H, 1.5F, 7.85F, ROD_TOP, 1.7F, STEEL, ROD, light);
            if (f >= GRAIN_START && f < GRAIN_END) {
                float t = (f - GRAIN_START) / (GRAIN_END - GRAIN_START);
                float y = GRAIN_TOP - (GRAIN_TOP - GRAIN_LAND) * t * t;
                ModelParts.box(pose, buffer, 9.3F, y, 1.1F, 10.3F, y + 1.0F, 2.1F, STEEL, STEEL, light);
            }
        });
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ElectrolyzerBlockEntity entity) {
        Direction side = entity.getBlockState().getValue(MachineBlock.FACING).getClockWise();
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 0.0, side.getStepZ());
    }
}
