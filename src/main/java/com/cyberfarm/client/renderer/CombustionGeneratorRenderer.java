package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The turbine vent on the Combustion Generator's chimney: six vanes that spin up when it lights and coast to a stop when the fuel is gone.
 * Always drawn, so they stop where they stop: 6 boxes, 36 quads per generator, within the default 64 blocks. Coordinates are model
 * pixels of the north-facing model (tools/machinemodels.py, combustion_generator()); the item's model has them at rest.
 */
public class CombustionGeneratorRenderer extends BlockEntityRenderer<CombustionGeneratorBlockEntity, CombustionGeneratorRenderer.State> {
    private static final float AXIS_X = 3.5F, AXIS_Z = 10.5F, Y0 = 16.4F, Y1 = 17.1F, INNER = 0.2F, OUTER = 2.3F, HALF = 0.25F;
    private static final float DEGREES_PER_TICK = 9.0F, SPIN_UP = 30.0F;
    private static final int VANES = 6;
    private static final int MINT_TOP = 0xFFA5EAD4, MINT_SIDE = 0xFF5EC2B8;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int light;
        float angle;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CombustionGeneratorBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().above());
        float target = entity.getBlockState().getValue(LitMachineBlock.LIT) ? 1.0F : 0.0F;
        state.angle = ModelParts.advance(entity, level.getGameTime() + partialTick, target, SPIN_UP) * DEGREES_PER_TICK;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        poseStack.translate(AXIS_X, 0.0F, AXIS_Z);
        for (int vane = 0; vane < VANES; vane++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.angle + vane * 360.0F / VANES));
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) ->
                    ModelParts.box(pose, buffer, -OUTER, Y0, -HALF, -INNER, Y1, HALF, MINT_TOP, MINT_SIDE, light));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The vent stands a little above its own block. */
    @Override
    public AABB getRenderBoundingBox(CombustionGeneratorBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 0.5, 0.0);
    }
}
