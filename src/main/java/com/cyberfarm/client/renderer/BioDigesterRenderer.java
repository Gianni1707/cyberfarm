package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The assembled Bio-Digester: the paddle's shadow stirring behind its porthole, winding down when it stops, the biogas in the tank's sight
 * glass, and the opening: the upper drum rising out of the lower one, its rings riding up after it, the tank sliding out along its pipe.
 * <p>
 * 4 quads for the paddle and up to 3 for the gas, within the default 64 blocks; during the 3 s of an opening the moving parts' models too
 * ({@link MachinePartBlock}). Coordinates are model pixels of the north-facing model (tools/machinemodels.py, bio_digester_parts()).
 */
public class BioDigesterRenderer implements BlockEntityRenderer<BioDigesterBlockEntity, BioDigesterRenderer.State> {
    private static final RenderType SHADOW = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    /** Ticks to spin up from still to full speed, or down again. */
    private static final float SPIN_UP = 30.0F;
    private static final int SLUDGE = 0x90253F14;
    /** The drum starts with its dome where it sits on the lone machine, the rings at the foot band, the tank inside the drum. */
    private static final float TOP_SINK = 16.0F, LOW_RING_SINK = 13.48F, HIGH_RING_SINK = 19.98F, TANK_SLIDE = 15.7F;
    /** The glass, in front of the slot at z 1.8. */
    private static final float GLASS_X = 22.3F, GLASS_W = 3.4F, GLASS_Y0 = 5.0F, GLASS_Y1 = 9.0F, GLASS_Z = 1.72F;

    public static class State extends BlockEntityRenderState {
        int stage;
        float t;
        MovingBlockRenderState @Nullable [] parts;
        Direction facing = Direction.NORTH;
        int light;
        int frontLight;
        /** How far the paddle has turned, in ticks at full speed. */
        float phase;
        @Nullable TextureAtlasSprite gas;
        float gasFill;
        int gasColour;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BioDigesterBlockEntity entity, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        state.t = entity.shownOpening(state.stage, time);
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.parts = null;
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.BIO_TANK - MachinePartBlock.BIO_TOP + 1];
            for (int i = 0; i < state.parts.length; i++) {
                state.parts[i] = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.BIO_TOP + i, state.facing);
            }
        }
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        state.frontLight = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().relative(state.facing));
        state.phase = ModelParts.advance(entity, time, entity.status.working() ? 1.0F : 0.0F, SPIN_UP);
        state.gas = ModelParts.fluidSprite(entity.getTank());
        state.gasFill = ModelParts.fluidFill(entity.getTank());
        state.gasColour = 0xC0 << 24 | ModelParts.fluidTint(entity.getTank()) & 0xFFFFFF;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        float t = state.t;
        float tankX = -TANK_SLIDE * (1.0F - ModelParts.smooth(t, 34.0F, 58.0F));
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            Direction f = state.facing;
            ModelParts.slid(poseStack, collector, parts[0], f, 0.0F, -TOP_SINK * (1.0F - ModelParts.settle(t, 4.0F, 30.0F)), 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], f, 0.0F, -LOW_RING_SINK * (1.0F - ModelParts.smooth(t, 18.0F, 36.0F)), 0.0F);
            ModelParts.slid(poseStack, collector, parts[2], f, 0.0F, -HIGH_RING_SINK * (1.0F - ModelParts.smooth(t, 12.0F, 34.0F)), 0.0F);
            ModelParts.slid(poseStack, collector, parts[3], f, tankX, 0.0F, 0.0F);
        }
        poseStack.pushPose();
        // A frame where the front looks north (-z), as in the block's model
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        collector.submitCustomGeometry(poseStack, SHADOW, (pose, buffer) -> paddle(pose, buffer, state));
        poseStack.popPose();
        TextureAtlasSprite gas = state.gas;
        if (gas != null) {
            poseStack.pushPose();
            ModelParts.enterModel(poseStack, state.facing);
            collector.submitCustomGeometry(poseStack, ModelParts.FLUID, (pose, buffer) -> glass(pose, buffer, tankX, state.gasFill, gas, state.gasColour, state.light));
            poseStack.popPose();
        }
    }

    /** The gas up to its level, in its own texture, moving with the tank. */
    private static void glass(PoseStack.Pose pose, VertexConsumer buffer, float dx, float fill, TextureAtlasSprite sprite, int colour, int light) {
        float top = GLASS_Y0 + fill * (GLASS_Y1 - GLASS_Y0);
        float x0 = GLASS_X + dx, x1 = x0 + GLASS_W;
        gasVertex(pose, buffer, x0, GLASS_Y0, x0, sprite, colour, light);
        gasVertex(pose, buffer, x0, top, x0, sprite, colour, light);
        gasVertex(pose, buffer, x1, top, x0, sprite, colour, light);
        gasVertex(pose, buffer, x1, GLASS_Y0, x0, sprite, colour, light);
    }

    private static void gasVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float u0, TextureAtlasSprite sprite, int colour, int light) {
        buffer.addVertex(pose, x, y, GLASS_Z).setColor(colour).setUv(sprite.getU((x - u0) / 16.0F), sprite.getV(1.0F - y / 16.0F))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    /** A two-armed paddle's shadow turning slowly in the sludge behind the tank's porthole. */
    private static void paddle(PoseStack.Pose pose, VertexConsumer buffer, State state) {
        float z = -0.5F + 1.25F * PIXEL - 0.002F;
        float y = 7.0F * PIXEL;
        for (int i = 0; i < 2; i++) {
            float angle = state.phase * 0.08F + i * Mth.PI;
            blade(pose, buffer, angle, 0.0F, 1.4F * PIXEL, 0.4F * PIXEL, y, z, SLUDGE, state.frontLight);
        }
    }

    /** One fan blade, a strip from {@code inner} to {@code outer} out from the middle, swept back a little. */
    private static void blade(PoseStack.Pose pose, VertexConsumer buffer, float angle, float inner, float outer, float half, float y, float z, int colour, int light) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float sweep = angle + 0.35F;
        float[][] corners = {
                {cos * inner - sin * half, sin * inner + cos * half},
                {cos * inner + sin * half, sin * inner - cos * half},
                {Mth.cos(sweep) * outer + Mth.sin(sweep) * half, Mth.sin(sweep) * outer - Mth.cos(sweep) * half},
                {Mth.cos(sweep) * outer - Mth.sin(sweep) * half, Mth.sin(sweep) * outer + Mth.cos(sweep) * half}};
        for (float[] corner : corners) {
            buffer.addVertex(pose, corner[0], y + corner[1], z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
        }
    }

    /** Its cell, the frame above and the tank beside it, whichever way it faces. */
    @Override
    public AABB getRenderBoundingBox(BioDigesterBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
    }
}
