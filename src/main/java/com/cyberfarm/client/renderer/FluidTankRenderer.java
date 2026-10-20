package com.cyberfarm.client.renderer;

import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.cyberfarm.fluid.GasTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws a tank's fluid in the window of each side, up to its level, with the fluid's own texture
 * and colour, so it works for any mod's fluid. Gases are drawn see-through.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    // The window of the side texture: columns 4..10, rows 3..12
    private static final float WINDOW_LEFT = 4 / 16.0F;
    private static final float WINDOW_RIGHT = 11 / 16.0F;
    private static final float WINDOW_BOTTOM = 3 / 16.0F;
    private static final float WINDOW_TOP = 13 / 16.0F;
    /** Just in front of the wall, so the fluid does not flicker against it. */
    private static final float OFFSET = 0.002F;
    private static final int GAS_ALPHA = 0xA0;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int color;
        float fill;
        /** Light in front of each horizontal side, by its 2D data value: the tank itself is opaque. */
        final int[] light = new int[4];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        GasTank tank = entity.getTank();
        state.sprite = null;
        if (tank.amount() <= 0) {
            return;
        }
        Fluid fluid = tank.fluid();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        FluidTintSource tint = model.fluidTintSource();
        int color = tint != null ? tint.colorAsStack(new FluidStack(fluid, tank.amount())) : 0xFFFFFFFF;
        int alpha = fluid.getFluidType().isLighterThanAir() ? GAS_ALPHA : 0xFF;
        state.sprite = model.stillMaterial().sprite();
        state.color = (alpha << 24) | (color & 0x00FFFFFF);
        state.fill = (float) tank.amount() / tank.capacity();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            state.light[side.get2DDataValue()] = entity.getLevel() != null
                    ? LevelRenderer.getLightCoords(entity.getLevel(), entity.getBlockPos().relative(side)) : state.lightCoords;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) {
            return;
        }
        float top = WINDOW_BOTTOM + state.fill * (WINDOW_TOP - WINDOW_BOTTOM);
        collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                window(pose, buffer, side, top, sprite, state.color, state.light[side.get2DDataValue()]);
            }
        });
    }

    /**
     * The fluid in one side's window, from the bottom of the window to {@code top}. Seen from
     * outside, the window's left edge is on the viewer's left whatever way the tank faces.
     */
    private static void window(PoseStack.Pose pose, VertexConsumer buffer, Direction side, float top,
            TextureAtlasSprite sprite, int color, int light) {
        Direction right = side.getCounterClockWise();
        float centreX = 0.5F + side.getStepX() * (0.5F + OFFSET);
        float centreZ = 0.5F + side.getStepZ() * (0.5F + OFFSET);
        float[][] corners = {{WINDOW_LEFT, WINDOW_BOTTOM}, {WINDOW_RIGHT, WINDOW_BOTTOM}, {WINDOW_RIGHT, top}, {WINDOW_LEFT, top}};
        for (float[] corner : corners) {
            float across = corner[0] - 0.5F;
            buffer.addVertex(pose, centreX + right.getStepX() * across, corner[1], centreZ + right.getStepZ() * across)
                    .setColor(color)
                    .setUv(sprite.getU(corner[0]), sprite.getV(1.0F - corner[1]))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, side.getStepX(), 0.0F, side.getStepZ());
        }
    }
}
