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
 * Draws a formed Fission Reactor as one faceted cooling tower: an octagonal shell (wide base, narrow
 * waist, flared rim) of enamel panels with glowing bands, its flat faces towards the four sides, over the now invisible shell blocks.
 * Each Reactor Port shows as a glowing hatch and the controller as a panel, flush with the surface
 * where a pipe or cable coming along the block's axis meets it.
 */
public class CoolingTowerRenderer implements BlockEntityRenderer<ReactorControllerBlockEntity, CoolingTowerRenderer.State> {
    private static final RenderType SHELL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/machine_shell.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/machine_shell_glow.png"));
    private static final RenderType HATCH = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_port_formed.png"));
    private static final RenderType PANEL = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_controller_formed.png"));
    private static final RenderType PANEL_ON = RenderTypes.entityCutout(CyberFarm.id("textures/block/reactor_controller_on.png"));

    private static final float WALL = 0.22F;
    private static final int SEGMENTS = 8;
    /** The corners' radius over the flats', which keep the tower's profile, where hatches and pipes meet them. */
    private static final float CORNERS = 1.0F / (float) Math.cos(Math.PI / SEGMENTS);
    private static final int INNER_COLOR = 0xFFA6B0AE, RIM_COLOR = 0xFFBDC6C5, FLOOR_COLOR = 0xFF233E3B, FLOOR_RING_COLOR = 0xFF5EC2B8, FLOOR_HUB_COLOR = 0xFF132423;
    private static final float INNER_GLOW = 0.04F;
    /** How far hatches and the panel stand out of the facets. */
    private static final float PLATE_HEIGHT = 0.08F;
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
        // The glowing bands a hair outside the shell: drawn on the same surface, shader packs flicker between the two
        poseStack.pushPose();
        poseStack.scale(1.004F, 1.0F, 1.004F);
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> drawTower(pose, buffer, size, LightCoordsUtil.FULL_BRIGHT, true));
        poseStack.popPose();
        // The controller sits in the middle of its side, on the second layer
        BlockPos controller = BlockPos.ZERO.relative(state.facing, size.base()).above();
        collector.submitCustomGeometry(poseStack, state.lit ? PANEL_ON : PANEL,
                (pose, buffer) -> onFacet(pose, buffer, size, controller, 0.5F, light));
        collector.submitCustomGeometry(poseStack, HATCH, (pose, buffer) -> {
            for (BlockPos port : state.ports) {
                if (outward(port) != null) {
                    onFacet(pose, buffer, size, port, 0.4F, light);
                }
            }
        });
        poseStack.popPose();
    }

    /** The outer skin, a tile on every block; for the full shell also the inner skin, the rim and the floor inside. */
    private static void drawTower(PoseStack.Pose pose, VertexConsumer buffer, TowerSize size, int light, boolean glowOnly) {
        int rows = size.height();
        float side = (float) (2.0 * Math.tan(Math.PI / SEGMENTS));
        for (int row = 0; row < rows; row++) {
            float h0 = row;
            float h1 = row + 1;
            float r0 = size.radius(h0) * CORNERS;
            float r1 = size.radius(h1) * CORNERS;
            int columns = ShellTiles.columns(size.radius(row + 0.5F) * side);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (Math.PI * (2 * i + 1) / SEGMENTS);
                float a1 = (float) (Math.PI * (2 * i + 3) / SEGMENTS);
                // The inner skin has the same tiles, in shadow; its lit lines stand a hair out from it, as the glow pass is scaled outward
                float inner = glowOnly ? WALL + INNER_GLOW : WALL;
                for (int column = 0; column < columns; column++) {
                    if (!glowOnly || ShellTiles.glows(row, rows, column, columns)) {
                        float t0 = (float) column / columns, t1 = (column + 1.0F) / columns;
                        float u0 = ShellTiles.u(row, rows, column, columns, 0), u1 = ShellTiles.u(row, rows, column, columns, 1);
                        cell(pose, buffer, a0, a1, r0, r1, h0, h1, t0, t1, u0, u1, 1.0F, 0.0F, 0xFFFFFFFF, light, 1.0F);
                        cell(pose, buffer, a0, a1, r0 - inner, r1 - inner, h0, h1, t0, t1, u0, u1, 1.0F, 0.0F, glowOnly ? 0xFFFFFFFF : INNER_COLOR, light, -1.0F);
                    }
                }
            }
        }
        if (glowOnly) {
            return;
        }
        // Rim on top, and the floor inside the tower
        float rim = size.radius(rows) * CORNERS;
        float floor = size.radius(1.0F) * CORNERS - WALL;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * (2 * i + 1) / SEGMENTS);
            float a1 = (float) (Math.PI * (2 * i + 3) / SEGMENTS);
            ring(pose, buffer, a0, a1, rim - WALL, rim, rows, RIM_COLOR, light);
            // The floor: a mint ring round a dark hub
            ring(pose, buffer, a0, a1, floor * 0.62F, floor, 1.0F, FLOOR_COLOR, light);
            ring(pose, buffer, a0, a1, floor * 0.56F, floor * 0.62F, 1.0F, FLOOR_RING_COLOR, light);
            ring(pose, buffer, a0, a1, 0.0F, floor * 0.56F, 1.0F, FLOOR_HUB_COLOR, light);
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
     * A plate standing out of the facet the line from the axis through {@code block} crosses, leaning with the tower's slope and
     * kept on its facet, as the facets may be narrower than the row of blocks behind them.
     */
    private static void onFacet(PoseStack.Pose pose, VertexConsumer buffer, TowerSize tower, BlockPos block, float half, int light) {
        double step = Math.PI * 2 / SEGMENTS;
        double facet = Math.round(Math.atan2(block.getZ(), block.getX()) / step) * step;
        float cos = (float) Math.cos(facet);
        float sin = (float) Math.sin(facet);
        float y = block.getY() + 0.5F;
        float low = tower.radius(block.getY());
        float high = tower.radius(block.getY() + 1.0F);
        float flat = (low + high) / 2.0F;
        float slope = high - low;
        float along = -block.getX() * sin + block.getZ() * cos;
        float room = Math.max(0.0F, flat * (float) Math.tan(Math.PI / SEGMENTS) - half);
        along = Math.clamp(along, -room, room);
        float length = (float) Math.sqrt(1.0F + slope * slope);
        ShellTiles.raisedPlate(pose, buffer, cos * flat - sin * along, y, sin * flat + cos * along,
                cos / length, -slope / length, sin / length, cos * slope / length, 1.0F / length, sin * slope / length, half, PLATE_HEIGHT, light);
    }

    /** A stretch of a facet, from {@code t0} to {@code t1} of the way across it, a straight flat between its two corners. */
    private static void cell(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float r0, float r1, float h0, float h1, float t0, float t1,
            float u0, float u1, float vBottom, float vTop, int color, int light, float outward) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        float nc = (c0 + c1) * 0.5F * outward;
        float ns = (s0 + s1) * 0.5F * outward;
        float xa = c0 + (c1 - c0) * t0, za = s0 + (s1 - s0) * t0, xb = c0 + (c1 - c0) * t1, zb = s0 + (s1 - s0) * t1;
        vertex(pose, buffer, xa * r0, h0, za * r0, u0, vBottom, color, light, nc, ns);
        vertex(pose, buffer, xb * r0, h0, zb * r0, u1, vBottom, color, light, nc, ns);
        vertex(pose, buffer, xb * r1, h1, zb * r1, u1, vTop, color, light, nc, ns);
        vertex(pose, buffer, xa * r1, h1, za * r1, u0, vTop, color, light, nc, ns);
    }

    /** A flat ring slice (or pie slice when {@code inner} is 0) facing up, at {@code height}. */
    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float a0, float a1, float inner, float outer, float height, int color, int light) {
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        float u = ShellTiles.WHITE_U, v = ShellTiles.WHITE_V;
        up(pose, buffer, c0 * inner, height, s0 * inner, u, v, color, light);
        up(pose, buffer, c0 * outer, height, s0 * outer, u, v, color, light);
        up(pose, buffer, c1 * outer, height, s1 * outer, u, v, color, light);
        up(pose, buffer, c1 * inner, height, s1 * inner, u, v, color, light);
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
