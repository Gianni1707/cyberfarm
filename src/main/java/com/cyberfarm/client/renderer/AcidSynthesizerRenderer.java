package com.cyberfarm.client.renderer;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The two windows in the Acid Synthesizer's front, filled up to their tanks' levels like a tank's:
 * water on the left, acid on the right, with bubbles rising through the acid while a batch is made.
 */
public class AcidSynthesizerRenderer implements BlockEntityRenderer<AcidSynthesizerBlockEntity, AcidSynthesizerRenderer.State> {
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    // The windows of the front texture, in its pixels: water in columns 3..6, acid in 9..12, rows 3..11
    private static final float WATER_LEFT = 3 / 16.0F;
    private static final float WATER_RIGHT = 7 / 16.0F;
    private static final float ACID_LEFT = 9 / 16.0F;
    private static final float ACID_RIGHT = 13 / 16.0F;
    private static final float BOTTOM = 4 / 16.0F;
    private static final float TOP = 13 / 16.0F;
    private static final int GLINT_COLOR = 0x40FFFFFF;
    private static final float OFFSET = 0.002F;
    private static final int WATER_COLOR = 0xE03F76E4;
    private static final int ACID_COLOR = 0xE09BE04A;
    private static final int BUBBLE_COLOR = 0xFFE4FFB0;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite water;
        @Nullable TextureAtlasSprite acid;
        float waterFill;
        float acidFill;
        boolean working;
        float time;
        Direction facing = Direction.NORTH;
        int light;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AcidSynthesizerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.waterFill = fill(entity.getInput());
        state.acidFill = fill(entity.getOutput());
        state.working = entity.isWorking();
        var models = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
        state.water = models.get(Fluids.WATER.defaultFluidState()).stillMaterial().sprite();
        state.acid = models.get(ModFluids.ACID.get().defaultFluidState()).stillMaterial().sprite();
        if (entity.getLevel() != null) {
            state.time = entity.getLevel().getGameTime() + partialTick;
            state.light = LevelRenderer.getLightCoords(entity.getLevel(), entity.getBlockPos().relative(state.facing));
        }
    }

    private static float fill(GasTank tank) {
        return tank.capacity() > 0 ? Math.min(1.0F, (float) tank.amount() / tank.capacity()) : 0.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite water = state.water;
        TextureAtlasSprite acid = state.acid;
        if (water == null || acid == null) {
            return;
        }
        Direction side = state.facing;
        collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> {
            if (state.waterFill > 0) {
                rect(pose, buffer, side, WATER_LEFT, WATER_RIGHT, BOTTOM, BOTTOM + state.waterFill * (TOP - BOTTOM), 0, water, WATER_COLOR, state.light);
            }
            float acidTop = BOTTOM + state.acidFill * (TOP - BOTTOM);
            if (state.acidFill > 0) {
                rect(pose, buffer, side, ACID_LEFT, ACID_RIGHT, BOTTOM, acidTop, 0, acid, ACID_COLOR, state.light);
            }
            // A glint down each pane, over the fluid
            float glint = 0.5F / 16.0F;
            rect(pose, buffer, side, WATER_LEFT + glint, WATER_LEFT + 2 * glint, BOTTOM, TOP, 0.0015F, water, GLINT_COLOR, state.light);
            rect(pose, buffer, side, ACID_LEFT + glint, ACID_LEFT + 2 * glint, BOTTOM, TOP, 0.0015F, acid, GLINT_COLOR, state.light);
            if (state.working && acidTop > BOTTOM + 1 / 16.0F) {
                // Three bubbles, each rising on its own loop through the acid
                for (int i = 0; i < 3; i++) {
                    float phase = (state.time * 0.05F + i / 3.0F) % 1.0F;
                    float bx = ACID_LEFT + (0.5F + i) / 16.0F;
                    float by = BOTTOM + phase * (acidTop - BOTTOM - 1 / 32.0F);
                    rect(pose, buffer, side, bx, bx + 1 / 32.0F, by, by + 1 / 32.0F, 0.001F, acid, BUBBLE_COLOR, state.light);
                }
            }
        });
    }

    /** A rectangle on the front face, in its texture's coordinates (left to right as seen from in front). */
    private static void rect(PoseStack.Pose pose, VertexConsumer buffer, Direction side, float left, float right, float bottom, float top,
            float lift, TextureAtlasSprite sprite, int color, int light) {
        Direction across = side.getCounterClockWise();
        float centreX = 0.5F + side.getStepX() * (0.5F + OFFSET + lift);
        float centreZ = 0.5F + side.getStepZ() * (0.5F + OFFSET + lift);
        float[][] corners = {{left, bottom}, {right, bottom}, {right, top}, {left, top}};
        for (float[] corner : corners) {
            float offset = corner[0] - 0.5F;
            buffer.addVertex(pose, centreX + across.getStepX() * offset, corner[1], centreZ + across.getStepZ() * offset)
                    .setColor(color)
                    .setUv(sprite.getU(corner[0]), sprite.getV(1.0F - corner[1]))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, side.getStepX(), 0.0F, side.getStepZ());
        }
    }
}
