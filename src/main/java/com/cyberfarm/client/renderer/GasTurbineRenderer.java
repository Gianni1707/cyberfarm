package com.cyberfarm.client.renderer;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
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
 * The assembled Gas Turbine: its rotor turning as fast as it makes power, the gas in the two sight glasses on its flanks,
 * and once, on assembling, the duct and nozzle sliding out, the doors swinging open and the blades blooming.
 * <p>
 * 7 boxes (42 quads) and up to 12 fluid quads once assembled; during the 3 s of an opening 13 boxes more, rarely. All within
 * the default 64 blocks. Coordinates are model pixels of the north-facing model; the tables use the design frame, as
 * tools/machinemodels.py does (gas_turbine_parts()).
 */
public class GasTurbineRenderer implements BlockEntityRenderer<GasTurbineBlockEntity, GasTurbineRenderer.State> {
    private static final int LID = 0xFF395853, BODY = 0xFF233E3B, STEEL_TOP = 0xFFBDC6C5, STEEL = 0xFFB1BEBC;
    private static final int SLOT = 0xFF030909, BEAM = 0xFF5B7771, MINT = 0xFF5EC2B8, GLINT = 0x40FFFFFF;
    private static final float HOLD_GROW = 0.02F;
    private static final float COWL_SLIDE = 7.9F, TAIL_SLIDE = 5.9F;
    /** The engine's axis; the blades' root, tip, width and depth; the hub's radius and depth. */
    private static final float CX = 8.0F, CY = 9.5F, ROOT = 1.7F, TIP = 4.3F, BLADE_W = 3.2F;
    private static final float BLADE_Z0 = 16.0F - 27.5F, BLADE_Z1 = 16.0F - 26.3F, HUB = 1.8F, HUB_Z0 = 16.0F - 28.2F, HUB_Z1 = 16.0F - 24.2F;
    private static final int BLADES = 5;
    /** Radians a tick at full speed, and the ticks to reach it. The power it makes at full speed: methane's. */
    private static final float SPEED = 0.28F, RAMP = 30.0F, FULL_POWER = 160.0F;
    /** Each door's hinge, at its wall, in the duct's front. */
    private static final float HINGE_LEFT = 3.05F, HINGE_RIGHT = 12.95F, HINGE_Z = 16.0F - 29.2F;
    /** The sight glasses: their faces' x, height and depth, filling from the back. */
    private static final float GLASS_EAST = 15.2F, GLASS_WEST = 0.8F, GLASS_Y0 = 2.2F, GLASS_Y1 = 5.2F, GLASS_FRONT = 3.0F, GLASS_BACK = 13.0F;

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {}

    /** The design frame has the front at high z. */
    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side) {
        return new Box(x0, y0, 16.0F - z1, x1, y1, 16.0F - z0, top, side);
    }

    private static final Box[] COWL = {
            box(2.0F, 3.5F, 24.0F, 14.0F, 4.5F, 30.0F, LID, MINT),
            box(2.0F, 14.5F, 24.0F, 14.0F, 15.5F, 30.0F, LID, MINT),
            box(2.0F, 4.5F, 24.0F, 3.0F, 14.5F, 30.0F, LID, MINT),
            box(13.0F, 4.5F, 24.0F, 14.0F, 14.5F, 30.0F, LID, MINT),
            box(3.0F, 4.5F, 24.0F, 13.0F, 14.5F, 24.2F, SLOT, SLOT),
            box(6.5F, 7.5F, 16.0F, 9.5F, 11.5F, 24.0F, BEAM, BEAM)};
    private static final Box[] TAIL = {
            box(6.2F, 7.7F, -6.2F, 9.8F, 11.3F, 1.2F, BEAM, BEAM),
            box(4.308F, 4.3F, -9.0F, 11.692F, 14.7F, -6.0F, LID, BODY),
            box(2.8F, 5.808F, -8.98F, 13.2F, 13.192F, -6.02F, LID, BODY),
            box(5.586F, 6.1F, -9.2F, 10.414F, 12.9F, -8.9F, SLOT, SLOT),
            box(4.6F, 7.086F, -9.18F, 11.4F, 11.914F, -8.92F, SLOT, SLOT)};
    private static final Box DOOR_LEFT = box(3.05F, 4.55F, 29.2F, 7.98F, 14.45F, 29.5F, STEEL_TOP, STEEL);
    private static final Box DOOR_RIGHT = box(8.02F, 4.55F, 29.2F, 12.95F, 14.45F, 29.5F, STEEL_TOP, STEEL);

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        int light;
        float t;
        /** Whether the renderer still has to draw what the static model will show. */
        boolean parts;
        float grow;
        float angle;
        @Nullable TextureAtlasSprite sprite;
        float fill;
        int tint;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GasTurbineBlockEntity entity, State state, float partialTick, Vec3 camera,
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
        boolean holding = entity.shownHolding(state.stage, time);
        state.parts = state.stage != SmallMultiblockBlock.OPEN || holding;
        state.grow = holding ? HOLD_GROW : 0.0F;
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        boolean working = state.stage == SmallMultiblockBlock.OPEN && entity.getBlockState().getValue(LitMachineBlock.LIT);
        // Free running: it slows down and stays wherever it stops, so a stop never jumps
        state.angle = SPEED * ModelParts.advance(entity, time, working ? Math.min(entity.status.made() / FULL_POWER, 1.0F) : 0.0F, RAMP);
        state.sprite = ModelParts.fluidSprite(entity.getTank());
        state.fill = ModelParts.fluidFill(entity.getTank());
        state.tint = ModelParts.fluidTint(entity.getTank());
    }

    private static float ease(float t, float from, float to) {
        float x = Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        float t = state.t;
        float cowl = COWL_SLIDE * (1.0F - ease(t, 0.0F, 24.0F));
        float tail = -TAIL_SLIDE * (1.0F - ease(t, 4.0F, 32.0F));
        float bloom = ease(t, 42.0F, 60.0F);
        int light = state.light;
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        if (state.parts) {
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
                draw(pose, buffer, COWL, cowl, state.grow, light);
                draw(pose, buffer, TAIL, tail, state.grow, light);
            });
            door(poseStack, collector, DOOR_LEFT, HINGE_LEFT, -ease(t, 24.0F, 40.0F) * Mth.HALF_PI, cowl, state.grow, light);
            door(poseStack, collector, DOOR_RIGHT, HINGE_RIGHT, ease(t, 26.0F, 42.0F) * Mth.HALF_PI, cowl, state.grow, light);
        }
        if (bloom >= 0.05F) {
            rotor(poseStack, collector, state.angle, bloom, cowl, light);
        }
        collector.submitCustomGeometry(poseStack, ModelParts.FLUID, (pose, buffer) -> {
            glass(pose, buffer, GLASS_EAST, 1.0F, state, light);
            glass(pose, buffer, GLASS_WEST, -1.0F, state, light);
        });
        poseStack.popPose();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, Box[] boxes, float dz, float grow, int light) {
        for (Box b : boxes) {
            drawBox(pose, buffer, b, dz, grow, light);
        }
    }

    private static void drawBox(PoseStack.Pose pose, VertexConsumer buffer, Box b, float dz, float grow, int light) {
        ModelParts.box(pose, buffer, b.x0 - grow, b.y0 - grow, b.z0 + dz - grow, b.x1 + grow, b.y1 + grow, b.z1 + dz + grow, b.top, b.side, light);
    }

    private static void door(PoseStack poseStack, SubmitNodeCollector collector, Box door, float hinge, float angle, float cowl, float grow, int light) {
        poseStack.pushPose();
        poseStack.translate(hinge, 0.0F, HINGE_Z + cowl);
        poseStack.mulPose(Axis.YP.rotation(angle));
        poseStack.translate(-hinge, 0.0F, -HINGE_Z);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> drawBox(pose, buffer, door, 0.0F, grow, light));
        poseStack.popPose();
    }

    /** The hub grows first, then the blades, one of them mint so the turning is never lost. */
    private static void rotor(PoseStack poseStack, SubmitNodeCollector collector, float angle, float bloom, float cowl, int light) {
        float hub = Math.max(0.4F, HUB * Math.min(1.0F, bloom * 2.0F));
        float cut = 0.29F * hub;
        float tip = Mth.lerp(bloom, ROOT, TIP);
        for (int i = 0; i <= BLADES; i++) {
            int blade = i;
            poseStack.pushPose();
            poseStack.translate(CX, CY, cowl);
            poseStack.mulPose(Axis.ZP.rotation(angle + (i < BLADES ? i * Mth.TWO_PI / BLADES : 0.0F)));
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
                if (blade < BLADES) {
                    int colour = blade == 0 ? MINT : STEEL;
                    ModelParts.box(pose, buffer, ROOT, -BLADE_W / 2.0F, BLADE_Z0, tip, BLADE_W / 2.0F, BLADE_Z1, blade == 0 ? MINT : STEEL_TOP, colour, light);
                } else {
                    ModelParts.box(pose, buffer, cut - hub, -hub, HUB_Z0, hub - cut, hub, HUB_Z1, BODY, BODY, light);
                    ModelParts.box(pose, buffer, -hub, cut - hub, HUB_Z0 + 0.02F, hub, hub - cut, HUB_Z1 - 0.02F, BODY, BODY, light);
                }
            });
            poseStack.popPose();
        }
    }

    /** One slit on a flank: the gas from the back to its level, a lighter line at its surface and a glint along the top. */
    private static void glass(PoseStack.Pose pose, VertexConsumer buffer, float x, float side, State state, int light) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null || state.fill <= 0.0F) {
            return;
        }
        float front = GLASS_BACK - state.fill * (GLASS_BACK - GLASS_FRONT);
        slit(pose, buffer, x, side, front, GLASS_BACK, GLASS_Y0, GLASS_Y1, sprite, 0xE0 << 24 | state.tint & 0xFFFFFF, light);
        slit(pose, buffer, x + side * 0.02F, side, front, front + 0.5F, GLASS_Y0, GLASS_Y1, sprite, 0xFF << 24 | ModelParts.lighter(state.tint), light);
        slit(pose, buffer, x + side * 0.04F, side, front, GLASS_BACK, GLASS_Y1 - 0.5F, GLASS_Y1, sprite, GLINT, light);
    }

    private static void slit(PoseStack.Pose pose, VertexConsumer buffer, float x, float side, float z0, float z1, float y0, float y1, TextureAtlasSprite sprite, int colour, int light) {
        boolean east = side > 0.0F;
        vertex(pose, buffer, x, y0, east ? z0 : z1, side, sprite, colour, light);
        vertex(pose, buffer, x, y1, east ? z0 : z1, side, sprite, colour, light);
        vertex(pose, buffer, x, y1, east ? z1 : z0, side, sprite, colour, light);
        vertex(pose, buffer, x, y0, east ? z1 : z0, side, sprite, colour, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float side, TextureAtlasSprite sprite, int colour, int light) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(sprite.getU(z / 16.0F), sprite.getV(1.0F - y / 16.0F))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, side, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(GasTurbineBlockEntity entity) {
        Direction front = entity.getBlockState().getValue(MachineBlock.FACING);
        return new AABB(entity.getBlockPos()).expandTowards(front.getStepX(), 0.0, front.getStepZ())
                .expandTowards(-front.getStepX(), 0.0, -front.getStepZ());
    }
}
