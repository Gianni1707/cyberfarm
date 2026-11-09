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
 * Draws a formed Steam Turbine as one enamel and mint octagonal prism lying along the hall, its flat faces towards the four sides: the controller
 * panel on the front cap, a rotor spinning with the turbine's speed behind the open back cap, and a
 * glowing hatch for every Turbine Port. Works in a local frame where x is to the controller's right,
 * y is up and z runs from the front cap to the back.
 */
public class TurbineRenderer implements BlockEntityRenderer<TurbineControllerBlockEntity, TurbineRenderer.State> {
    private static final RenderType SHELL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/machine_shell.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/machine_shell_glow.png"));
    private static final RenderType HATCH = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_port_formed.png"));
    private static final RenderType PANEL = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_controller_formed.png"));
    private static final RenderType PANEL_ON = RenderTypes.entityCutout(CyberFarm.id("textures/block/turbine_controller_on.png"));

    private static final int SEGMENTS = 8;
    /** The corners' radius over the flats', which stay where pipes meet them. */
    private static final float CORNERS = 1.0F / (float) Math.cos(Math.PI / SEGMENTS);
    private static final int BLADES = 8;
    private static final int CAP_COLOR = 0xFF4A6B65;
    private static final int BACK_COLOR = 0xFF233E3B;
    private static final int BLADE_COLOR = 0xFF7C8989;
    private static final int RIM_COLOR = 0xFF7C8989, RING_COLOR = 0xFF5EC2B8;
    /** How far hatches and the panel stand out of the hull, and a hatch's half-width. */
    private static final float PLATE_HEIGHT = 0.08F, HATCH_HALF = 0.4F;

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
            BlockPos offset = port.subtract(controller);
            return new Vector3f(offset.getX() * right.getStepX() + offset.getZ() * right.getStepZ(), offset.getY(),
                    offset.getX() * back.getStepX() + offset.getZ() * back.getStepZ());
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
            housing(pose, buffer, radius * CORNERS, front, size.length(), light, false);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = angle(i);
                float a1 = angle(i + 1);
                disc(pose, buffer, a0, a1, 0.0F, radius * CORNERS, front, -1.0F, CAP_COLOR, light);
                // The back is open: a dark ring around a dark well, the rotor in front of it
                disc(pose, buffer, a0, a1, radius * CORNERS * 0.85F, radius * CORNERS, end, 1.0F, CAP_COLOR, light);
                disc(pose, buffer, a0, a1, 0.0F, radius * CORNERS * 0.85F, end - 0.4F, 1.0F, BACK_COLOR, light);
            }
            frontCap(pose, buffer, size.half(), radius, front, light);
            rotor(pose, buffer, radius * 0.8F, end - 0.2F, state.rotation, light);
        });
        collector.submitCustomGeometry(poseStack, GLOW,
                // A hair outside the shell: drawn on the same surface, shader packs flicker between the two
                (pose, buffer) -> housing(pose, buffer, radius * CORNERS + 0.006F, front, size.length(), LightCoordsUtil.FULL_BRIGHT, true));
        collector.submitCustomGeometry(poseStack, state.lit ? PANEL_ON : PANEL,
                (pose, buffer) -> ShellTiles.raisedPlate(pose, buffer, 0.0F, 0.0F, front - 0.012F, 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F, 0.5F, PLATE_HEIGHT, light));
        collector.submitCustomGeometry(poseStack, HATCH, (pose, buffer) -> {
            for (Vector3f port : state.ports) {
                hatch(pose, buffer, port, size, radius, light);
            }
        });
        poseStack.popPose();
    }

    private static float angle(int segment) {
        return (float) (Math.PI * (2 * segment + 1) / SEGMENTS);
    }

    /** The front cap's plates, one on every block, cut to the octagon, with a mint ring and a steel rim round them. */
    private static void frontCap(PoseStack.Pose pose, VertexConsumer buffer, int half, float flat, float front, int light) {
        float z = front - 0.006F;
        for (int y = -half; y <= half; y++) {
            for (int x = -half; x <= half; x++) {
                capCorner(pose, buffer, x, y, -0.5F, -0.5F, flat, z, light);
                capCorner(pose, buffer, x, y, 0.5F, -0.5F, flat, z, light);
                capCorner(pose, buffer, x, y, 0.5F, 0.5F, flat, z, light);
                capCorner(pose, buffer, x, y, -0.5F, 0.5F, flat, z, light);
            }
        }
        float corner = flat * CORNERS;
        for (int i = 0; i < SEGMENTS; i++) {
            disc(pose, buffer, angle(i), angle(i + 1), corner * 0.94F, corner, front - 0.012F, -1.0F, RIM_COLOR, light);
            disc(pose, buffer, angle(i), angle(i + 1), corner * 0.86F, corner * 0.89F, front - 0.012F, -1.0F, RING_COLOR, light);
        }
    }

    /** A corner of a plate: its texture stays where it is, and a corner past the octagon's slanted side moves back onto it. */
    private static void capCorner(PoseStack.Pose pose, VertexConsumer buffer, int cellX, int cellY, float dx, float dy, float flat, float z, int light) {
        float x = cellX + dx;
        float y = cellY + dy;
        float over = (Math.abs(x) + Math.abs(y) - flat * (1.0F + (float) Math.tan(Math.PI / SEGMENTS))) / 2.0F;
        if (over > 0.0F) {
            x -= Math.signum(x) * over;
            y -= Math.signum(y) * over;
        }
        vertex(pose, buffer, x, y, z, ShellTiles.plateU(cellX, cellY, x - cellX + 0.5F), cellY + 0.5F - y, 0xFFFFFFFF, light, 0.0F, 0.0F, -1.0F);
    }

    /** The hull's skin, a tile on every block; for the glow only the lit ones. */
    private static void housing(PoseStack.Pose pose, VertexConsumer buffer, float radius, float front, int length, int light, boolean glowOnly) {
        int columns = ShellTiles.columns(2.0F * radius * (float) Math.sin(Math.PI / SEGMENTS));
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = angle(i);
            float a1 = angle(i + 1);
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float nx = (c0 + c1) * 0.5F;
            float ny = (s0 + s1) * 0.5F;
            for (int row = 0; row < length; row++) {
                float z0 = front + row;
                float z1 = z0 + 1.0F;
                for (int column = 0; column < columns; column++) {
                    if (glowOnly && !ShellTiles.glows(row, length, column, columns)) {
                        continue;
                    }
                    float t0 = (float) column / columns, t1 = (column + 1.0F) / columns;
                    float xa = (c0 + (c1 - c0) * t0) * radius, ya = (s0 + (s1 - s0) * t0) * radius;
                    float xb = (c0 + (c1 - c0) * t1) * radius, yb = (s0 + (s1 - s0) * t1) * radius;
                    float u0 = ShellTiles.u(row, length, column, columns, 0), u1 = ShellTiles.u(row, length, column, columns, 1);
                    vertex(pose, buffer, xa, ya, z0, u0, 1.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
                    vertex(pose, buffer, xb, yb, z0, u1, 1.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
                    vertex(pose, buffer, xb, yb, z1, u1, 0.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
                    vertex(pose, buffer, xa, ya, z1, u0, 0.0F, 0xFFFFFFFF, light, nx, ny, 0.0F);
                }
            }
        }
    }

    /** A flat ring slice (a pie slice when {@code inner} is 0) across the axis at {@code z}. */
    private static void disc(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float inner, float outer, float z,
            float nz, int color, int light) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        vertex(pose, buffer, c0 * inner, s0 * inner, z, ShellTiles.WHITE_U, ShellTiles.WHITE_V, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c0 * outer, s0 * outer, z, ShellTiles.WHITE_U, ShellTiles.WHITE_V, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c1 * outer, s1 * outer, z, ShellTiles.WHITE_U, ShellTiles.WHITE_V, color, light, 0.0F, 0.0F, nz);
        vertex(pose, buffer, c1 * inner, s1 * inner, z, ShellTiles.WHITE_U, ShellTiles.WHITE_V, color, light, 0.0F, 0.0F, nz);
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
        vertex(pose, buffer, (float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, z, ShellTiles.WHITE_U, ShellTiles.WHITE_V, BLADE_COLOR, light,
                0.0F, 0.0F, 1.0F);
    }

    /**
     * A port's hatch where a pipe plugged into it meets the turbine: on the cap for ports inside the cap, otherwise a plate standing
     * out of the facet the line from the axis through the port crosses, kept on that facet.
     */
    private static void hatch(PoseStack.Pose pose, VertexConsumer buffer, Vector3f port, TurbineSize size, float radius, int light) {
        float x = port.x();
        float y = port.y();
        float z = port.z();
        boolean cap = z == 0 || z == size.length() - 1;
        if (cap && Math.sqrt(x * x + y * y) <= radius - 0.5F) {
            boolean atFront = z == 0;
            float face = atFront ? -0.512F : size.length() - 0.5F;
            ShellTiles.raisedPlate(pose, buffer, x, y, face, 0.0F, 0.0F, atFront ? -1.0F : 1.0F, 0.0F, 1.0F, 0.0F, HATCH_HALF, PLATE_HEIGHT, light);
            return;
        }
        double step = Math.PI * 2 / SEGMENTS;
        double facet = Math.round(Math.atan2(y, x) / step) * step;
        float nx = (float) Math.cos(facet);
        float ny = (float) Math.sin(facet);
        float along = -x * ny + y * nx;
        float room = Math.max(0.0F, radius * (float) Math.tan(Math.PI / SEGMENTS) - HATCH_HALF);
        along = Math.clamp(along, -room, room);
        ShellTiles.raisedPlate(pose, buffer, nx * radius - ny * along, ny * radius + nx * along, z, nx, ny, 0.0F, 0.0F, 0.0F, 1.0F, HATCH_HALF, PLATE_HEIGHT, light);
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
