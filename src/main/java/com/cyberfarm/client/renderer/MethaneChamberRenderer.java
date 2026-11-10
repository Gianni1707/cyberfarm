package com.cyberfarm.client.renderer;

import static com.cyberfarm.client.renderer.CrystallizerRenderer.cap;
import static com.cyberfarm.client.renderer.CrystallizerRenderer.cornerX;
import static com.cyberfarm.client.renderer.CrystallizerRenderer.cornerZ;
import static com.cyberfarm.client.renderer.CrystallizerRenderer.prism;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.IntakeGateBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a formed Methane Extractor chamber as one faceted machine, its blocks hidden, a sibling of the Crystallizer: a stepped
 * octagonal plinth, four pylons holding up an octagonal crown whose hub is the extractor's head, and between them a stepped glass
 * bell with steel hoops, the mob in plain view, the acid pool in its lower step. While a mob dissolves a spray ring strokes up and
 * down round its head on four rods. Ports show as couplings like the Crystallizer's, gates as roll-up doors between two pylons.
 * <p>
 * About 700 quads a frame, twice the Crystallizer without its beams, within the default 64 blocks.
 * Works in a frame centred on the chamber's axis, y up from the chamber's bottom face, in blocks; flat faces towards the four sides.
 */
public class MethaneChamberRenderer implements BlockEntityRenderer<MethaneExtractorBlockEntity, MethaneChamberRenderer.State> {
    private static final RenderType METAL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/methane_chamber.png"));
    private static final RenderType GLASS = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/methane_chamber_glass.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));

    private static final int SIDES = 8;
    private static final float PIXEL = 1 / 16.0F;
    private static final float COS = 0.9238795F;
    private static final float PLINTH = 1.5F, STEP = 0.7F, UPPER = 1.25F, DECK = 1.0F, BASIN = 0.95F, DRAIN = 0.2F;
    private static final float CONSOLE = PLINTH * COS;
    private static final float PYLON_OUT = 1.2F;
    private static final float CROWN_LOW = 3.55F, CROWN_HIGH = 3.75F, CROWN_IN = 0.95F, CROWN_OUT = 1.4F;
    /** The extractor's head: a drum in the crown, a dome over it and a spray nozzle under it, clear of a zombie's head at 2.98. */
    private static final float HUB = 0.38F, HUB_LOW = 3.2F, DOME = 0.28F, DOME_TOP = 3.9F, NOZZLE = 0.18F, NOZZLE_LOW = 3.1F;
    private static final float SPOKE_LOW = 3.58F, SPOKE_HIGH = 3.68F, SPOKE_HALF = 1.3F * PIXEL;
    /** The glass bell's three steps: from, to, radius. */
    private static final float[][] BELL = {{DECK, 1.9F, 1.2F}, {1.9F, 2.8F, 1.1F}, {2.8F, CROWN_LOW, 1.0F}};
    private static final float HOOP = 0.025F, HOOP_HALF = 0.06F;
    /** The pool stays in the bell's lowest step, under a mob's chest. */
    private static final float POOL = 1.19F, MAX_DEPTH = 0.85F;

    /** The spray ring in pixels: its openings from flat to axis, clearing a mob's head wherever it stands; its height and nozzles. */
    private static final float RING_IN = 11.0F / (16.0F * COS), RING_OUT = 13.6F / (16.0F * COS), RING_THICK = 2.5F * PIXEL, NOZZLE_H = PIXEL;
    /** The underside of the nozzles at the top and bottom of the stroke, clear of a zombie's arms below. */
    private static final float RING_UP = 51.3F * PIXEL, RING_DOWN = 41.5F * PIXEL;
    /** The rods sliding in the sleeves under the spokes, outside the opening. */
    private static final float ROD = 12.3F * PIXEL, ROD_HALF = 1.3F * PIXEL, SLEEVE_HALF = 1.7F * PIXEL, SLEEVE_LOW = 55.0F * PIXEL;
    /** Ticks a stroke takes, and to get going. */
    private static final float CYCLE = 31.0F, RAMP = 20.0F;

    /** A gate's door, seen with +x out through it, on the bell's lowest step. */
    private static final float DOOR_X = 1.2F * COS, DOOR_HALF = 6.5F * PIXEL, DOOR_TOP = 30.5F * PIXEL;

    private static final int SHELL = 0xFF395853, DARK = 0xFF233E3B, STEEL = 0xFF7C8989, STEEL_LIGHT = 0xFFB1BEBC, BAND = 0xFF5EC2B8, MINT_TOP = 0xFFA5EAD4;
    private static final int LAMP = 0xFFD1FEE7, SCREEN = 0xFF071314;
    private static final int GLASS_TINT = 0x50A8E4F0;
    private static final int ACID = 0xB09BE04A;
    private static final int ACID_GLOW = 0xFF9BE04A;

    /** A gate, by its angle round the axis. */
    record Gate(float angle, boolean open) {}

    /** A port's coupling, and whether it is a Methane Outlet. */
    record Port(CrystallizerRenderer.Port port, boolean outlet) {}

    public static class State extends BlockEntityRenderState {
        boolean formed;
        float depth;
        float acid;
        float time;
        boolean filling;
        boolean working;
        int light;
        float ring;
        float consoleAngle;
        List<Gate> gates = List.of();
        List<Port> ports = List.of();
        /** The bell's facets a door takes the middle of. */
        final boolean[] doors = new boolean[SIDES];
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
        BlockPos floor = top.below(3);
        state.time = level.getGameTime() + partialTick;
        state.light = LightCoordsUtil.getLightCoords(level, top.above());
        state.filling = entity.isFilling();
        state.acid = entity.shownAcid(state.time);
        state.depth = MAX_DEPTH * state.acid;
        state.working = entity.status.working();
        // It slows down and stays wherever it stops, starting from the top, away from the mob
        float stroke = ModelParts.advance(entity, state.time, state.working ? 1.0F : 0.0F, RAMP) * Mth.TWO_PI / CYCLE;
        state.ring = Mth.lerp((1.0F - Mth.cos(stroke)) / 2.0F, RING_UP, RING_DOWN);
        Direction facing = block.getValue(MachineBlock.FACING);
        state.consoleAngle = (float) Math.atan2(facing.getStepZ(), facing.getStepX());
        List<Gate> gates = new ArrayList<>();
        List<Port> ports = new ArrayList<>();
        java.util.Arrays.fill(state.doors, false);
        for (BlockPos pos : MethaneExtractorBlockEntity.chamberBlocks(top)) {
            int dx = pos.getX() - floor.getX();
            int dz = pos.getZ() - floor.getZ();
            if (dx == 0 && dz == 0) {
                continue;
            }
            BlockState part = level.getBlockState(pos);
            if (part.is(ModBlocks.INTAKE_GATE.get())) {
                float angle = (float) Math.atan2(dz, dx);
                gates.add(new Gate(angle, part.getValue(IntakeGateBlock.OPEN)));
                state.doors[(Math.round(angle / (Mth.TWO_PI / SIDES)) + 2 * SIDES - 1) % SIDES] = true;
            } else if (part.is(ModBlocks.ACID_INLET.get()) || part.is(ModBlocks.METHANE_OUTLET.get())) {
                Identifier[] links = CrystallizerRenderer.linksOf(level, pos, floor);
                ports.add(new Port(new CrystallizerRenderer.Port(dx, pos.getY() - floor.getY(), dz, links != null ? links : new Identifier[6]),
                        part.is(ModBlocks.METHANE_OUTLET.get())));
            }
        }
        state.gates = gates;
        state.ports = ports;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        int light = state.light;
        poseStack.pushPose();
        poseStack.translate(0.5F, -3.0F, 0.5F);

        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            // The stepped plinth with the floor on its top
            prism(pose, buffer, PLINTH, 0.0F, STEP, false, SHELL, light);
            cap(pose, buffer, UPPER, PLINTH, STEP, true, SHELL, light);
            prism(pose, buffer, UPPER, STEP, DECK, false, STEEL, light);
            cap(pose, buffer, BASIN, UPPER, DECK, true, SHELL, light);
            cap(pose, buffer, 0.0F, BASIN, DECK, true, DARK, light);
            cap(pose, buffer, 0.0F, DRAIN, DECK + 0.004F, true, SCREEN, light);
            // Pylons: three tapering stages from the lower step to the crown
            for (int i = 0; i < 4; i++) {
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                float x = PYLON_OUT * Mth.cos(a);
                float z = PYLON_OUT * Mth.sin(a);
                box(pose, buffer, x - 0.17F, STEP, z - 0.17F, x + 0.17F, 2.0F, z + 0.17F, SHELL, light);
                box(pose, buffer, x - 0.13F, 2.0F, z - 0.13F, x + 0.13F, 3.0F, z + 0.13F, SHELL, light);
                box(pose, buffer, x - 0.09F, 3.0F, z - 0.09F, x + 0.09F, CROWN_LOW, z + 0.09F, STEEL, light);
            }
            // The crown, the head in its hub and the spokes between them
            prism(pose, buffer, CROWN_OUT, CROWN_LOW, CROWN_HIGH, false, SHELL, light);
            prism(pose, buffer, CROWN_IN, CROWN_LOW, CROWN_HIGH, true, SHELL, light);
            cap(pose, buffer, CROWN_IN, CROWN_OUT, CROWN_HIGH, true, SHELL, light);
            cap(pose, buffer, CROWN_IN, CROWN_OUT, CROWN_LOW, false, SHELL, light);
            prism(pose, buffer, HUB, HUB_LOW, CROWN_HIGH, false, DARK, light);
            cap(pose, buffer, 0.0F, HUB, HUB_LOW, false, DARK, light);
            cap(pose, buffer, DOME, HUB, CROWN_HIGH, true, DARK, light);
            prism(pose, buffer, DOME, CROWN_HIGH, DOME_TOP, false, STEEL, light);
            cap(pose, buffer, 0.0F, DOME, DOME_TOP, true, DARK, light);
            prism(pose, buffer, NOZZLE, NOZZLE_LOW, HUB_LOW, false, STEEL, light);
            float near = HUB - 0.03F, far = CROWN_IN + 0.02F;
            box(pose, buffer, near, SPOKE_LOW, -SPOKE_HALF, far, SPOKE_HIGH, SPOKE_HALF, STEEL, light);
            box(pose, buffer, -far, SPOKE_LOW, -SPOKE_HALF, -near, SPOKE_HIGH, SPOKE_HALF, STEEL, light);
            box(pose, buffer, -SPOKE_HALF, SPOKE_LOW, near, SPOKE_HALF, SPOKE_HIGH, far, STEEL, light);
            box(pose, buffer, -SPOKE_HALF, SPOKE_LOW, -far, SPOKE_HALF, SPOKE_HIGH, -near, STEEL, light);
            // The bell's hoops
            for (float[] step : BELL) {
                prism(pose, buffer, step[2] + HOOP, step[1] - HOOP_HALF, step[1] + HOOP_HALF, false, STEEL, light);
                cap(pose, buffer, step[2], step[2] + HOOP, step[1] + HOOP_HALF, true, STEEL, light);
            }
            sprayRing(pose, buffer, state.ring, light);
        });

        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            prism(pose, buffer, PLINTH + 0.004F, 0.5F, 0.56F, false, BAND, full);
            prism(pose, buffer, CROWN_OUT + 0.004F, 3.62F, 3.68F, false, BAND, full);
            prism(pose, buffer, HUB + 0.004F, 3.4F, 3.46F, false, BAND, full);
            cap(pose, buffer, 0.0F, NOZZLE, NOZZLE_LOW, false, LAMP, full);
            cap(pose, buffer, 0.0F, 0.14F, DOME_TOP + 0.004F, true, LAMP, full);
            for (int i = 0; i < 4; i++) {
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                float x = (PYLON_OUT - 0.175F) * Mth.cos(a);
                float z = (PYLON_OUT - 0.175F) * Mth.sin(a);
                box(pose, buffer, x - 0.025F, STEP + 0.1F, z - 0.025F, x + 0.025F, 1.9F, z + 0.025F, BAND, full);
            }
            bubbles(pose, buffer, state, full);
            if (state.filling) {
                splashes(pose, buffer, state, full);
            }
        });

        // The console on the plinth's front, its bar the acid in the chamber: +x now points out through it
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(-state.consoleAngle));
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) ->
                box(pose, buffer, CONSOLE - 0.02F, 0.1F, -0.34F, CONSOLE + 0.06F, 0.46F, 0.34F, DARK, light));
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            box(pose, buffer, CONSOLE + 0.061F, 0.26F, -0.28F, CONSOLE + 0.064F, 0.42F, 0.28F, SCREEN, full);
            box(pose, buffer, CONSOLE + 0.065F, 0.3F, -0.04F, CONSOLE + 0.067F, 0.38F, 0.04F, state.working ? LAMP : BAND, full);
            if (state.acid > 0.0F) {
                box(pose, buffer, CONSOLE + 0.061F, 0.14F, -0.28F, CONSOLE + 0.064F, 0.2F, -0.28F + 0.56F * state.acid, LAMP, full);
            }
        });
        poseStack.popPose();

        for (Gate gate : state.gates) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotation(-gate.angle()));
            door(poseStack, collector, gate.open(), light);
            poseStack.popPose();
        }
        for (Port port : state.ports) {
            CrystallizerRenderer.coupling(poseStack, collector, port.port(), light, port.outlet() ? STEEL_LIGHT : BAND);
            if (port.port().dy() == 0) {
                if (port.outlet()) {
                    vent(poseStack, collector, port.port(), light);
                } else {
                    CrystallizerRenderer.plinthSocket(poseStack, collector, port.port(), light);
                }
            }
            CrystallizerRenderer.links(poseStack, collector, port.port(), light);
        }

        float surface = DECK + 0.01F + state.depth;
        // Acid and glass in one render type, so its sorting draws them back to front: translucent quads write depth, and glass
        // drawn first would hide the pool. The pool gives up the fluid's own texture for that. The glass shows from outside and within.
        collector.submitCustomGeometry(poseStack, GLASS, (pose, buffer) -> {
            if (state.depth > 0.01F) {
                pool(pose, buffer, surface);
            }
            if (state.filling) {
                stream(pose, buffer, surface, state.time);
            }
            for (int s = 0; s < BELL.length; s++) {
                float[] step = BELL[s];
                boolean[] doors = s == 0 ? state.doors : null;
                bell(pose, buffer, step[2], step[0], step[1], false, doors, light);
                bell(pose, buffer, step[2], step[0], step[1], true, doors, light);
                if (s < BELL.length - 1) {
                    cap(pose, buffer, BELL[s + 1][2], step[2], step[1], true, GLASS_TINT, light);
                    cap(pose, buffer, BELL[s + 1][2], step[2], step[1], false, GLASS_TINT, light);
                }
            }
        });
        poseStack.popPose();
    }

    /** The ring on its nozzles, the rods up from it and the sleeves they slide in. */
    private static void sprayRing(PoseStack.Pose pose, VertexConsumer buffer, float under, int light) {
        float y0 = under + NOZZLE_H;
        float y1 = y0 + RING_THICK;
        prism(pose, buffer, RING_OUT, y0, y1, false, BAND, light);
        prism(pose, buffer, RING_IN, y0, y1, true, BAND, light);
        cap(pose, buffer, RING_IN, RING_OUT, y1, true, MINT_TOP, light);
        cap(pose, buffer, RING_IN, RING_OUT, y0, false, BAND, light);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            float x = side.getStepX() * ROD;
            float z = side.getStepZ() * ROD;
            box(pose, buffer, x - ROD_HALF, under, z - ROD_HALF, x + ROD_HALF, y0, z + ROD_HALF, STEEL_LIGHT, light);
            box(pose, buffer, x - ROD_HALF, y1, z - ROD_HALF, x + ROD_HALF, SPOKE_LOW, z + ROD_HALF, STEEL_LIGHT, light);
            box(pose, buffer, x - SLEEVE_HALF, SLEEVE_LOW, z - SLEEVE_HALF, x + SLEEVE_HALF, SPOKE_LOW, z + SLEEVE_HALF, SHELL, light);
        }
    }

    /** A step of the glass, facing out or in; a facet with a door keeps only its strips beside the door. */
    private static void bell(PoseStack.Pose pose, VertexConsumer buffer, float r, float y0, float y1, boolean inward, boolean @Nullable [] doors, int light) {
        for (int i = 0; i < SIDES; i++) {
            float x0 = cornerX(r, i), z0 = cornerZ(r, i), x1 = cornerX(r, i + 1), z1 = cornerZ(r, i + 1);
            float mid = Mth.TWO_PI * (i + 1) / SIDES;
            float nx = Mth.cos(mid) * (inward ? -1 : 1), nz = Mth.sin(mid) * (inward ? -1 : 1);
            if (doors == null || !doors[i]) {
                pane(pose, buffer, x0, z0, x1, z1, y0, y1, 0.0F, 1.0F, inward, nx, nz, light);
            } else {
                float width = Mth.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
                float t = 0.5F - DOOR_HALF / width;
                float ax = Mth.lerp(t, x0, x1), az = Mth.lerp(t, z0, z1), bx = Mth.lerp(1.0F - t, x0, x1), bz = Mth.lerp(1.0F - t, z0, z1);
                pane(pose, buffer, x0, z0, ax, az, y0, y1, 0.0F, t, inward, nx, nz, light);
                pane(pose, buffer, bx, bz, x1, z1, y0, y1, 1.0F - t, 1.0F, inward, nx, nz, light);
            }
        }
    }

    private static void pane(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, float y0, float y1,
            float u0, float u1, boolean inward, float nx, float nz, int light) {
        if (inward) {
            vertex(pose, buffer, x0, y0, z0, u0, 1, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x1, y0, z1, u1, 1, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x1, y1, z1, u1, 0, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x0, y1, z0, u0, 0, GLASS_TINT, light, nx, 0, nz);
        } else {
            vertex(pose, buffer, x1, y0, z1, u1, 1, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x0, y0, z0, u0, 1, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x0, y1, z0, u0, 0, GLASS_TINT, light, nx, 0, nz);
            vertex(pose, buffer, x1, y1, z1, u1, 0, GLASS_TINT, light, nx, 0, nz);
        }
    }

    /** A door between two pylons with +x out through it: jambs, a header with a lit line, and its shutter, rolled into a drum while it lets a mob in. */
    private static void door(PoseStack poseStack, SubmitNodeCollector collector, boolean open, int light) {
        float x = DOOR_X;
        float z = DOOR_HALF;
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            box(pose, buffer, x, DECK, -0.5F, x + 2.2F * PIXEL, DOOR_TOP, -z, STEEL_LIGHT, light);
            box(pose, buffer, x, DECK, z, x + 2.2F * PIXEL, DOOR_TOP, 0.5F, STEEL_LIGHT, light);
            box(pose, buffer, x, DOOR_TOP, -0.5125F, x + 3.2F * PIXEL, 34.0F * PIXEL, 0.5125F, SHELL, light);
            if (open) {
                box(pose, buffer, x + 0.2F * PIXEL, 28.65F * PIXEL, -z, x + 2.6F * PIXEL, DOOR_TOP, z, STEEL, light);
            } else {
                box(pose, buffer, x + 0.3F * PIXEL, DECK, -z, x + 1.2F * PIXEL, DOOR_TOP, z, DARK, light);
                for (float y : new float[] {18.0F, 21.0F, 24.0F, 27.0F}) {
                    box(pose, buffer, x + 1.2F * PIXEL, y * PIXEL, -z + 0.2F * PIXEL, x + 1.5F * PIXEL, (y + 0.8F) * PIXEL, z - 0.2F * PIXEL, STEEL, light);
                }
            }
        });
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            box(pose, buffer, x + 3.2F * PIXEL, 31.0F * PIXEL, -7.2F * PIXEL, x + 3.4F * PIXEL, 31.9F * PIXEL, 7.2F * PIXEL, BAND, full);
            if (open) {
                box(pose, buffer, x - 0.4F * PIXEL, DECK, -z, x + 0.2F * PIXEL, DECK + 0.3F * PIXEL, z, BAND, full);
            } else {
                box(pose, buffer, x + 1.2F * PIXEL, 29.0F * PIXEL, -1.2F * PIXEL, x + 1.5F * PIXEL, 29.8F * PIXEL, 1.2F * PIXEL, BAND, full);
            }
        });
    }

    /** A Methane Outlet in the floor: a steel flange with a slotted vent on the plinth's facet. */
    private static void vent(PoseStack poseStack, SubmitNodeCollector collector, CrystallizerRenderer.Port port, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(-(float) Math.atan2(port.dz(), port.dx())));
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            box(pose, buffer, CONSOLE - 0.02F, 0.24F, -0.32F, CONSOLE + 0.05F, 0.76F, 0.32F, STEEL_LIGHT, light);
            box(pose, buffer, CONSOLE + 0.05F, 0.3F, -0.26F, CONSOLE + 0.07F, 0.7F, 0.26F, SCREEN, light);
            for (float y : new float[] {0.38F, 0.5F, 0.62F}) {
                box(pose, buffer, CONSOLE + 0.07F, y - 0.025F, -0.24F, CONSOLE + 0.085F, y + 0.025F, 0.24F, STEEL_LIGHT, light);
            }
        });
        poseStack.popPose();
    }

    /** Acid pouring from the extractor's head down onto the pool, its streaks running downwards. */
    private static void stream(PoseStack.Pose pose, VertexConsumer buffer, float surface, float time) {
        float r = 0.14F;
        float scroll = (time * 0.08F) % 1.0F;
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            // Thinner where it leaves the head, wider where it hits the pool
            float top = r * 0.6F;
            float u0 = (float) i / segments, u1 = (float) (i + 1) / segments;
            vertex(pose, buffer, r * c1, surface, r * s1, u1, scroll + 0.5F, ACID, LightCoordsUtil.FULL_BRIGHT, c1, 0, s1);
            vertex(pose, buffer, r * c0, surface, r * s0, u0, scroll + 0.5F, ACID, LightCoordsUtil.FULL_BRIGHT, c0, 0, s0);
            vertex(pose, buffer, top * c0, NOZZLE_LOW, top * s0, u0, scroll, ACID, LightCoordsUtil.FULL_BRIGHT, c0, 0, s0);
            vertex(pose, buffer, top * c1, NOZZLE_LOW, top * s1, u1, scroll, ACID, LightCoordsUtil.FULL_BRIGHT, c1, 0, s1);
        }
    }

    /** Drops thrown up where the stream hits the pool. */
    private static void splashes(PoseStack.Pose pose, VertexConsumer buffer, State state, int light) {
        float surface = DECK + 0.01F + state.depth;
        for (int i = 0; i < 10; i++) {
            float phase = (state.time * 0.06F + i * 0.1F) % 1.0F;
            float angle = i * 2.2F + state.time * 0.01F;
            float out = 0.15F + phase * 0.5F;
            float hop = 0.35F * Mth.sin(phase * Mth.PI);
            float x = out * Mth.cos(angle);
            float z = out * Mth.sin(angle);
            float s = 0.03F * (1.0F - phase) + 0.01F;
            box(pose, buffer, x - s, surface + hop - s, z - s, x + s, surface + hop + s, z + s, ACID_GLOW, light);
        }
    }

    /** Small glowing bubbles rising through the acid, each on its own loop. */
    private static void bubbles(PoseStack.Pose pose, VertexConsumer buffer, State state, int light) {
        if (state.depth < 0.1F) {
            return;
        }
        for (int i = 0; i < 14; i++) {
            float phase = (state.time * (0.012F + 0.004F * (i % 3)) + i * 0.37F) % 1.0F;
            float angle = i * 2.4F;
            float r = 0.25F + 0.8F * ((i * 0.61F) % 1.0F);
            float bx = r * Mth.cos(angle);
            float bz = r * Mth.sin(angle);
            float by = DECK + 0.05F + phase * (state.depth - 0.1F);
            float s = 0.025F + 0.015F * (i % 2);
            box(pose, buffer, bx - s, by - s, bz - s, bx + s, by + s, bz + s, 0xFFE4FFB0, light);
        }
    }

    /** The pool: eight sides and the surface. */
    private static void pool(PoseStack.Pose pose, VertexConsumer buffer, float surface) {
        int full = LightCoordsUtil.FULL_BRIGHT;
        for (int i = 0; i < SIDES; i++) {
            float x0 = cornerX(POOL, i), z0 = cornerZ(POOL, i), x1 = cornerX(POOL, i + 1), z1 = cornerZ(POOL, i + 1);
            float mid = Mth.TWO_PI * (i + 1) / SIDES;
            float nx = Mth.cos(mid), nz = Mth.sin(mid);
            vertex(pose, buffer, x1, DECK, z1, 1, 1, ACID, full, nx, 0, nz);
            vertex(pose, buffer, x0, DECK, z0, 0, 1, ACID, full, nx, 0, nz);
            vertex(pose, buffer, x0, surface, z0, 0, 0, ACID, full, nx, 0, nz);
            vertex(pose, buffer, x1, surface, z1, 1, 0, ACID, full, nx, 0, nz);
            vertex(pose, buffer, 0, surface, 0, 0.5F, 0.5F, ACID, full, 0, 1, 0);
            vertex(pose, buffer, 0, surface, 0, 0.5F, 0.5F, ACID, full, 0, 1, 0);
            vertex(pose, buffer, x1, surface, z1, 0.5F + x1 / 3, 0.5F + z1 / 3, ACID, full, 0, 1, 0);
            vertex(pose, buffer, x0, surface, z0, 0.5F + x0 / 3, 0.5F + z0 / 3, ACID, full, 0, 1, 0);
        }
    }

    static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
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

    static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color, int light,
            float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    // The machine fills the three blocks under the extractor and its own; pipes reach to the edge of the 3x3
    @Override
    public AABB getRenderBoundingBox(MethaneExtractorBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, -3.0, 0.0);
    }
}
