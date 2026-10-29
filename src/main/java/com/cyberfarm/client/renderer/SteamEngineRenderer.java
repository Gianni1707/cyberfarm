package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.block.entity.SteamEngineBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a complete Steam Engine as one machine, its blocks hidden: a white induction boiler with
 * glowing orange coils, a cylinder per block with its piston moving in a glass sleeve, the steam
 * line over them, and the dynamo with copper windings and its flywheel. All on a dark plinth with a
 * cyan light line. Works in a frame centred on the boiler where +x runs back along the row.
 */
public class SteamEngineRenderer implements BlockEntityRenderer<SteamEngineBlockEntity, SteamEngineRenderer.State> {
    private static final RenderType METAL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/methane_chamber.png"));
    private static final RenderType GLASS = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/methane_chamber_glass.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));

    private static final int SEGMENTS = 24;
    private static final int SPOKES = 6;
    private static final int SHELL = 0xFFE3E9ED;
    private static final int DARK = 0xFF2A3138;
    private static final int STEEL = 0xFF8A96A2;
    private static final int GLASS_TINT = 0x50A8E4F0;
    private static final int CYAN = 0xFF2FE0FF;
    private static final int ORANGE = 0xFFFF8C1A;
    private static final int COPPER = 0xFFE08A45;

    private static final float BASE_TOP = -0.3F;
    private static final float WHEEL_RADIUS = 0.72F;
    private static final float WHEEL_Y = 0.22F;

    public static class State extends BlockEntityRenderState {
        int cylinders;
        Direction facing = Direction.NORTH;
        float angle;
        float speed;
        float time;
        int light;
        /** Sides of the boiler and of the dynamo a pipe or cable comes in from. */
        List<Direction> boilerSockets = List.of();
        List<Direction> dynamoSockets = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SteamEngineBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.cylinders = entity.cylinders();
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.angle = entity.angle(partialTick);
        state.speed = entity.speed();
        Level level = entity.getLevel();
        if (level != null) {
            state.time = level.getGameTime() + partialTick;
            state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos().above());
            BlockPos dynamo = entity.dynamoPos();
            state.boilerSockets = sockets(level, entity.getBlockPos());
            state.dynamoSockets = dynamo != null ? sockets(level, dynamo) : List.of();
        }
    }

    /** The sides of {@code pos} a pipe or cable is connected to. */
    private static List<Direction> sockets(Level level, BlockPos pos) {
        List<Direction> sides = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(side));
            if (side != Direction.DOWN && neighbor.getBlock() instanceof NetworkPipeBlock
                    && neighbor.getValue(NetworkPipeBlock.CONNECTIONS.get(side.getOpposite()))) {
                sides.add(side);
            }
        }
        return sides;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int n = state.cylinders;
        if (n <= 0) {
            return;
        }
        int light = state.light;
        float angle = state.angle;
        float dynamo = n + 1;
        Direction back = state.facing.getOpposite();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // Collars from the boiler and the dynamo out to the pipes and cables, in the world's own axes
        float dynamoX = back.getStepX() * dynamo;
        float dynamoZ = back.getStepZ() * dynamo;
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            for (Direction side : state.boilerSockets) {
                collar(pose, buffer, 0, 0, 0, side, side == Direction.UP ? 0.4F : 0.36F, STEEL, 0.17F, light);
            }
            for (Direction side : state.dynamoSockets) {
                collar(pose, buffer, dynamoX, 0, dynamoZ, side, side == Direction.UP ? 0.4F : 0.36F, STEEL, 0.17F, light);
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            for (Direction side : state.boilerSockets) {
                collar(pose, buffer, 0, 0, 0, side, 0.46F, CYAN, 0.19F, full);
            }
            for (Direction side : state.dynamoSockets) {
                collar(pose, buffer, dynamoX, 0, dynamoZ, side, 0.46F, CYAN, 0.19F, full);
            }
        });
        // +x now points from the boiler back along the row
        poseStack.mulPose(Axis.YP.rotation(-(float) Math.atan2(back.getStepZ(), back.getStepX())));

        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            // Plinth under the whole row
            box(pose, buffer, -0.48F, -0.5F, -0.46F, dynamo + 0.48F, BASE_TOP, 0.46F, DARK, light);
            // Boiler: a white tank with a domed cap and the steam line out of its top
            vcyl(pose, buffer, 0, 0, 0.42F, BASE_TOP, 0.32F, SHELL, light);
            vcyl(pose, buffer, 0, 0, 0.3F, 0.32F, 0.42F, SHELL, light);
            hdisc(pose, buffer, 0, 0, 0.32F, 0.3F, 0.42F, SHELL, light);
            hdisc(pose, buffer, 0, 0, 0.42F, 0.0F, 0.3F, STEEL, light);
            // The steam line runs beside the pistons, dropping into each cylinder
            hcyl(pose, buffer, 0.0F, n + 0.0F, 0.52F, -0.32F, 0.07F, STEEL, light);
            for (int i = 0; i <= n; i++) {
                vcyl(pose, buffer, i, -0.32F, 0.07F, i == 0 ? 0.3F : 0.15F, 0.52F, STEEL, light);
            }
            for (int i = 1; i <= n; i++) {
                cylinder(pose, buffer, i, angle + i * Mth.HALF_PI, light);
            }
            // Dynamo: a drum lying along the row, the flywheel on its back
            hcyl(pose, buffer, dynamo - 0.45F, dynamo + 0.4F, 0.05F, 0, 0.4F, SHELL, light);
            xdisc(pose, buffer, dynamo - 0.45F, 0.05F, 0, 0.0F, 0.4F, 0, SHELL, light);
            xdisc(pose, buffer, dynamo + 0.4F, 0.05F, 0, 0.0F, 0.4F, 0, DARK, light);
            hcyl(pose, buffer, dynamo + 0.4F, dynamo + 0.56F, WHEEL_Y, 0, 0.07F, STEEL, light);
            flywheel(pose, buffer, dynamo + 0.56F, angle, light);
        });
        collector.submitCustomGeometry(poseStack, GLASS, (pose, buffer) -> {
            for (int i = 1; i <= n; i++) {
                vcyl(pose, buffer, i, 0, 0.2F, 0.22F, 0.6F, GLASS_TINT, light);
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            float pulse = 0.7F + 0.3F * Mth.sin(state.time * (0.1F + 0.3F * state.speed));
            int coil = scale(ORANGE, 0.35F + 0.65F * Math.max(state.speed, 0.15F) * pulse);
            // Light line along both sides of the plinth
            box(pose, buffer, -0.46F, -0.42F, -0.465F, dynamo + 0.46F, -0.38F, -0.46F, CYAN, full);
            box(pose, buffer, -0.46F, -0.42F, 0.46F, dynamo + 0.46F, -0.38F, 0.465F, CYAN, full);
            // Induction coils round the boiler, brighter the faster it runs
            for (float y : new float[] {-0.18F, -0.02F, 0.14F}) {
                vcyl(pose, buffer, 0, 0, 0.425F, y, y + 0.06F, coil, full);
            }
            hdisc(pose, buffer, 0, 0, 0.421F, 0.18F, 0.24F, CYAN, full);
            for (int i = 1; i <= n; i++) {
                // The piston head, lit up inside its glass sleeve
                float head = pistonHead(angle + i * Mth.HALF_PI);
                vcyl(pose, buffer, i, 0, 0.17F, head, head + 0.05F, CYAN, full);
                vcyl(pose, buffer, i, 0, 0.305F, 0.0F, 0.04F, CYAN, full);
            }
            // Copper windings on the dynamo, and the flywheel's rim
            for (int k = 0; k < 3; k++) {
                float x = dynamo - 0.3F + k * 0.22F;
                hcyl(pose, buffer, x, x + 0.08F, 0.05F, 0, 0.405F, scale(COPPER, 0.5F + 0.5F * state.speed), full);
            }
            xring(pose, buffer, dynamo + 0.6F, 0.9F * WHEEL_RADIUS, WHEEL_RADIUS, angle, CYAN, full);
        });
        poseStack.popPose();
    }

    /**
     * A square sleeve from {@code from} out to the block's face on {@code side}, around the point
     * ({@code cx}, {@code cy}, {@code cz}): {@code half} is half its width.
     */
    private static void collar(PoseStack.Pose pose, VertexConsumer buffer, float cx, float cy, float cz, Direction side, float from,
            int color, float half, int light) {
        float[] lo = {cx - half, cy - half, cz - half};
        float[] hi = {cx + half, cy + half, cz + half};
        int axis = side.getAxis().ordinal();
        float centre = axis == 0 ? cx : axis == 1 ? cy : cz;
        int sign = side.getAxisDirection().getStep();
        // Up and down are measured from the block's centre, the sides from the machine's axis
        float base = axis == 1 ? 0.0F : centre;
        lo[axis] = base + sign * (sign > 0 ? from : 0.5F);
        hi[axis] = base + sign * (sign > 0 ? 0.5F : from);
        box(pose, buffer, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2], color, light);
    }

    /** Height of a piston head at crank angle {@code a}. */
    private static float pistonHead(float a) {
        return 0.34F + 0.12F * Mth.sin(a);
    }

    /** One cylinder block: a white body with a dark band, the glass sleeve's frame and the piston rod. */
    private static void cylinder(PoseStack.Pose pose, VertexConsumer buffer, float x, float a, int light) {
        vcyl(pose, buffer, x, 0, 0.3F, BASE_TOP, 0.22F, SHELL, light);
        vcyl(pose, buffer, x, 0, 0.31F, -0.12F, -0.04F, DARK, light);
        hdisc(pose, buffer, x, 0, 0.22F, 0.2F, 0.3F, SHELL, light);
        hdisc(pose, buffer, x, 0, 0.6F, 0.12F, 0.24F, STEEL, light);
        vcyl(pose, buffer, x, 0, 0.24F, 0.6F, 0.64F, STEEL, light);
        for (int k = 0; k < 4; k++) {
            float pa = Mth.HALF_PI * k + Mth.HALF_PI / 2;
            float px = x + 0.215F * Mth.cos(pa);
            float pz = 0.215F * Mth.sin(pa);
            box(pose, buffer, px - 0.02F, 0.22F, pz - 0.02F, px + 0.02F, 0.6F, pz + 0.02F, STEEL, light);
        }
        float head = pistonHead(a);
        vcyl(pose, buffer, x, 0, 0.04F, head, 0.74F, STEEL, light);
        vcyl(pose, buffer, x, 0, 0.17F, head - 0.06F, head, STEEL, light);
    }

    private static void flywheel(PoseStack.Pose pose, VertexConsumer buffer, float x, float spin, int light) {
        xdisc(pose, buffer, x, WHEEL_Y, 0, 0.0F, 0.12F, spin, STEEL, light);
        xdisc(pose, buffer, x, WHEEL_Y, 0, 0.78F * WHEEL_RADIUS, 0.9F * WHEEL_RADIUS, spin, DARK, light);
        hcyl(pose, buffer, x - 0.03F, x + 0.03F, WHEEL_Y, 0, 0.9F * WHEEL_RADIUS, DARK, light);
        for (int s = 0; s < SPOKES; s++) {
            float a = spin + s * Mth.TWO_PI / SPOKES;
            float r0 = 0.12F;
            float r1 = 0.8F * WHEEL_RADIUS;
            float w = 0.04F;
            float cy = Mth.cos(a), cz = Mth.sin(a), py = -cz * w, pz = cy * w;
            quad(pose, buffer, STEEL, light, 1, 0, 0,
                    x, WHEEL_Y + cy * r0 - py, cz * r0 - pz, x, WHEEL_Y + cy * r1 - py, cz * r1 - pz,
                    x, WHEEL_Y + cy * r1 + py, cz * r1 + pz, x, WHEEL_Y + cy * r0 + py, cz * r0 + pz);
        }
    }

    private static int scale(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        int r = (int) (((color >> 16) & 0xFF) * f);
        int g = (int) (((color >> 8) & 0xFF) * f);
        int b = (int) ((color & 0xFF) * f);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static float angle(int segment) {
        return Mth.TWO_PI * segment / SEGMENTS;
    }

    /** An upright cylinder's side at ({@code cx}, {@code cz}). */
    private static void vcyl(PoseStack.Pose pose, VertexConsumer buffer, float cx, float cz, float r, float y0, float y1, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float c0 = Mth.cos(angle(i)), s0 = Mth.sin(angle(i)), c1 = Mth.cos(angle(i + 1)), s1 = Mth.sin(angle(i + 1));
            float u0 = (float) i / SEGMENTS, u1 = (float) (i + 1) / SEGMENTS;
            vertex(pose, buffer, cx + r * c1, y0, cz + r * s1, u1, 1, color, light, c1, 0, s1);
            vertex(pose, buffer, cx + r * c0, y0, cz + r * s0, u0, 1, color, light, c0, 0, s0);
            vertex(pose, buffer, cx + r * c0, y1, cz + r * s0, u0, 0, color, light, c0, 0, s0);
            vertex(pose, buffer, cx + r * c1, y1, cz + r * s1, u1, 0, color, light, c1, 0, s1);
        }
    }

    /** A flat horizontal ring (a disc when {@code inner} is 0) at height {@code y}. */
    private static void hdisc(PoseStack.Pose pose, VertexConsumer buffer, float cx, float cz, float y, float inner, float outer, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float c0 = Mth.cos(angle(i)), s0 = Mth.sin(angle(i)), c1 = Mth.cos(angle(i + 1)), s1 = Mth.sin(angle(i + 1));
            quad(pose, buffer, color, light, 0, 1, 0,
                    cx + inner * c0, y, cz + inner * s0, cx + inner * c1, y, cz + inner * s1,
                    cx + outer * c1, y, cz + outer * s1, cx + outer * c0, y, cz + outer * s0);
        }
    }

    /** A cylinder lying along x, from {@code x0} to {@code x1}, its axis at ({@code cy}, {@code cz}). */
    private static void hcyl(PoseStack.Pose pose, VertexConsumer buffer, float x0, float x1, float cy, float cz, float r, int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float c0 = Mth.cos(angle(i)), s0 = Mth.sin(angle(i)), c1 = Mth.cos(angle(i + 1)), s1 = Mth.sin(angle(i + 1));
            float u0 = (float) i / SEGMENTS, u1 = (float) (i + 1) / SEGMENTS;
            vertex(pose, buffer, x0, cy + r * c1, cz + r * s1, u1, 1, color, light, 0, c1, s1);
            vertex(pose, buffer, x0, cy + r * c0, cz + r * s0, u0, 1, color, light, 0, c0, s0);
            vertex(pose, buffer, x1, cy + r * c0, cz + r * s0, u0, 0, color, light, 0, c0, s0);
            vertex(pose, buffer, x1, cy + r * c1, cz + r * s1, u1, 0, color, light, 0, c1, s1);
        }
    }

    /** A flat ring standing across the row at {@code x}, turned by {@code spin}. */
    private static void xdisc(PoseStack.Pose pose, VertexConsumer buffer, float x, float cy, float cz, float inner, float outer, float spin,
            int color, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = spin + angle(i), a1 = spin + angle(i + 1);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(pose, buffer, color, light, 1, 0, 0,
                    x, cy + inner * c0, cz + inner * s0, x, cy + outer * c0, cz + outer * s0,
                    x, cy + outer * c1, cz + outer * s1, x, cy + inner * c1, cz + inner * s1);
        }
    }

    /** The flywheel's glowing rim, broken into dashes so its turning shows. */
    private static void xring(PoseStack.Pose pose, VertexConsumer buffer, float x, float inner, float outer, float spin, int color, int light) {
        for (int i = 0; i < SEGMENTS; i += 2) {
            float a0 = spin + angle(i), a1 = spin + angle(i + 1);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(pose, buffer, color, light, 1, 0, 0,
                    x, WHEEL_Y + inner * c0, inner * s0, x, WHEEL_Y + outer * c0, outer * s0,
                    x, WHEEL_Y + outer * c1, outer * s1, x, WHEEL_Y + inner * c1, inner * s1);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, int light, float nx, float ny, float nz, float... p) {
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            vertex(pose, buffer, p[i * 3], p[i * 3 + 1], p[i * 3 + 2], uv[i][0], uv[i][1], color, light, nx, ny, nz);
        }
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        quad(pose, buffer, color, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, color, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(pose, buffer, color, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, buffer, color, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        quad(pose, buffer, color, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, color, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color, int light,
            float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // The row runs up to 5 blocks behind the boiler: keep drawing while any of it is in view
    @Override
    public AABB getRenderBoundingBox(SteamEngineBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(SteamEngineBlockEntity.MAX_CYLINDERS + 3.0);
    }
}
