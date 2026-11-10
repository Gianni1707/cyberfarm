package com.cyberfarm.client.renderer;

import com.cyberfarm.block.CoolantMixerBlock;
import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.fluid.GasTank;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The assembled Coolant Mixer: both tanks' fluids in their sight glasses, the carriage running between them while it
 * works, and the bench opening once when it is assembled, folding back when it comes apart.
 * <p>
 * Idle, 2 boxes and up to 6 fluid quads. During the 3 s of an opening 26 boxes (156 quads), rarely. All within the default
 * 64 blocks. Coordinates are model pixels of the north-facing model (tools/machinemodels.py, coolant_mixer_parts()); the
 * tables use the design frame, as that file does.
 */
public class CoolantMixerRenderer extends BlockEntityRenderer<CoolantMixerBlockEntity, CoolantMixerRenderer.State> {
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final int LID = 0xFF395853, BODY = 0xFF233E3B, STEEL_TOP = 0xFFBDC6C5, STEEL = 0xFFB1BEBC;
    private static final int SLOT = 0xFF030909, BEAM = 0xFF5B7771, MINT = 0xFF5EC2B8, GLINT = 0x40FFFFFF;
    /** After a stage changes the renderer keeps drawing this long, a hair larger, while the chunk rebuilds under it. */
    private static final float HOLD = CoolantMixerBlockEntity.REBUILD_TICKS, GROW = 0.02F;
    private static final float T_OPEN = CoolantMixerBlockEntity.OPEN_TICKS;
    private static final float SLIDE = 4.5F, SINK = 5.1F, RISE = 6.5F, PISTON = 2.7F, RAIL_LIFT = 5.0F, SETTLE = 0.05F;
    private static final float SWING = 2.5F, RAMP = 10.0F;
    private static final float GLASS_Y0 = 9.4F, GLASS_Y1 = 15.0F, GLASS_W = 5.0F, GLASS_Z = 3.9F, WATER_GLASS_X = 7.0F, COOLANT_GLASS_X = -12.0F;

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {}

    /** From the design frame (x 0..32, front at high z) to the model's: the machine block at x 0..16, its front at low z. */
    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {
        return new Box(x0 - 16.0F, y0, 16.0F - z1, x1 - 16.0F, y1, 16.0F - z0, top, side);
    }

    private static Box[] tank(float x0) {
        float x1 = x0 + 9.0F;
        return new Box[] {
                box(x0 + 2.25F, 7.5F, 2.5F, x1 - 2.25F, 15.5F, 11.5F, LID, BODY),
                box(x0, 7.52F, 4.75F, x1, 15.48F, 9.25F, LID, BODY),
                box(x0 + 1.0F, 15.5F, 3.5F, x1 - 1.0F, 16.0F, 10.5F, LID, STEEL),
                box(x0 + 0.6F, 7.55F, 3.1F, x1 - 0.6F, 8.3F, 11.75F, STEEL_TOP, STEEL),
                box(x0 + 1.5F, 8.3F, 11.5F, x0 + 2.0F, 15.5F, 12.25F, STEEL_TOP, STEEL),
                box(x1 - 2.0F, 8.3F, 11.5F, x1 - 1.5F, 15.5F, 12.25F, STEEL_TOP, STEEL),
                box(x0 + 2.0F, 9.4F, 11.5F, x1 - 2.0F, 15.0F, 12.1F, SLOT, SLOT)};
    }

    private static final Box[] WATER_TANK = tank(21.0F), COOLANT_TANK = tank(2.0F);
    private static final Box[] WATER_SADDLE = {box(22.0F, 3.0F, 3.5F, 29.0F, 7.6F, 10.5F, BODY, BODY)};
    private static final Box[] COOLANT_SADDLE = {box(3.0F, 3.0F, 3.5F, 10.0F, 7.6F, 10.5F, BODY, BODY)};
    private static final Box[] RAIL = {box(11.5F, 5.1F, 3.0F, 20.5F, 9.9F, 4.9F, LID, BEAM), box(12.0F, 7.6F, 4.9F, 20.0F, 8.4F, 4.95F, SLOT, SLOT)};
    private static final Box[] CARRIAGE = {box(14.25F, 7.0F, 4.85F, 17.75F, 9.8F, 9.0F, LID, MINT), box(14.5F, 5.4F, 7.2F, 17.5F, 7.01F, 8.8F, STEEL_TOP, STEEL)};
    /** The hood's panels, sunk into the bench at rest: {right half, left half}, then when each pair sinks. */
    private static final Box[][] HOOD = {
            {box(3.1F, 0.16F, 12.3F, 11.4F, 4.95F, 13.4F, LID, BODY), box(20.6F, 0.16F, 12.3F, 28.9F, 4.95F, 13.4F, LID, BODY)},
            {box(3.1F, 0.16F, 1.1F, 11.4F, 4.95F, 2.3F, LID, BODY), box(20.6F, 0.16F, 1.1F, 28.9F, 4.95F, 2.3F, LID, BODY)},
            {box(1.1F, 0.16F, 3.1F, 1.9F, 4.95F, 12.9F, LID, BODY), box(30.1F, 0.16F, 3.1F, 30.9F, 4.95F, 12.9F, LID, BODY)}};
    private static final float[][] SINK_T = {{20.0F, 30.0F}, {23.0F, 33.0F}, {26.0F, 36.0F}};

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        int light;
        /** How far the opening has got, and whether the renderer still has to draw what the static model will show. */
        float t;
        boolean tanks;
        float grow;
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
        state.stage = entity.getBlockState().getValue(CoolantMixerBlock.STAGE);
        if (level == null || state.stage == CoolantMixerBlock.LOOSE) {
            state.stage = CoolantMixerBlock.LOOSE;
            entity.shownStage = CoolantMixerBlock.LOOSE;
            return;
        }
        float time = level.getGameTime() + partialTick;
        if (state.stage != entity.shownStage) {
            // Arriving at a machine that is already open, the chunk has its model: nothing to hold
            entity.shownStageTime = entity.shownStage < 0 ? time - HOLD : time;
            entity.shownStage = state.stage;
        }
        float since = time - entity.shownStageTime;
        boolean holding = state.stage != CoolantMixerBlock.OPENING && since < HOLD;
        state.tanks = state.stage != CoolantMixerBlock.OPEN || holding;
        state.grow = holding ? GROW : 0.0F;
        state.t = switch (state.stage) {
            case CoolantMixerBlock.OPENING -> Math.min(since, T_OPEN);
            case CoolantMixerBlock.CLOSING -> Math.max(T_OPEN - Math.max(since - HOLD, 0.0F), 0.0F);
            default -> T_OPEN;
        };
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        boolean working = state.stage == CoolantMixerBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        // A free phase, not the recipe's progress, which rewinds at the first status after a power cut
        float phase = ModelParts.advance(entity, time, working ? 1.0F : 0.0F, RAMP);
        state.carriage = SWING * Mth.sin(phase * Mth.TWO_PI / 20.0F);
        state.inputSprite = sprite(entity.getInput());
        state.outputSprite = sprite(entity.getOutput());
        state.inputFill = fill(entity.getInput());
        state.outputFill = fill(entity.getOutput());
        state.inputColour = tint(entity.getInput());
        state.outputColour = tint(entity.getOutput());
    }

    private static float fill(GasTank tank) {
        return tank.capacity() > 0 ? Math.min(1.0F, (float) tank.amount() / tank.capacity()) : 0.0F;
    }

    private static @Nullable TextureAtlasSprite sprite(GasTank tank) {
        return tank.amount() > 0 ? FluidSprites.still(tank.fluid()) : null;
    }

    private static int tint(GasTank tank) {
        if (tank.amount() <= 0) {
            return 0;
        }
        return FluidSprites.tint(tank.fluid(), tank.amount());
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
        if (state.stage == CoolantMixerBlock.LOOSE) {
            return;
        }
        float t = state.t;
        float inward = (1.0F - smooth(t, 0.0F, 20.0F)) * SLIDE;
        float waterRise = -RISE * (1.0F - settle(t, 30.0F, 48.0F));
        float coolantRise = -RISE * (1.0F - settle(t, 35.0F, 53.0F));
        float railRise = -RAIL_LIFT * (1.0F - smooth(t, 46.0F, 60.0F));
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            if (state.tanks) {
                draw(pose, buffer, WATER_TANK, -inward, waterRise, state.grow, light);
                draw(pose, buffer, COOLANT_TANK, inward, coolantRise, state.grow, light);
                draw(pose, buffer, WATER_SADDLE, -inward, Math.max(waterRise, -PISTON), state.grow, light);
                draw(pose, buffer, COOLANT_SADDLE, inward, Math.max(coolantRise, -PISTON), state.grow, light);
                draw(pose, buffer, RAIL, 0.0F, railRise, state.grow, light);
            }
            if (state.stage != CoolantMixerBlock.OPEN) {
                for (int i = 0; i < HOOD.length; i++) {
                    if (t < SINK_T[i][1]) {
                        float sink = SINK * (1.0F - smooth(t, SINK_T[i][0], SINK_T[i][1]));
                        draw(pose, buffer, new Box[] {HOOD[i][0]}, inward, sink, 0.0F, light);
                        draw(pose, buffer, new Box[] {HOOD[i][1]}, -inward, sink, 0.0F, light);
                    }
                }
            }
            draw(pose, buffer, CARRIAGE, state.carriage, railRise, 0.0F, light);
        });
        collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> {
            glass(pose, buffer, WATER_GLASS_X - inward, waterRise, state.inputFill, state.inputSprite, state.inputColour, light);
            glass(pose, buffer, COOLANT_GLASS_X + inward, coolantRise, state.outputFill, state.outputSprite, state.outputColour, light);
        });
        poseStack.popPose();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Box[] boxes, float dx, float dy, float grow, int light) {
        for (Box b : boxes) {
            ModelParts.box(pose, buffer, b.x0 + dx - grow, b.y0 + dy - grow, b.z0 - grow, b.x1 + dx + grow, b.y1 + dy + grow, b.z1 + grow, b.top, b.side, light);
        }
    }

    /** One sight glass: the fluid up to its level, a lighter line at its surface and a glint. Far enough off the glass not to flicker. */
    private static void glass(PoseStack.Pose pose, VertexConsumer buffer, float x, float dy, float fill, @Nullable TextureAtlasSprite sprite, int tint, int light) {
        if (sprite == null || fill <= 0.0F) {
            return;
        }
        float top = fill * (GLASS_Y1 - GLASS_Y0);
        int colour = 0xE0 << 24 | tint & 0xFFFFFF;
        int surface = 0xFF << 24 | lighter(tint);
        quad(pose, buffer, x, 0.0F, x + GLASS_W, top, dy, GLASS_Z - 0.08F, x, sprite, colour, light);
        quad(pose, buffer, x, top - 0.4F, x + GLASS_W, top, dy, GLASS_Z - 0.10F, x, sprite, surface, light);
        quad(pose, buffer, x + 0.5F, 0.0F, x + 0.9F, top, dy, GLASS_Z - 0.12F, x, sprite, GLINT, light);
    }

    private static int lighter(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r + (255 - r) * 2 / 5 << 16 | g + (255 - g) * 2 / 5 << 8 | b + (255 - b) * 2 / 5;
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
        buffer.addVertex(pose, x, GLASS_Y0 + y + dy, z).setColor(colour).setUv(sprite.getU((x - u0) / 16.0F), sprite.getV(1.0F - y / 16.0F))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, -1.0F);
    }

    @Override
    public AABB getRenderBoundingBox(CoolantMixerBlockEntity entity) {
        Direction side = CoolantMixerBlock.frameSide(entity.getBlockState());
        return new AABB(entity.getBlockPos()).expandTowards(side.getStepX(), 0.0, side.getStepZ());
    }
}
