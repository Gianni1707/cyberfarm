package com.cyberfarm.client.renderer;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.multiblock.TowerSize;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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

/**
 * Draws a formed Fission Reactor as one smooth cooling tower: a hyperboloid shell (wide base, narrow
 * waist, flared rim) of white panels with glowing cyan bands, over the now invisible shell blocks.
 * Each Reactor Port shows as a glowing hatch and the controller as a panel, flush with the surface
 * where a pipe or cable coming along the block's axis meets it.
 */
public class CoolingTowerRenderer implements BlockEntityRenderer<ReactorControllerBlockEntity, CoolingTowerRenderer.State> {
    private static final RenderType SHELL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/cooling_tower.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/cooling_tower_glow.png"));
    private static final RenderType HATCH = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_port_formed.png"));
    private static final RenderType PANEL = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_controller_formed.png"));
    private static final RenderType PANEL_ON = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_controller_on.png"));

    private static final float WALL = 0.22F;
    private static final int SEGMENTS = 40;
    /** Rows of the surface per block of height. */
    private static final int ROWS_PER_BLOCK = 4;
    /** Texture repeats every this many segments around the tower. */
    private static final int SEGMENTS_PER_TILE = 8;
    private static final int INNER_COLOR = 0xFF8C969E;
    /** Hatches and the panel stand this far out of the surface, so they never flicker into it. */
    private static final float INSET_OFFSET = 0.03F;

    public static class State extends BlockEntityRenderState {
        boolean formed;
        boolean lit;
        TowerSize size = TowerSize.SMALL;
        Direction facing = Direction.NORTH;
        int towerLight;
        /** Ports relative to the floor centre of the tower. */
        List<BlockPos> ports = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ReactorControllerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        BlockState block = entity.getBlockState();
        state.formed = block.getValue(ReactorControllerBlock.FORMED);
        state.facing = block.getValue(MachineBlock.FACING);
        state.lit = block.getValue(ReactorControllerBlock.LIT);
        state.size = TowerSize.byOrdinal(block.getValue(ReactorControllerBlock.SIZE));
        BlockPos floor = state.size.floorCentre(entity.getBlockPos(), state.facing);
        state.ports = state.formed ? entity.portsForRender().stream().map(port -> port.subtract(floor)).toList() : List.of();
        // The controller sits inside solid shell blocks: take the light above the rim instead
        if (entity.getLevel() != null) {
            state.towerLight = LevelRenderer.getLightCoords(entity.getLevel(), floor.above(state.size.height() + 1));
        }
    }


    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        Direction back = state.facing.getOpposite();
        poseStack.pushPose();
        TowerSize size = state.size;
        poseStack.translate(back.getStepX() * size.base() + 0.5F, -1.0F, back.getStepZ() * size.base() + 0.5F);
        int light = state.towerLight;
        collector.submitCustomGeometry(poseStack, SHELL, (pose, buffer) -> drawTower(pose, buffer, size, light, false));
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> drawTower(pose, buffer, size, LightCoordsUtil.FULL_BRIGHT, true));
        // The controller sits in the middle of its side, on the second layer
        BlockPos controller = BlockPos.ZERO.relative(state.facing, size.base()).above();
        collector.submitCustomGeometry(poseStack, state.lit ? PANEL_ON : PANEL,
                (pose, buffer) -> inset(pose, buffer, size, controller, 1.0F, light));
        collector.submitCustomGeometry(poseStack, HATCH, (pose, buffer) -> {
            for (BlockPos port : state.ports) {
                if (outward(port) != null) {
                    inset(pose, buffer, size, port, 0.8F, LightCoordsUtil.FULL_BRIGHT);
                }
            }
        });
        poseStack.popPose();
    }

    /** The outer skin; for the full shell also the inner skin, the rim and the floor inside. */
    private static void drawTower(PoseStack.Pose pose, VertexConsumer buffer, TowerSize size, int light, boolean glowOnly) {
        float height = size.height();
        int rows = size.height() * ROWS_PER_BLOCK;
        for (int row = 0; row < rows; row++) {
            float h0 = height * row / rows;
            float h1 = height * (row + 1) / rows;
            float r0 = size.radius(h0);
            float r1 = size.radius(h1);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (Math.PI * 2 * i / SEGMENTS);
                float a1 = (float) (Math.PI * 2 * (i + 1) / SEGMENTS);
                float u0 = (float) (i % SEGMENTS_PER_TILE) / SEGMENTS_PER_TILE;
                float u1 = u0 + 1.0F / SEGMENTS_PER_TILE;
                // The skin stretches with the tower: bigger towers have bigger panels
                float v0 = 1.0F - h0 / height;
                float v1 = 1.0F - h1 / height;
                quad(pose, buffer, a0, a1, r0, r1, h0, h1, u0, u1, v0, v1, 0xFFFFFFFF, light, 1.0F);
                if (!glowOnly) {
                    quad(pose, buffer, a0, a1, r0 - WALL, r1 - WALL, h0, h1, u0, u1, v0, v1, INNER_COLOR, light, -1.0F);
                }
            }
        }
        if (glowOnly) {
            return;
        }
        // Rim on top, and the floor inside the tower
        float rim = size.radius(height);
        float floor = size.radius(1.0F) - WALL;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2 * i / SEGMENTS);
            float a1 = (float) (Math.PI * 2 * (i + 1) / SEGMENTS);
            ring(pose, buffer, a0, a1, rim - WALL, rim, height, 0.0F, 0.02F, light);
            ring(pose, buffer, a0, a1, 0.0F, floor, 1.0F, 0.0F, 0.0F, light);
        }
    }

    /**
     * The side a pipe or cable reaches a port from: away from the tower's axis, along the main
     * horizontal axis of the port's offset. Ports in the middle of the floor are only reached from below.
     */
    private static Direction outward(BlockPos port) {
        int dx = port.getX();
        int dz = port.getZ();
        if (dx == 0 && dz == 0 || port.getY() == 0 && Math.max(Math.abs(dx), Math.abs(dz)) < 2) {
            return null;
        }
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /**
     * A square plate wrapped onto the tower's curved surface, centred where the line from the axis
     * through the middle of {@code block} crosses it. Each edge follows the surface radius at its own
     * height, so the plate lies flush on the curve and the slope instead of sticking out of it.
     */
    private static void inset(PoseStack.Pose pose, VertexConsumer buffer, TowerSize tower, BlockPos block, float size, int light) {
        float half = size / 2;
        float bottom = block.getY() + 0.5F - half;
        float centre = (float) Math.atan2(block.getZ(), block.getX());
        // Same width in blocks wherever the plate sits: the angle it spans shrinks with the radius
        float span = half / tower.radius(block.getY() + 0.5F);
        int columns = 4;
        for (int i = 0; i < columns; i++) {
            float a0 = centre - span + 2 * span * i / columns;
            float a1 = centre - span + 2 * span * (i + 1) / columns;
            float u0 = 1.0F - (float) i / columns;
            float u1 = 1.0F - (float) (i + 1) / columns;
            plate(pose, buffer, a0, bottom, tower, u0, 1.0F, light);
            plate(pose, buffer, a1, bottom, tower, u1, 1.0F, light);
            plate(pose, buffer, a1, bottom + size, tower, u1, 0.0F, light);
            plate(pose, buffer, a0, bottom + size, tower, u0, 0.0F, light);
        }
    }

    private static void plate(PoseStack.Pose pose, VertexConsumer buffer, float angle, float y, TowerSize tower, float u, float v, int light) {
        float r = tower.radius(y) + INSET_OFFSET;
        float c = (float) Math.cos(angle);
        float sn = (float) Math.sin(angle);
        buffer.addVertex(pose, c * r, y, sn * r).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, c, 0.0F, sn);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float r0, float r1, float h0, float h1,
            float u0, float u1, float v0, float v1, int color, int light, float outward) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        float nc = (c0 + c1) * 0.5F * outward;
        float ns = (s0 + s1) * 0.5F * outward;
        vertex(pose, buffer, c0 * r0, h0, s0 * r0, u0, v0, color, light, nc, ns);
        vertex(pose, buffer, c1 * r0, h0, s1 * r0, u1, v0, color, light, nc, ns);
        vertex(pose, buffer, c1 * r1, h1, s1 * r1, u1, v1, color, light, nc, ns);
        vertex(pose, buffer, c0 * r1, h1, s0 * r1, u0, v1, color, light, nc, ns);
    }

    /** A flat ring slice (or pie slice when {@code inner} is 0) facing up, at {@code height}. */
    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float inner, float outer, float height,
            float v0, float v1, int light) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        int color = 0xFFD9E0E5;
        up(pose, buffer, c0 * inner, height, s0 * inner, 0.0F, v0, color, light);
        up(pose, buffer, c0 * outer, height, s0 * outer, 0.0F, v1, color, light);
        up(pose, buffer, c1 * outer, height, s1 * outer, 0.1F, v1, color, light);
        up(pose, buffer, c1 * inner, height, s1 * inner, 0.1F, v0, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
            int color, int light, float nx, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, 0.0F, nz);
    }

    private static void up(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color, int light) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // The tower is much bigger than the controller block: keep drawing it while any part is in view
    @Override
    public AABB getRenderBoundingBox(ReactorControllerBlockEntity entity) {
        BlockState block = entity.getBlockState();
        TowerSize size = TowerSize.byOrdinal(block.getValue(ReactorControllerBlock.SIZE));
        BlockPos floor = size.floorCentre(entity.getBlockPos(), block.getValue(MachineBlock.FACING));
        return new AABB(floor).inflate(size.base() + 2, 0.0, size.base() + 2).expandTowards(0.0, size.height() + 1, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
