package com.cyberfarm.client.renderer;

import com.cyberfarm.block.CoolantMixerBlock;
import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;

import net.minecraft.client.Minecraft;
import com.cyberfarm.port.client.FluidSprites;
import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import com.cyberfarm.port.client.CameraRenderState;
import com.cyberfarm.port.client.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The assembled Coolant Mixer: both tanks' fluids in their sight glasses, the carriage running between them while it
 * works, and the bench opening once when it is assembled, folding back when it comes apart.
 * <p>
 * Idle, 2 boxes and up to 6 fluid quads; during the 3 s of an opening the moving parts' models too, the same as the finished
 * machine's ({@link MachinePartBlock}), so nothing changes when they stop. All within the default
 * 64 blocks. Coordinates are model pixels of the north-facing model (tools/machinemodels.py, coolant_mixer_parts()); the
 * tables use the design frame, as that file does.
 */
public class CoolantMixerRenderer extends BlockEntityRenderer<CoolantMixerBlockEntity, CoolantMixerRenderer.State> {
    private static final int LID = 0xFF395853, STEEL_TOP = 0xFFBDC6C5, STEEL = 0xFFB1BEBC;
    private static final int MINT = 0xFF5EC2B8, GLINT = 0x40FFFFFF;
    /** After a stage changes the renderer keeps drawing this long, a hair larger, while the chunk rebuilds under it. */
    private static final float SLIDE = 4.5F, SINK = 5.1F, RISE = 6.5F, PISTON = 2.7F, RAIL_LIFT = 5.0F, SETTLE = 0.05F;
    private static final float SWING = 2.5F, RAMP = 10.0F;
    private static final float GLASS_Y0 = 9.4F, GLASS_Y1 = 15.0F, GLASS_W = 5.0F, GLASS_Z = 3.9F, WATER_GLASS_X = 7.0F, COOLANT_GLASS_X = -12.0F;

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {}

    /** From the design frame (x 0..32, front at high z) to the model's: the machine block at x 0..16, its front at low z. */
    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {
        return new Box(x0 - 16.0F, y0, 16.0F - z1, x1 - 16.0F, y1, 16.0F - z0, top, side);
    }

    private static final Box[] CARRIAGE = {box(14.25F, 7.0F, 4.85F, 17.75F, 9.8F, 9.0F, LID, MINT), box(14.5F, 5.4F, 7.2F, 17.5F, 7.01F, 8.8F, STEEL_TOP, STEEL)};
    /** When each pair of hood panels sinks: front, back, ends. */
    private static final float[][] SINK_T = {{20.0F, 30.0F}, {23.0F, 33.0F}, {26.0F, 36.0F}};
    private static final int HOOD_PANELS = 6;

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        int light;
        /** How far the opening has got, and the parts' models while they aren't the static model's yet, or null. */
        float t;
        MovingBlockRenderState @Nullable [] parts;
        float carriage;
        @Nullable TextureAtlasSprite inputSprite;
        @Nullable TextureAtlasSprite outputSprite;
        float inputFill;
        float outputFill;
        int inputColour;
        int outputColour;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoolantMixerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
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
        state.parts = null;
        if (state.stage != SmallMultiblockBlock.OPEN || entity.shownHolding(state.stage, time)) {
            state.parts = new MovingBlockRenderState[MachinePartBlock.MIXER_HOOD + HOOD_PANELS];
            for (int part = MachinePartBlock.MIXER_WATER_TANK; part < state.parts.length; part++) {
                state.parts[part] = ModelParts.movingPart(level, entity.getBlockPos(), part, entity.getBlockState().getValue(MachineBlock.FACING));
            }
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        boolean working = state.stage == SmallMultiblockBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        // A free phase, not the recipe's progress, which rewinds at the first status after a power cut
        float phase = ModelParts.advance(entity, time, working ? 1.0F : 0.0F, RAMP);
        state.carriage = SWING * Mth.sin(phase * Mth.TWO_PI / 20.0F);
        state.inputSprite = ModelParts.fluidSprite(entity.getInput());
        state.outputSprite = ModelParts.fluidSprite(entity.getOutput());
        state.inputFill = ModelParts.fluidFill(entity.getInput());
        state.outputFill = ModelParts.fluidFill(entity.getOutput());
        state.inputColour = ModelParts.fluidTint(entity.getInput());
        state.outputColour = ModelParts.fluidTint(entity.getOutput());
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
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        float t = state.t;
        float inward = (1.0F - smooth(t, 0.0F, 20.0F)) * SLIDE;
        float waterRise = -RISE * (1.0F - settle(t, 30.0F, 48.0F));
        float coolantRise = -RISE * (1.0F - settle(t, 35.0F, 53.0F));
        float railRise = -RAIL_LIFT * (1.0F - smooth(t, 46.0F, 60.0F));
        int light = state.light;
        MovingBlockRenderState[] parts = state.parts;
        if (parts != null) {
            Direction f = state.facing;
            ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_WATER_TANK], f, -inward, waterRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_COOLANT_TANK], f, inward, coolantRise, 0.0F);
            ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_WATER_SADDLE], f, -inward, Math.max(waterRise, -PISTON), 0.0F);
            ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_COOLANT_SADDLE], f, inward, Math.max(coolantRise, -PISTON), 0.0F);
            ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_RAIL], f, 0.0F, railRise, 0.0F);
            for (int i = 0; i < SINK_T.length; i++) {
                if (t < SINK_T[i][1]) {
                    float sink = SINK * (1.0F - smooth(t, SINK_T[i][0], SINK_T[i][1]));
                    ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_HOOD + 2 * i], f, inward, sink, 0.0F);
                    ModelParts.slid(poseStack, collector, parts[MachinePartBlock.MIXER_HOOD + 2 * i + 1], f, -inward, sink, 0.0F);
                }
            }
        }
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> draw(pose, buffer, CARRIAGE, state.carriage, railRise, light));
        collector.submitCustomGeometry(poseStack, ModelParts.FLUID, (pose, buffer) -> {
            glass(pose, buffer, WATER_GLASS_X - inward, waterRise, state.inputFill, state.inputSprite, state.inputColour, light);
            glass(pose, buffer, COOLANT_GLASS_X + inward, coolantRise, state.outputFill, state.outputSprite, state.outputColour, light);
        });
        poseStack.popPose();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Box[] boxes, float dx, float dy, int light) {
        for (Box b : boxes) {
            ModelParts.box(pose, buffer, b.x0 + dx, b.y0 + dy, b.z0, b.x1 + dx, b.y1 + dy, b.z1, b.top, b.side, light);
        }
    }

    /** One sight glass: the fluid up to its level, a lighter line at its surface and a glint. Far enough off the glass not to flicker. */
    private static void glass(PoseStack.Pose pose, VertexConsumer buffer, float x, float dy, float fill, @Nullable TextureAtlasSprite sprite, int tint, int light) {
        if (sprite == null || fill <= 0.0F) {
            return;
        }
        float top = fill * (GLASS_Y1 - GLASS_Y0);
        int colour = 0xE0 << 24 | tint & 0xFFFFFF;
        int surface = 0xFF << 24 | ModelParts.lighter(tint);
        quad(pose, buffer, x, 0.0F, x + GLASS_W, top, dy, GLASS_Z - 0.08F, x, sprite, colour, light);
        quad(pose, buffer, x, top - 0.4F, x + GLASS_W, top, dy, GLASS_Z - 0.10F, x, sprite, surface, light);
        quad(pose, buffer, x + 0.5F, 0.0F, x + 0.9F, top, dy, GLASS_Z - 0.12F, x, sprite, GLINT, light);
    }

    /** A rectangle facing north, y from the bottom of the glass, textured from its corner at {@code u0}. */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float x1, float y1, float dy, float z, float u0,
            TextureAtlasSprite sprite, int colour, int light) {
        vertex(pose, buffer, x0, y0, dy, z, u0, sprite, colour, light);
        vertex(pose, buffer, x0, y1, dy, z, u0, sprite, colour, light);
        vertex(pose, buffer, x1, y1, dy, z, u0, sprite, colour, light);
        vertex(pose, buffer, x1, y0, dy, z, u0, sprite, colour, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float dy, float z, float u0, TextureAtlasSprite sprite, int colour, int light) {
        buffer.addVertex(pose, x, GLASS_Y0 + y + dy, z).setColor(colour).setUv(sprite.getU(16.0F * ((x - u0) / 16.0F)), sprite.getV(16.0F * (1.0F - y / 16.0F)))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    @Override
    public AABB getRenderBoundingBox(CoolantMixerBlockEntity entity) {
        Direction side = CoolantMixerBlock.frameSide(entity.getBlockState());
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 0.0, side.getStepZ());
    }
}
