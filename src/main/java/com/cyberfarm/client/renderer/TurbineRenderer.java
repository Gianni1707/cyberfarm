package com.cyberfarm.client.renderer;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.block.entity.TurbineControllerBlockEntity;
import com.cyberfarm.multiblock.TurbineSize;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws a formed Steam Turbine as one white and cyan cylinder lying along the hall: the controller
 * panel on the front cap, a rotor spinning with the turbine's speed behind the open back cap, and a
 * glowing hatch for every Turbine Port. Works in a local frame where x is to the controller's right,
 * y is up and z runs from the front cap to the back.
 */
public class TurbineRenderer implements BlockEntityRenderer<TurbineControllerBlockEntity, TurbineRenderer.State> {
    private static final RenderType SHELL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/cooling_tower.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/cooling_tower_glow.png"));
    private static final RenderType HATCH = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_port_formed.png"));
    private static final RenderType PANEL = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_controller_formed.png"));
    private static final RenderType PANEL_ON = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_controller_on.png"));

    private static final int SEGMENTS = 32;
    private static final int SEGMENTS_PER_TILE = 8;
    private static final int BLADES = 8;
    private static final int CAP_COLOR = 0xFFE3E9ED;
    private static final int BACK_COLOR = 0xFF20272E;
    private static final int BLADE_COLOR = 0xFFC4D0D8;
    private static final float OFFSET = 0.03F;

    public static class State extends BlockEntityRenderState {
        boolean formed;
        boolean lit;
        TurbineSize size = TurbineSize.SMALL;
        Direction facing = Direction.NORTH;
        int light;
        float rotation;
        /** Ports as (right, up, along) from the controller. */
        List<Vector3f> ports = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TurbineControllerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        BlockState block = entity.getBlockState();
        state.formed = block.getValue(TurbineControllerBlock.FORMED);
        state.lit = block.getValue(TurbineControllerBlock.LIT);
        state.facing = block.getValue(MachineBlock.FACING);
        state.size = TurbineSize.byOrdinal(block.getValue(TurbineControllerBlock.SIZE));
        state.rotation = entity.rotation(partialTick);
        BlockPos controller = entity.getBlockPos();
        Direction back = state.facing.getOpposite();
        Direction right = state.facing.getClockWise();
        state.ports = state.formed ? entity.portsForRender().stream().map(port -> {
            BlockPos d = port.subtract(controller);
            return new Vector3f(d.getX() * right.getStepX() + d.getZ() * right.getStepZ(), d.getY(),
                    d.getX() * back.getStepX() + d.getZ() * back.getStepZ());
        }).toList() : List.of();
        // The controller is wrapped in shell blocks: take the light in front of it
        if (entity.getLevel() != null) {
            state.light = LevelRenderer.getLightCoords(entity.getLevel(), controller.relative(state.facing));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        TurbineSize size = state.size;
        float radius = size.half() + 0.5F;
        float front = -0.5F;
        float end = size.length() - 0.5F;
        int light = state.light;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.getOpposite().toYRot()));

        collector.submitCustomGeometry(poseStack, SHELL, (pose, buffer) -> {
            housing(pose, buffer, radius, front, end, size.length(), light);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = angle(i);
                float a1 = angle(i + 1);
                disc(pose, buffer, a0, a1, 0.0F, radius, front, -1.0F, CAP_COLOR, light);
                // The back is open: a dark ring around a dark well, the rotor in front of it
                disc(pose, buffer, a0, a1, radius * 0.85F, radius, end, 1.0F, CAP_COLOR, light);
                disc(pose, buffer, a0, a1, 0.0F, radius * 0.85F, end - 0.4F, 1.0F, BACK_COLOR, light);
            }
            rotor(pose, buffer, radius * 0.8F, end - 0.2F, state.rotation, light);
        });
        collector.submitCustomGeometry(poseStack, GLOW,
                (pose, buffer) -> housing(pose, buffer, radius, front, end, size.length(), LightCoordsUtil.FULL_BRIGHT));
        collector.submitCustomGeometry(poseStack, state.lit ? PANEL_ON : PANEL,
                (pose, buffer) -> plate(pose, buffer, 0.0F, 0.0F, front - OFFSET, Direction.NORTH, 1.0F, light));
        collector.submitCustomGeometry(poseStack, HATCH, (pose, buffer) -> {
            for (Vector3f port : state.ports) {
                hatch(pose, buffer, port, size, radius);
            }
        });
        poseStack.popPose();
    }

    private static float angle(int segment) {
        return (float) (Math.PI * 2 * segment / SEGMENTS);
    }

    /** The cylinder's skin, the texture running once along the hall. */
    private static void housing(PoseStack.Pose pose, VertexConsumer buffer, float radius, float front, float end, int length, int light) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = angle(i);
            float a1 = angle(i + 1);
            float u0 = (float) (i % SEGMENTS_PER_TILE) / SEGMENTS_PER_TILE;
            float u1 = u0 + 1.0F / SEGMENTS_PER_TILE;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float nx = (c0 + c1) * 0.5F;
            float ny = (s0 + s1) * 0.5F;
            vertex(pose, buffer, c0 * radius, s0 * radius, front, u0, 0.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
            vertex(pose, buffer, c1 * radius, s1 * radius, front, u1, 0.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
            vertex(pose, buffer, c1 * radius, s1 * radius, end, u1, 1.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
            vertex(pose, buffer, c0 * radius, s0 * radius, end, u0, 1.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
        }
    }

    /** A flat ring slice (a pie slice when {@code inner} is 0) across the axis at {@code z}. */
    private static void disc(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float inner, float outer, float z,
            float nz, int color, int light) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        vertex(pose, buffer, c0 * inner, s0 * inner, z, 0.0F, 0.5F, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c0 * outer, s0 * outer, z, 0.0F, 0.55F, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c1 * outer, s1 * outer, z, 0.05F, 0.55F, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c1 * inner, s1 * inner, z, 0.05F, 0.5F, color, light, 0.0F, 0.0F, nz);
    }

    /** Twisted blades around a hub, turned by {@code rotation} degrees. */
    private static void rotor(PoseStack.Pose pose, VertexConsumer buffer, float radius, float z, float rotation, int light) {
        float hub = radius * 0.2F;
        for (int i = 0; i < BLADES; i++) {
            float a = (float) Math.toRadians(rotation + 360.0F * i / BLADES);
            float root = 0.22F;
            float tip = 0.42F;
            // Narrow at the hub, wide and swept at the tip
            corner(pose, buffer, a - root, hub, z + 0.05F, light);
            corner(pose, buffer, a + root, hub, z - 0.05F, light);
            corner(pose, buffer, a + tip * 0.6F, radius, z - 0.12F, light);
            corner(pose, buffer, a - tip * 0.4F, radius, z + 0.12F, light);
        }
        for (int i = 0; i < SEGMENTS; i++) {
            disc(pose, buffer, angle(i), angle(i + 1), 0.0F, hub * 1.2F, z + 0.06F, 1.0F, CAP_COLOR, light);
        }
    }

    private static void corner(PoseStack.Pose pose, VertexConsumer buffer, float angle, float radius, float z, int light) {
        vertex(pose, buffer, (float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, z, 0.1F, 0.52F, BLADE_COLOR, light,
                0.0F, 0.0F, 1.0F);
    }

    /**
     * A port's hatch where a pipe plugged into it meets the turbine: on the cap for ports inside the
     * cap's disc, otherwise on the cylinder, on the side the port sticks out to.
     */
    private static void hatch(PoseStack.Pose pose, VertexConsumer buffer, Vector3f port, TurbineSize size, float radius) {
        float x = port.x();
        float y = port.y();
        float z = port.z();
        int light = LightCoordsUtil.FULL_BRIGHT;
        boolean cap = z == 0 || z == size.length() - 1;
        if (cap && Math.sqrt(x * x + y * y) <= radius - 0.5F) {
            boolean atFront = z == 0;
            plate(pose, buffer, x, y, atFront ? -0.5F - OFFSET : size.length() - 0.5F + OFFSET, atFront ? Direction.NORTH : Direction.SOUTH, 0.8F, light);
        } else if (Math.abs(x) >= Math.abs(y)) {
            float out = (float) Math.sqrt(Math.max(0.0F, radius * radius - y * y)) + OFFSET;
            plate(pose, buffer, Math.signum(x) * out, y, z, x > 0 ? Direction.EAST : Direction.WEST, 0.8F, light);
        } else {
            float out = (float) Math.sqrt(Math.max(0.0F, radius * radius - x * x)) + OFFSET;
            plate(pose, buffer, x, Math.signum(y) * out, z, y > 0 ? Direction.UP : Direction.DOWN, 0.8F, light);
        }
    }

    /** A square plate of side {@code size} centred on (x, y, z), facing {@code side} of the local frame. */
    private static void plate(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, Direction side, float size, int light) {
        float h = size / 2;
        // Two axes spanning the plate
        Vector3f n = side.step();
        Vector3f a = side.getAxis() == Direction.Axis.Y ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f b = new Vector3f(n).cross(a);
        for (float[] c : new float[][] {{-1, -1, 1, 1}, {1, -1, 0, 1}, {1, 1, 0, 0}, {-1, 1, 1, 0}}) {
            float px = x + (b.x() * c[0] + a.x() * c[1]) * h;
            float py = y + (b.y() * c[0] + a.y() * c[1]) * h;
            float pz = z + (b.z() * c[0] + a.z() * c[1]) * h;
            vertex(pose, buffer, px, py, pz, c[2], c[3], 0xFFFFFFFF, light, n.x(), n.y(), n.z());
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
            int color, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // The hall is much bigger than the controller block: keep drawing it while any part is in view
    @Override
    public AABB getRenderBoundingBox(TurbineControllerBlockEntity entity) {
        TurbineSize size = TurbineSize.byOrdinal(entity.getBlockState().getValue(TurbineControllerBlock.SIZE));
        return new AABB(entity.getBlockPos()).inflate(size.length() + 1);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
