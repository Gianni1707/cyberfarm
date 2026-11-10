package com.cyberfarm.client.renderer;

import com.cyberfarm.port.client.FluidSprites;
import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import com.cyberfarm.port.client.LightCoordsUtil;
import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import com.cyberfarm.port.client.CameraRenderState;
import com.cyberfarm.port.client.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The assembled Acid Synthesizer: its two sight glasses, filled up to their tanks' levels (water on the left, acid on the right
 * from the front), while it works the steel collar turning round its tower and bubbles rising through the acid, and the
 * tanks and tower rising out of its base once when it is assembled, folding back when it comes apart.
 * <p>
 * At most 45 quads per machine within the default 64 blocks: the fluids always, the collar and bubbles only while lit; during
 * the 3.5 s of an opening the moving parts' models too ({@link MachinePartBlock}). Coordinates are model pixels of the north-facing
 * model (tools/machinemodels.py, acid_synthesizer_parts()).
 */
public class AcidSynthesizerRenderer extends BlockEntityRenderer<AcidSynthesizerBlockEntity, AcidSynthesizerRenderer.State> {
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final int STEEL = 0xFFBDC6C5;
    private static final int STEEL_DARK = 0xFF7C8989;
    private static final int SEAM = 0xFF071314;
    /** The turning collar: round the tower's axis, over the still one in the model (its corners stay inside these flats). */
    private static final float COLLAR_X = 8.0F, COLLAR_Z = 10.0F, COLLAR_R = 4.9F, COLLAR_INNER = 4.5F, COLLAR_Y0 = 21.11F, COLLAR_Y1 = 25.31F;
    private static final float TURN_PER_TICK = 6.0F * Mth.DEG_TO_RAD;
    /** The glasses, in front of the slots at z 1.05, far enough apart not to flicker with them or each other. */
    private static final float WATER_X = 21.83F, ACID_X = -10.17F, GLASS_W = 4.34F, GLASS_Y0 = 5.95F, GLASS_Y1 = 23.3F;
    private static final float FLUID_Z = 0.97F, SURFACE_Z = 0.95F, BUBBLE_Z = 0.93F, GLINT_Z = 0.91F;
    private static final float SURFACE_H = 0.25F, GLINT_W = 0.3F, BUBBLE = 0.6F;
    /** A fluid's texture repeats each block up the tall glasses instead of stretching. */
    private static final float TILE = 16.0F;
    private static final int WATER_COLOR = 0xE03F76E4;
    private static final int ACID_COLOR = 0xE09BE04A;
    private static final int WATER_SURFACE = 0xFF73BDFF;
    private static final int ACID_SURFACE = 0xFFEFFF81;
    private static final int GLINT_COLOR = 0x40FFFFFF;
    private static final int BUBBLE_COLOR = 0xFFE4FFB0;
    /** How far under their places the parts start: each just below the top of the base. */
    private static final float TANK_SINK = 23.0F, TOWER_SINK = 22.0F, TOP_SINK = 6.0F, CHIMNEY_SINK = 1.5F, SETTLE = 0.05F;

    public static class State extends BlockEntityRenderState {
        int stage;
        float t;
        MovingBlockRenderState @Nullable [] parts;
        @Nullable TextureAtlasSprite water;
        @Nullable TextureAtlasSprite acid;
        float waterFill;
        float acidFill;
        boolean lit;
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
        Level level = entity.getLevel();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        if (level == null) {
            return;
        }
        state.time = level.getGameTime() + partialTick;
        state.t = entity.shownOpening(state.stage, state.time);
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.parts = null;
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, state.time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.ACID_CHIMNEY - MachinePartBlock.ACID_WATER + 1];
            for (int i = 0; i < state.parts.length; i++) {
                state.parts[i] = ModelParts.movingPart(level, entity.getBlockPos(), MachinePartBlock.ACID_WATER + i, state.facing);
            }
        }
        state.waterFill = fill(entity.getInput());
        state.acidFill = fill(entity.getOutput());
        // Not isWorking(): the server drops it for a tick at the end of each batch, and everything would blink
        state.lit = state.stage == SmallMultiblockBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        state.water = FluidSprites.still(Fluids.WATER);
        state.acid = FluidSprites.still(ModFluids.ACID.get());
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
    }

    private static float fill(GasTank tank) {
        return tank.capacity() > 0 ? Math.min(1.0F, (float) tank.amount() / tank.capacity()) : 0.0F;
    }

    private static float smooth(float t, float from, float to) {
        float x = Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Rises past its place by a little, then settles. */
    private static float settle(float t, float from, float to) {
        float x = Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
        return x < 0.8F ? (1.0F + SETTLE) * smooth(x, 0.0F, 0.8F) : 1.0F + SETTLE - SETTLE * smooth(x, 0.8F, 1.0F);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite water = state.water;
        TextureAtlasSprite acid = state.acid;
        if (state.stage == SmallMultiblockBlock.LOOSE || water == null || acid == null) {
            return;
        }
        float t = state.t;
        float waterRise = -TANK_SINK * (1.0F - settle(t, 4.0F, 28.0F));
        float acidRise = -TANK_SINK * (1.0F - settle(t, 10.0F, 34.0F));
        float towerRise = -TOWER_SINK * (1.0F - settle(t, 30.0F, 50.0F));
        float topRise = towerRise - TOP_SINK * (1.0F - smooth(t, 44.0F, 58.0F));
        float chimneyRise = topRise - CHIMNEY_SINK * (1.0F - smooth(t, 58.0F, 68.0F));
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            Direction f = state.facing;
            ModelParts.slid(poseStack, collector, parts[0], f, 0.0F, waterRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[1], f, 0.0F, acidRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[2], f, 0.0F, towerRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[3], f, 0.0F, topRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[4], f, 0.0F, chimneyRise, 0.0F);
        }
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        if (state.lit) {
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> collar(pose, buffer, state.time * TURN_PER_TICK, state.light));
        }
        collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> {
            glass(pose, buffer, WATER_X, waterRise, state.waterFill, water, WATER_COLOR, WATER_SURFACE, state.light);
            float top = glass(pose, buffer, ACID_X, acidRise, state.acidFill, acid, ACID_COLOR, ACID_SURFACE, state.light);
            if (state.lit && top > GLASS_Y0 + 1.0F) {
                // Three bubbles, each rising on its own loop through the acid
                for (int i = 0; i < 3; i++) {
                    float phase = (state.time * 0.05F + i / 3.0F) % 1.0F;
                    float x = ACID_X + 0.6F + i * 1.3F;
                    float y = GLASS_Y0 + phase * (top - GLASS_Y0 - BUBBLE);
                    north(pose, buffer, x, y, x + BUBBLE, y + BUBBLE, 0.0F, BUBBLE_Z, ACID_X, y, acid, BUBBLE_COLOR, state.light);
                }
            }
        });
        poseStack.popPose();
    }

    /** One sight glass: the fluid up to {@code fill}, a lighter line at its surface and a glint; returns the surface's height at rest. */
    private static float glass(PoseStack.Pose pose, VertexConsumer buffer, float x, float dy, float fill, TextureAtlasSprite sprite, int colour, int surface, int light) {
        float top = GLASS_Y0 + fill * (GLASS_Y1 - GLASS_Y0);
        if (fill > 0.0F) {
            for (float y = GLASS_Y0; y < top; y += TILE) {
                float end = Math.min(y + TILE, top);
                north(pose, buffer, x, y, x + GLASS_W, end, dy, FLUID_Z, x, y, sprite, colour, light);
                north(pose, buffer, x + GLINT_W, y, x + 2 * GLINT_W, end, dy, GLINT_Z, x, y, sprite, GLINT_COLOR, light);
            }
            north(pose, buffer, x, top - SURFACE_H, x + GLASS_W, top, dy, SURFACE_Z, x, top - SURFACE_H, sprite, surface, light);
        }
        return top;
    }

    /** A rectangle facing north (the player at the front), textured from its corner at {@code u0, v0}. */
    private static void north(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float x1, float y1, float dy, float z, float u0, float v0,
            TextureAtlasSprite sprite, int colour, int light) {
        fluidVertex(pose, buffer, x0, y0, dy, z, u0, v0, sprite, colour, light);
        fluidVertex(pose, buffer, x0, y1, dy, z, u0, v0, sprite, colour, light);
        fluidVertex(pose, buffer, x1, y1, dy, z, u0, v0, sprite, colour, light);
        fluidVertex(pose, buffer, x1, y0, dy, z, u0, v0, sprite, colour, light);
    }

    private static void fluidVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float dy, float z, float u0, float v0, TextureAtlasSprite sprite, int colour, int light) {
        buffer.addVertex(pose, x, y + dy, z).setColor(colour).setUv(sprite.getU(16.0F * ((x - u0) / TILE)), sprite.getV(16.0F * (1.0F - (y - v0) / TILE)))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    /** Twelve faces and their top ring, with a dark seam on every third face so it reads as turning. */
    private static void collar(PoseStack.Pose pose, VertexConsumer buffer, float angle, int light) {
        for (int k = 0; k < 12; k++) {
            float a0 = angle + k * Mth.PI / 6;
            float a1 = a0 + Mth.PI / 6;
            float mid = (a0 + a1) / 2;
            float cos0 = Mth.cos(a0), sin0 = Mth.sin(a0), cos1 = Mth.cos(a1), sin1 = Mth.sin(a1);
            float x0 = COLLAR_X + COLLAR_R * cos0, z0 = COLLAR_Z + COLLAR_R * sin0;
            float x1 = COLLAR_X + COLLAR_R * cos1, z1 = COLLAR_Z + COLLAR_R * sin1;
            int side = k % 3 == 0 ? SEAM : k % 2 == 0 ? STEEL : STEEL_DARK;
            float nx = Mth.cos(mid), nz = Mth.sin(mid);
            ModelParts.vertex(pose, buffer, x1, COLLAR_Y1, z1, side, light, nx, 0.0F, nz);
            ModelParts.vertex(pose, buffer, x1, COLLAR_Y0, z1, side, light, nx, 0.0F, nz);
            ModelParts.vertex(pose, buffer, x0, COLLAR_Y0, z0, side, light, nx, 0.0F, nz);
            ModelParts.vertex(pose, buffer, x0, COLLAR_Y1, z0, side, light, nx, 0.0F, nz);
            int top = k % 3 == 0 ? SEAM : STEEL;
            ModelParts.vertex(pose, buffer, x1, COLLAR_Y1, z1, top, light, 0.0F, 1.0F, 0.0F);
            ModelParts.vertex(pose, buffer, x0, COLLAR_Y1, z0, top, light, 0.0F, 1.0F, 0.0F);
            ModelParts.vertex(pose, buffer, COLLAR_X + COLLAR_INNER * cos0, COLLAR_Y1, COLLAR_Z + COLLAR_INNER * sin0, top, light, 0.0F, 1.0F, 0.0F);
            ModelParts.vertex(pose, buffer, COLLAR_X + COLLAR_INNER * cos1, COLLAR_Y1, COLLAR_Z + COLLAR_INNER * sin1, top, light, 0.0F, 1.0F, 0.0F);
        }
    }

    /** The whole machine, a cell to each side and one up, whichever way it faces. */
    @Override
    public AABB getRenderBoundingBox(AcidSynthesizerBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
    }
}
