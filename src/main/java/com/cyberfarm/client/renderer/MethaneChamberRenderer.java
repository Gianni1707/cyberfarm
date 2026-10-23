package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.IntakeGateBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a formed Methane Extractor chamber as one smooth tank, its blocks hidden: a metal base and
 * cap with glowing bands, a glass cylinder between them held by four pillars with orange light
 * strips, the acid pool bubbling inside, a hatch with a pipe collar for every port, a roll-up door
 * for every Intake Gate.
 * Works in a frame centred on the chamber's axis, y from the extractor's bottom face.
 */
public class MethaneChamberRenderer implements BlockEntityRenderer<MethaneExtractorBlockEntity, MethaneChamberRenderer.State> {
    private static final RenderType METAL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/methane_chamber.png"));
    private static final RenderType GLASS = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/methane_chamber_glass.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private static final int SEGMENTS = 32;
    private static final float RADIUS = 1.5F;
    private static final float GLASS_RADIUS = 1.32F;
    private static final float FLOOR = -2.0F;
    private static final float CEILING = 0.0F;
    /** The pool fills at most this much of the chamber's 2 blocks, so the mob's head shows. */
    private static final float MAX_DEPTH = 1.6F;

    private static final int SHELL = 0xFFE3E9ED;
    private static final int DARK = 0xFF2A3138;
    private static final int GLASS_TINT = 0x50A8E4F0;
    private static final int CYAN = 0xFF2FE0FF;
    private static final int ORANGE = 0xFFFF8C1A;
    private static final int ACID = 0xB09BE04A;
    private static final int ACID_GLOW = 0xFF9BE04A;
    private static final int HAZARD = 0xFFE0B020;

    /** A port or gate of the chamber: its angle around the axis, its layer and what it is. */
    record Part(float angle, int layer, Kind kind, boolean open) {}

    enum Kind { ACID_INLET, METHANE_OUTLET, GATE }

    public static class State extends BlockEntityRenderState {
        boolean formed;
        @Nullable TextureAtlasSprite acidSprite;
        float depth;
        float acid;
        float time;
        boolean filling;
        int light;
        List<Part> parts = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MethaneExtractorBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        BlockState block = entity.getBlockState();
        Level level = entity.getLevel();
        state.formed = block.getValue(MethaneExtractorBlock.FORMED) && level != null;
        if (!state.formed) {
            return;
        }
        BlockPos top = entity.getBlockPos();
        state.time = level.getGameTime() + partialTick;
        state.light = LevelRenderer.getLightCoords(level, top.above());
        state.acid = entity.shownAcid(state.time);
        state.filling = entity.isFilling();
        state.depth = MAX_DEPTH * state.acid;
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(ModFluids.ACID.get().defaultFluidState());
        state.acidSprite = model.stillMaterial().sprite();
        List<Part> parts = new ArrayList<>();
        for (BlockPos pos : MethaneExtractorBlockEntity.chamberBlocks(top)) {
            int dx = pos.getX() - top.getX();
            int dz = pos.getZ() - top.getZ();
            if (dx == 0 && dz == 0) {
                continue;
            }
            BlockState part = level.getBlockState(pos);
            Kind kind = part.is(ModBlocks.ACID_INLET.get()) ? Kind.ACID_INLET : part.is(ModBlocks.METHANE_OUTLET.get()) ? Kind.METHANE_OUTLET
                    : part.is(ModBlocks.INTAKE_GATE.get()) ? Kind.GATE : null;
            if (kind != null) {
                parts.add(new Part((float) Math.atan2(dz, dx), pos.getY() - top.getY(), kind,
                        kind == Kind.GATE && part.getValue(IntakeGateBlock.OPEN)));
            }
        }
        state.parts = parts;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        int light = state.light;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            // Base under the floor, floor plate, cap over the ceiling with its extractor hub
            side(pose, buffer, RADIUS, -3.0F, FLOOR, SHELL, light);
            ring(pose, buffer, GLASS_RADIUS, RADIUS, FLOOR, 1, SHELL, light);
            ring(pose, buffer, 0.0F, GLASS_RADIUS, FLOOR + 0.002F, 1, DARK, light);
            side(pose, buffer, RADIUS, CEILING, 1.0F, SHELL, light);
            ring(pose, buffer, GLASS_RADIUS, RADIUS, CEILING, -1, SHELL, light);
            ring(pose, buffer, 0.0F, GLASS_RADIUS, CEILING - 0.002F, -1, DARK, light);
            ring(pose, buffer, 0.62F, RADIUS, 1.0F, 1, SHELL, light);
            side(pose, buffer, 0.62F, 1.0F, 1.18F, DARK, light);
            ring(pose, buffer, 0.0F, 0.62F, 1.18F, 1, DARK, light);
            for (int i = 0; i < 4; i++) {
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                pillar(pose, buffer, a, light);
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            side(pose, buffer, RADIUS + 0.004F, FLOOR - 0.28F, FLOOR - 0.18F, CYAN, full);
            side(pose, buffer, RADIUS + 0.004F, CEILING + 0.16F, CEILING + 0.26F, CYAN, full);
            ring(pose, buffer, 0.38F, 0.48F, 1.181F, 1, CYAN, full);
            // The "shower head" over the mob
            ring(pose, buffer, 0.25F, 0.42F, CEILING - 0.004F, -1, ACID_GLOW, full);
            for (int i = 0; i < 4; i++) {
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                strip(pose, buffer, a, full);
            }
            bubbles(pose, buffer, state, full);
            if (state.filling) {
                splashes(pose, buffer, state, full);
            }
        });
        TextureAtlasSprite sprite = state.acidSprite;
        if (sprite != null && state.depth > 0.01F) {
            float surface = FLOOR + 0.01F + state.depth;
            collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> pool(pose, buffer, sprite, surface));
        }
        if (sprite != null && state.filling) {
            float surface = FLOOR + 0.01F + state.depth;
            collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> stream(pose, buffer, sprite, surface, state.time));
        }
        collector.submitCustomGeometry(poseStack, GLASS, (pose, buffer) -> side(pose, buffer, GLASS_RADIUS, FLOOR, CEILING, GLASS_TINT, light));

        for (Part part : state.parts) {
            poseStack.pushPose();
            // +x now points out of the chamber through the part
            poseStack.mulPose(Axis.YP.rotation(-part.angle()));
            if (part.kind() == Kind.GATE) {
                gate(poseStack, collector, part.open(), light);
            } else {
                port(poseStack, collector, part, light, state.filling, state.time);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** A hatch on the shell at the port's layer, with a collar reaching out to the block's face for the pipe. */
    private static void port(PoseStack poseStack, SubmitNodeCollector collector, Part part, int light, boolean filling, float time) {
        float mid = part.layer() + 0.5F;
        float cos = Math.abs(Mth.cos(part.angle()));
        float sin = Math.abs(Mth.sin(part.angle()));
        float reach = Math.max(RADIUS + 0.08F, RADIUS / Math.max(cos, sin));
        // An inlet pulses brighter while acid flows in
        int color = part.kind() != Kind.ACID_INLET ? CYAN
                : filling ? scale(0xFFE4FFB0, 0.75F + 0.25F * Mth.sin(time * 0.5F)) : ACID_GLOW;
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            box(pose, buffer, RADIUS - 0.02F, mid - 0.3F, -0.3F, RADIUS + 0.04F, mid + 0.3F, 0.3F, DARK, light);
            box(pose, buffer, RADIUS, mid - 0.16F, -0.16F, reach, mid + 0.16F, 0.16F, SHELL, light);
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            box(pose, buffer, RADIUS + 0.041F, mid - 0.26F, -0.26F, RADIUS + 0.045F, mid - 0.2F, 0.26F, color, full);
            box(pose, buffer, RADIUS + 0.041F, mid + 0.2F, -0.26F, RADIUS + 0.045F, mid + 0.26F, 0.26F, color, full);
            box(pose, buffer, reach - 0.06F, mid - 0.17F, -0.17F, reach - 0.02F, mid + 0.17F, 0.17F, color, full);
        });
    }

    /** A door frame in the lower wall and its shutter, rolled up while the gate lets a mob in. */
    private static void gate(PoseStack poseStack, SubmitNodeCollector collector, boolean open, int light) {
        float x0 = GLASS_RADIUS - 0.04F;
        float shutterBottom = open ? -1.15F : FLOOR;
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            box(pose, buffer, x0, FLOOR, -0.5F, RADIUS, -1.0F, -0.4F, SHELL, light);
            box(pose, buffer, x0, FLOOR, 0.4F, RADIUS, -1.0F, 0.5F, SHELL, light);
            box(pose, buffer, x0, -1.15F, -0.4F, RADIUS, -1.0F, 0.4F, SHELL, light);
            if (!open) {
                box(pose, buffer, x0 + 0.04F, shutterBottom, -0.4F, x0 + 0.1F, -1.15F, 0.4F, DARK, light);
                box(pose, buffer, x0 + 0.1F, FLOOR, -0.4F, x0 + 0.11F, FLOOR + 0.1F, 0.4F, HAZARD, light);
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            int color = open ? ACID_GLOW : ORANGE;
            box(pose, buffer, RADIUS, FLOOR, -0.47F, RADIUS + 0.005F, -1.03F, -0.43F, color, full);
            box(pose, buffer, RADIUS, FLOOR, 0.43F, RADIUS + 0.005F, -1.03F, 0.47F, color, full);
        });
    }

    private static int scale(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static void pillar(PoseStack.Pose pose, VertexConsumer buffer, float angle, int light) {
        float cx = (GLASS_RADIUS + 0.06F) * Mth.cos(angle);
        float cz = (GLASS_RADIUS + 0.06F) * Mth.sin(angle);
        box(pose, buffer, cx - 0.12F, FLOOR, cz - 0.12F, cx + 0.12F, CEILING, cz + 0.12F, SHELL, light);
    }

    /** The orange light strip down the outside of a pillar. */
    private static void strip(PoseStack.Pose pose, VertexConsumer buffer, float angle, int light) {
        float cx = (GLASS_RADIUS + 0.185F) * Mth.cos(angle);
        float cz = (GLASS_RADIUS + 0.185F) * Mth.sin(angle);
        box(pose, buffer, cx - 0.03F, FLOOR + 0.1F, cz - 0.03F, cx + 0.03F, CEILING - 0.1F, cz + 0.03F, ORANGE, light);
    }

    /** Acid pouring from the extractor's head down onto the pool, its texture running downwards. */
    private static void stream(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, float surface, float time) {
        float r = 0.14F;
        float scroll = (time * 0.08F) % 1.0F;
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            // Thinner where it leaves the head, wider where it hits the pool
            float top = r * 0.6F;
            float u0 = sprite.getU((float) i / segments), u1 = sprite.getU((float) (i + 1) / segments);
            float v0 = sprite.getV(scroll), v1 = sprite.getV(Math.min(1.0F, scroll + 0.5F));
            vertex(pose, buffer, r * c1, surface, r * s1, u1, v1, ACID, LightCoordsUtil.FULL_BRIGHT, c1, 0, s1);
            vertex(pose, buffer, r * c0, surface, r * s0, u0, v1, ACID, LightCoordsUtil.FULL_BRIGHT, c0, 0, s0);
            vertex(pose, buffer, top * c0, CEILING - 0.01F, top * s0, u0, v0, ACID, LightCoordsUtil.FULL_BRIGHT, c0, 0, s0);
            vertex(pose, buffer, top * c1, CEILING - 0.01F, top * s1, u1, v0, ACID, LightCoordsUtil.FULL_BRIGHT, c1, 0, s1);
        }
    }

    /** Drops thrown up where the stream hits the pool, and ripples spreading out. */
    private static void splashes(PoseStack.Pose pose, VertexConsumer buffer, State state, int light) {
        float surface = FLOOR + 0.01F + state.depth;
        for (int i = 0; i < 10; i++) {
            float phase = (state.time * 0.06F + i * 0.1F) % 1.0F;
            float angle = i * 2.2F + state.time * 0.01F;
            float out = 0.15F + phase * 0.5F;
            float hop = 0.35F * Mth.sin(phase * Mth.PI);
            float x = out * Mth.cos(angle);
            float z = out * Mth.sin(angle);
            float s = 0.03F * (1.0F - phase) + 0.01F;
            box(pose, buffer, x - s, surface + hop - s, z - s, x + s, surface + hop + s, z + s, 0xFFC8F890, light);
        }
        float ripple = (state.time * 0.04F) % 1.0F;
        float rr = 0.2F + ripple * (GLASS_RADIUS - 0.35F);
        ring(pose, buffer, rr, rr + 0.03F, surface + 0.005F, 1, scale(ACID_GLOW, 1.0F - ripple), light);
    }

    /** Small glowing bubbles rising through the acid, each on its own loop. */
    private static void bubbles(PoseStack.Pose pose, VertexConsumer buffer, State state, int light) {
        if (state.depth < 0.1F) {
            return;
        }
        for (int i = 0; i < 14; i++) {
            float phase = (state.time * (0.012F + 0.004F * (i % 3)) + i * 0.37F) % 1.0F;
            float angle = i * 2.4F;
            float r = 0.25F + 0.9F * ((i * 0.61F) % 1.0F);
            float bx = r * Mth.cos(angle);
            float bz = r * Mth.sin(angle);
            float by = FLOOR + 0.05F + phase * (state.depth - 0.1F);
            float s = 0.025F + 0.015F * (i % 2);
            box(pose, buffer, bx - s, by - s, bz - s, bx + s, by + s, bz + s, 0xFFE4FFB0, light);
        }
    }

    private static void pool(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, float surface) {
        float r = GLASS_RADIUS - 0.03F;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = angle(i);
            float a1 = angle(i + 1);
            float x0 = r * Mth.cos(a0), z0 = r * Mth.sin(a0), x1 = r * Mth.cos(a1), z1 = r * Mth.sin(a1);
            float u0 = sprite.getU((float) i / SEGMENTS), u1 = sprite.getU((float) (i + 1) / SEGMENTS);
            vertex(pose, buffer, x1, FLOOR, z1, u1, sprite.getV(1), ACID, LightCoordsUtil.FULL_BRIGHT, Mth.cos(a1), 0, Mth.sin(a1));
            vertex(pose, buffer, x0, FLOOR, z0, u0, sprite.getV(1), ACID, LightCoordsUtil.FULL_BRIGHT, Mth.cos(a0), 0, Mth.sin(a0));
            vertex(pose, buffer, x0, surface, z0, u0, sprite.getV(0), ACID, LightCoordsUtil.FULL_BRIGHT, Mth.cos(a0), 0, Mth.sin(a0));
            vertex(pose, buffer, x1, surface, z1, u1, sprite.getV(0), ACID, LightCoordsUtil.FULL_BRIGHT, Mth.cos(a1), 0, Mth.sin(a1));
            // The surface as a fan of thin quads, the texture laid flat
            vertex(pose, buffer, 0, surface, 0, sprite.getU(0.5F), sprite.getV(0.5F), ACID, LightCoordsUtil.FULL_BRIGHT, 0, 1, 0);
            vertex(pose, buffer, 0, surface, 0, sprite.getU(0.5F), sprite.getV(0.5F), ACID, LightCoordsUtil.FULL_BRIGHT, 0, 1, 0);
            vertex(pose, buffer, x1, surface, z1, sprite.getU(0.5F + x1 / 3), sprite.getV(0.5F + z1 / 3), ACID, LightCoordsUtil.FULL_BRIGHT, 0, 1, 0);
            vertex(pose, buffer, x0, surface, z0, sprite.getU(0.5F + x0 / 3), sprite.getV(0.5F + z0 / 3), ACID, LightCoordsUtil.FULL_BRIGHT, 0, 1, 0);
        }
    }

    private static float angle(int segment) {
        return Mth.TWO_PI * segment / SEGMENTS;
    }

    /** The side of a cylinder around the axis, the texture going once around it. */
    private static void side(PoseStack.Pose pose, VertexConsumer buffer, float r, float y0, float y1, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = angle(i);
            float a1 = angle(i + 1);
            float u0 = (float) i / SEGMENTS;
            float u1 = (float) (i + 1) / SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            vertex(pose, buffer, r * c1, y0, r * s1, u1, 1, color, light, c1, 0, s1);
            vertex(pose, buffer, r * c0, y0, r * s0, u0, 1, color, light, c0, 0, s0);
            vertex(pose, buffer, r * c0, y1, r * s0, u0, 0, color, light, c0, 0, s0);
            vertex(pose, buffer, r * c1, y1, r * s1, u1, 0, color, light, c1, 0, s1);
        }
    }

    /** A flat ring (a disc when {@code inner} is 0) at height {@code y}, facing up (1) or down (-1). */
    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float inner, float outer, float y, int up, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = angle(i);
            float a1 = angle(i + 1);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            vertex(pose, buffer, inner * c0, y, inner * s0, 0, 0, color, light, 0, up, 0);
            vertex(pose, buffer, inner * c1, y, inner * s1, 1, 0, color, light, 0, up, 0);
            vertex(pose, buffer, outer * c1, y, outer * s1, 1, 1, color, light, 0, up, 0);
            vertex(pose, buffer, outer * c0, y, outer * s0, 0, 1, color, light, 0, up, 0);
        }
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1},
        };
        float[][] uv = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
        for (float[] f : faces) {
            for (int i = 0; i < 4; i++) {
                vertex(pose, buffer, f[i * 3], f[i * 3 + 1], f[i * 3 + 2], uv[i][0], uv[i][1], color, light, f[12], f[13], f[14]);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color, int light,
            float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // The tank spans the three blocks under the extractor, the port collars reach out to the corners
    @Override
    public AABB getRenderBoundingBox(MethaneExtractorBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(2.5, 0.0, 2.5).expandTowards(0.0, -3.0, 0.0).expandTowards(0.0, 0.3, 0.0);
    }
}
