package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Fabricator printing a circuit, like paper out of a printer: the card comes out from under the acid drum in
 * seven short steps over the recipe, the print head sweeping the drum's lip before each one, while the pinch roller
 * at the exit and the feed roller behind turn with it and the chip comes out between them; the bar on the tray
 * shows how far the recipe is. When a card is done it flashes away and the next one starts.
 * <p>
 * Drawn only while the machine works (the lit model leaves the card out and the lip lamp off; the unlit one shows
 * the card at rest): about 36 quads per Fabricator, within the default 64 blocks. Coordinates are model pixels of
 * the north-facing model (tools/machinemodels.py, fabricator()).
 */
public class FabricatorRenderer implements BlockEntityRenderer<FabricatorBlockEntity, FabricatorRenderer.State> {
    private static final RenderType CARD = RenderTypes.entityCutout(CyberFarm.id("textures/entity/fabricator_card.png"));
    private static final RenderType GLOW = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    private static final int LAMP = 0xD1FEE7;
    private static final int STEEL = 0xFFBDC6C5;
    private static final int STEEL_DARK = 0xFFB1BEBC;
    private static final int SEAM = 0xFF071314;
    private static final int WHITE = 0xFFFFFFFF;
    /** The card: its width, thickness and length; its front edge from under the pinch roller to its rest on the tray. */
    private static final float CARD_X0 = 4.75F, CARD_X1 = 11.25F, CARD_Y = 10.0F, CARD_T = 0.3F, CARD_LEN = 3.3F;
    private static final float Z_START = 5.6F, Z_REST = 2.25F;
    /** The chip on the card; nothing of it behind the nip, so it comes out of the rollers as the card feeds. */
    private static final float CHIP_X0 = 6.5F, CHIP_X1 = 9.5F, CHIP_FRONT = 0.5F, CHIP_BACK = 2.3F, CHIP_H = 0.75F, NIP = 4.6F;
    /** Over the fallback rollers in the model (their corners stay inside these 12-gons' flats). */
    private static final float PINCH_Y = 11.08F, PINCH_Z = 5.4F, PINCH_R = 0.77F;
    private static final float FEED_Y = 11.25F, FEED_Z = 12.25F, FEED_R = 1.37F, FEED_X0 = 3.5F, FEED_X1 = 12.5F;
    /** Seven feeds over progress 0..0.85, then the card rests until it is done; each feed takes this many ticks. */
    private static final int STEPS = 7;
    private static final float TRAVEL_END = 0.85F;
    private static final float FEED_TICKS = 4.0F;
    private static final float FLASH_TICKS = 6.0F;
    /** The progress comes every 20 ticks: carried on for no longer, so a stalled recipe stops the card. */
    private static final float CARRY = 25.0F;
    private static final float LIP_X0 = 5.5F, LIP_X1 = 10.5F, HEAD = 1.5F;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int light;
        /** Nothing printing: no card, head or bar. */
        boolean idle = true;
        float progress;
        /** The card's front edge, the print head's right end (or NaN between sweeps), how far the rollers have run. */
        float cardZ = Z_REST;
        float head = Float.NaN;
        float travel;
        /** Ticks since the last card was done (the flash), or past FLASH_TICKS. */
        float sinceDone = Float.MAX_VALUE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FabricatorBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.idle = true;
        state.sinceDone = Float.MAX_VALUE;
        if (level == null) {
            return;
        }
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().above());
        float time = level.getGameTime() + partialTick;
        state.sinceDone = entity.cards > 0 ? time - entity.doneTime : Float.MAX_VALUE;
        // A player who arrives mid-recipe doesn't know how long it takes: an empty bed until the next one starts
        if (!entity.status.working() || entity.shownTime == 0 || entity.doneTime > entity.startTime) {
            return;
        }
        boolean fresh = entity.statusTime > entity.startTime && entity.status.progress() >= 0.0F;
        float base = fresh ? entity.status.progress() : 0.0F;
        float since = time - (fresh ? entity.statusTime : entity.startTime);
        float p = Mth.clamp(base + Math.min(since, CARRY) / entity.shownTime, 0.0F, 1.0F);
        state.idle = false;
        state.progress = p;
        float travelled = motion(p * entity.shownTime, entity.shownTime, state);
        state.travel = entity.cards * (Z_START - Z_REST) + travelled;
    }

    /** Sets the card's place and the print head for {@code ticks} into a recipe of {@code recipe} ticks; returns the card's travel. */
    private static float motion(float ticks, float recipe, State state) {
        float stride = (Z_START - Z_REST) / STEPS;
        float stepLength = TRAVEL_END * recipe / STEPS;
        state.head = Float.NaN;
        if (ticks >= TRAVEL_END * recipe) {
            state.cardZ = Z_REST;
            return Z_START - Z_REST;
        }
        int step = (int) (ticks / stepLength);
        float into = ticks - step * stepLength;
        float sweep = Math.max(1.0F, stepLength - FEED_TICKS);
        float moved = 0.0F;
        if (into < sweep) {
            // The head crosses the lip from the right end to the left as the player sees it
            state.head = LIP_X1 - (LIP_X1 - LIP_X0 - HEAD) * into / sweep;
        } else {
            float t = Mth.clamp((into - sweep) / FEED_TICKS, 0.0F, 1.0F);
            moved = t * t * (3.0F - 2.0F * t);
        }
        float travelled = (step + moved) * stride;
        state.cardZ = Z_START - travelled;
        return travelled;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean flashing = state.sinceDone < FLASH_TICKS;
        if (state.idle && !flashing) {
            return;
        }
        poseStack.pushPose();
        // A frame where the front looks north (-z), as in the block's model, in model pixels
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        poseStack.translate(-8.0F, 0.0F, -8.0F);
        collector.submitCustomGeometry(poseStack, CARD, (pose, buffer) -> {
            if (!state.idle) {
                card(pose, buffer, state.cardZ, state.light);
                roller(pose, buffer, CARD_X0, CARD_X1, PINCH_Y, PINCH_Z, PINCH_R, -state.travel / PINCH_R, state.light);
                roller(pose, buffer, FEED_X0, FEED_X1, FEED_Y, FEED_Z, FEED_R, -state.travel / FEED_R, state.light);
            }
            if (flashing && state.sinceDone < 2.0F) {
                card(pose, buffer, Z_REST, state.light);   // the finished card, under its flash for a moment
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            if (!state.idle) {
                if (!Float.isNaN(state.head)) {
                    north(pose, buffer, state.head - HEAD, 11.98F, state.head, 12.7F, 5.44F, 0xFF << 24 | LAMP);
                }
                float bar = 8.0F * state.progress;
                if (bar > 0.05F) {
                    north(pose, buffer, 12.0F - bar, 8.0F, 12.0F, 9.0F, 1.99F, 0xFF << 24 | LAMP);
                }
            }
            if (flashing) {
                flash(pose, buffer, state.sinceDone / FLASH_TICKS);
            }
        });
        poseStack.popPose();
    }

    /** The card with its front edge at {@code z}, and its chip as far as it has come out of the nip. */
    private static void card(PoseStack.Pose pose, VertexConsumer buffer, float z, int light) {
        float y1 = CARD_Y + CARD_T;
        // Top: the card texture's texels 0..8 x 0..4 are the model's x 4..12, z 2..6 at rest (v1 lands on the front edge)
        box(pose, buffer, CARD_X0, CARD_Y, z, CARD_X1, y1, z + CARD_LEN, light,
                (CARD_X0 - 4.0F) / 16, (0.25F + CARD_LEN) / 16, (CARD_X1 - 4.0F) / 16, 0.25F / 16, 0.0F, 4.0F / 16, 8.0F / 16, 5.0F / 16);
        float back = Math.min(z + CHIP_BACK, NIP);
        float front = z + CHIP_FRONT;
        if (back - front > 0.02F) {
            float shown = (back - front) / (CHIP_BACK - CHIP_FRONT);
            box(pose, buffer, CHIP_X0, y1, front, CHIP_X1, y1 + CHIP_H, back, light,
                    8.0F / 16, 0.0F, 11.0F / 16, 2.0F * shown / 16, 8.0F / 16, 2.0F / 16, 14.0F / 16, 3.0F / 16);
        }
    }

    /** A box with no bottom: its top samples (tu0, tv0)-(tu1, tv1), its sides (su0, sv0)-(su1, sv1). */
    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1,
            int light, float tu0, float tv0, float tu1, float tv1, float su0, float sv0, float su1, float sv1) {
        quad(pose, buffer, WHITE, light, 0, 1, 0, tu0, tv0, tu1, tv1, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, WHITE, light, 0, 0, -1, su0, sv0, su1, sv1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, WHITE, light, 0, 0, 1, su0, sv0, su1, sv1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(pose, buffer, WHITE, light, -1, 0, 0, su0, sv0, su1, sv1, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, buffer, WHITE, light, 1, 0, 0, su0, sv0, su1, sv1, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** A twelve-faced steel roller along x, turned by {@code angle}, with a dark seam on every third face so it reads as turning. */
    private static void roller(PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1, float cy, float cz, float r, float angle, int light) {
        float white = 15.5F / 16;
        for (int k = 0; k < 12; k++) {
            float a0 = angle + k * Mth.PI / 6;
            float a1 = a0 + Mth.PI / 6;
            float mid = (a0 + a1) / 2;
            float y0 = cy + r * Mth.sin(a0), z0 = cz + r * Mth.cos(a0);
            float y1 = cy + r * Mth.sin(a1), z1 = cz + r * Mth.cos(a1);
            int colour = k % 3 == 0 ? SEAM : k % 2 == 0 ? STEEL : STEEL_DARK;
            quad(pose, buffer, colour, light, 0, Mth.sin(mid), Mth.cos(mid), white, white, white, white,
                    x0, y1, z1, x0, y0, z0, x1, y0, z0, x1, y1, z1);
        }
    }

    /** A glowing face looking north (to the player at the front), from (x0, y0) to (x1, y1) at depth z. */
    private static void north(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float x1, float y1, float z, int colour) {
        glowQuad(pose, buffer, colour, 0, 0, -1, x0, y0, z, x0, y1, z, x1, y1, z, x1, y0, z);
    }

    /** The finished card's flash: a shell of light round it, widening and fading away like the Drone Assembler's print. */
    private static void flash(PoseStack.Pose pose, VertexConsumer buffer, float t) {
        float grow = 0.12F * (1.0F - (1.0F - t) * (1.0F - t));
        int colour = (int) (0x70 * (1.0F - t)) << 24 | LAMP;
        float x0 = CARD_X0 - grow, x1 = CARD_X1 + grow;
        float y0 = CARD_Y - grow, y1 = CARD_Y + CARD_T + CHIP_H + grow;
        float z0 = Z_REST - grow, z1 = Z_REST + CARD_LEN + grow;
        glowQuad(pose, buffer, colour, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        glowQuad(pose, buffer, colour, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        glowQuad(pose, buffer, colour, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        glowQuad(pose, buffer, colour, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        glowQuad(pose, buffer, colour, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    /** Four corners c, textured from (u0, v0) at the first corner round to (u1, v1). */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int colour, int light, float nx, float ny, float nz,
            float u0, float v0, float u1, float v1, float... c) {
        float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(colour).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }

    private static void glowQuad(PoseStack.Pose pose, VertexConsumer buffer, int colour, float nx, float ny, float nz, float... c) {
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(colour).setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
        }
    }

    @Override
    public AABB getRenderBoundingBox(FabricatorBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
