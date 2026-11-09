package com.cyberfarm.client.renderer;

import com.cyberfarm.block.DockTankBlock;
import com.cyberfarm.block.entity.DockTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The Dock Tank's two sight windows: the real liquid up to its real level, and a float riding on it.
 * Four boxes per tank; the rest of the block is its static model.
 */
public class DockTankRenderer implements BlockEntityRenderer<DockTankBlockEntity, DockTankRenderer.State> {
    // In pixels of the north-facing model, as in tools/machinemodels.py: the window's bottom and how tall it is, and how wide
    private static final float BOTTOM = 5.6F;
    private static final float WINDOW = 6.0F;
    private static final float NORTH = 5.8F;
    private static final float SOUTH = 10.4F;
    private static final int FLOAT_TOP = 0xFFD1FEE7;
    private static final int FLOAT = 0xFFA5EAD4;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float fill;
        @Nullable TextureAtlasSprite fluid;
        int tint;
        int light;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DockTankBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        Fluid fluid = entity.tank().fluid();
        float target = (float) entity.tank().amount() / entity.tank().capacity();
        // Slides to the new level instead of jumping when a drone empties
        entity.shownFill += (target - entity.shownFill) * 0.1F;
        state.fill = entity.shownFill;
        state.facing = entity.getBlockState().getValue(DockTankBlock.FACING);
        state.fluid = target > 0.0F ? ModelParts.fluidModel(fluid).stillMaterial().sprite() : null;
        state.tint = 0xFFFFFFFF;
        if (state.fluid != null) {
            var source = ModelParts.fluidModel(fluid).fluidTintSource();
            state.tint = source != null ? 0xFF000000 | source.colorAsStack(new FluidStack(fluid, 1000)) : 0xFFFFFFFF;
        }
        boolean glows = fluid.getFluidType().getLightLevel() > 0;
        state.light = glows || level == null ? LightCoordsUtil.FULL_BRIGHT : LevelRenderer.getLightCoords(level, entity.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        float top = BOTTOM + WINDOW * state.fill;
        TextureAtlasSprite sprite = state.fluid;
        if (sprite != null && state.fill > 0.01F) {
            collector.submitCustomGeometry(poseStack, ModelParts.FLUID, (pose, buffer) -> {
                liquid(pose, buffer, sprite, 14.4F, 1.0F, top, state);
                liquid(pose, buffer, sprite, 1.6F, -1.0F, top, state);
            });
        }
        float floatAt = Mth.clamp(4.6F + WINDOW * state.fill, BOTTOM, 9.6F);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, 14.4F, floatAt, 6.6F, 14.9F, floatAt + 2.0F, 9.6F, FLOAT_TOP, FLOAT, LightCoordsUtil.FULL_BRIGHT);
            ModelParts.box(pose, buffer, 1.1F, floatAt, 6.6F, 1.6F, floatAt + 2.0F, 9.6F, FLOAT_TOP, FLOAT, LightCoordsUtil.FULL_BRIGHT);
        });
        poseStack.popPose();
    }

    /** A window's liquid: its face to the outside at {@code x} and its surface, reaching 0.3 px in. */
    private static void liquid(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, float x, float outwards, float top, State state) {
        float inner = x - 0.3F * outwards;
        float v = (top - BOTTOM) / 16.0F;
        float u0 = NORTH / 16.0F;
        float u1 = SOUTH / 16.0F;
        vertex(pose, buffer, x, BOTTOM, NORTH, sprite, u0, 0.0F, outwards, 0.0F, state);
        vertex(pose, buffer, x, BOTTOM, SOUTH, sprite, u1, 0.0F, outwards, 0.0F, state);
        vertex(pose, buffer, x, top, SOUTH, sprite, u1, v, outwards, 0.0F, state);
        vertex(pose, buffer, x, top, NORTH, sprite, u0, v, outwards, 0.0F, state);
        vertex(pose, buffer, x, top, NORTH, sprite, u0, 0.0F, 0.0F, 1.0F, state);
        vertex(pose, buffer, x, top, SOUTH, sprite, u1, 0.0F, 0.0F, 1.0F, state);
        vertex(pose, buffer, inner, top, SOUTH, sprite, u1, 0.02F, 0.0F, 1.0F, state);
        vertex(pose, buffer, inner, top, NORTH, sprite, u0, 0.02F, 0.0F, 1.0F, state);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, TextureAtlasSprite sprite, float u, float v,
            float nx, float ny, State state) {
        buffer.addVertex(pose, x, y, z).setColor(state.tint).setUv(sprite.getU(u), sprite.getV(v)).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(state.light).setNormal(pose, nx, ny, 0.0F);
    }
}
