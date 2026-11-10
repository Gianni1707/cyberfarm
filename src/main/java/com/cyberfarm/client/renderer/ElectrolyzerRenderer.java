package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Electrolyzer's feeder punch: four strokes per ingot into the bath, each followed by a grain of alumina falling
 * from the spout. It follows the recipe's progress, so it stops when the machine does.
 * <p>
 * 3 boxes, 18 quads per machine, only while lit (the lit model leaves the punch out), within the default 64 blocks.
 * Coordinates are model pixels of the north-facing model (tools/machinemodels.py, electrolyzer()).
 */
public class ElectrolyzerRenderer extends BlockEntityRenderer<ElectrolyzerBlockEntity, ElectrolyzerRenderer.State> {
    private static final int STROKES = 4;
    /** The head's underside at rest and at the bottom of the stroke. */
    private static final float HEAD_REST = 13.9F, HEAD_LOW = 10.8F, HEAD_H = 1.0F, ROD_TOP = 15.0F;
    /** The grain's underside at the spout and on the bath, and the stroke phases it falls between. */
    private static final float GRAIN_TOP = 13.45F, GRAIN_LAND = 10.8F, GRAIN_START = 0.55F, GRAIN_END = 0.8F;
    private static final int STEEL = 0xFFBDC6C5, ROD = 0xFFB1BEBC;

    public static class State extends BlockEntityRenderState {
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
        if (!state.lit) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
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
        return new AABB(entity.getBlockPos());
    }
}
