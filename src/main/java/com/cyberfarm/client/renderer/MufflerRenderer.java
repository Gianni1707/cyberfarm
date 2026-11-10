package com.cyberfarm.client.renderer;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.MufflerBlockEntity;
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
 * The Muffler's damper: a heavy weight bobbing on a coil, faster and further the more kinds of sound it is set to silence, coming to
 * rest when it silences none. 6 boxes, 36 quads per Muffler, within the default 64 blocks. Coordinates are model pixels
 * (tools/machinemodels.py, muffler_parts()); the item's model has them at rest.
 */
public class MufflerRenderer extends BlockEntityRenderer<MufflerBlockEntity, MufflerRenderer.State> {
    /** The weight's underside at rest, its height, how far it bobs and how long a bob takes at full speed. */
    private static final float REST = 10.6F, HEIGHT = 3.0F, TRAVEL = 1.6F, PERIOD = 22.0F, SPIN_UP = 30.0F;
    private static final float COIL_BASE = 7.5F, RING = 0.3F;
    private static final int RINGS = 4;
    private static final int TOP = 0xFF395853, SIDE = 0xFF233E3B, MINT_TOP = 0xFFA5EAD4, MINT = 0xFF5EC2B8, STEEL_TOP = 0xFFBDC6C5, STEEL = 0xFFB1BEBC;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int light;
        float weight = REST;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MufflerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().above());
        int on = 0;
        for (MufflerBlockEntity.Category category : MufflerBlockEntity.Category.values()) {
            on += entity.isMuting(category) ? 1 : 0;
        }
        float phase = ModelParts.advance(entity, level.getGameTime() + partialTick, on / (float) MufflerBlockEntity.Category.values().length, SPIN_UP);
        state.weight = REST + TRAVEL * entity.motionSpeed * Mth.sin(phase * Mth.TWO_PI / PERIOD);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float weight = state.weight;
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 4.0F, weight, 4.0F, 12.0F, weight + HEIGHT, 12.0F, TOP, SIDE, light);
            ModelParts.box(pose, buffer, 3.6F, weight + 1.1F, 3.6F, 12.4F, weight + 1.9F, 12.4F, MINT_TOP, MINT, light);
            for (int i = 0; i < RINGS; i++) {
                float y = COIL_BASE + (i + 0.5F) * (weight - COIL_BASE) / RINGS;
                ModelParts.box(pose, buffer, 5.0F, y - RING, 5.0F, 11.0F, y + RING, 11.0F, STEEL_TOP, STEEL, light);
            }
        });
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MufflerBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
