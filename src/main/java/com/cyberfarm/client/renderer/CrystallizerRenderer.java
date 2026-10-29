package com.cyberfarm.client.renderer;

import static com.cyberfarm.client.renderer.MethaneChamberRenderer.box;
import static com.cyberfarm.client.renderer.MethaneChamberRenderer.vertex;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.CrystallizerBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a complete Crystallizer as one faceted machine, its blocks hidden: a stepped octagonal
 * plinth with a basin of acid sunk in its top, four white pylons holding up an octagonal crown with
 * an emitter hub in its middle, and a violet energy field between them. While a batch runs, beams
 * from the basin's emitter and the hub feed the Fusion Crystal growing in the middle, arcs crackle
 * from the pylons to it and motes of light spiral in; a finished crystal hangs there, turning
 * slowly, until it is taken out. The console on the plinth's front shows the progress, and every
 * Crystallizer Port shows as a socket lit violet where it is: on the plinth, clamped to a pylon or
 * set in the crown.
 * Works in a frame centred on the chamber's axis, y up from the ground; the octagons have flat
 * faces towards the four sides.
 */
public class CrystallizerRenderer implements BlockEntityRenderer<CrystallizerBlockEntity, CrystallizerRenderer.State> {
    private static final RenderType METAL = RenderTypes.entityCutout(CyberFarm.id("textures/entity/methane_chamber.png"));
    private static final RenderType FIELD = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/methane_chamber_glass.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));
    private static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType CRYSTAL = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/resonance_crystal.png"));
    private static final RenderType HEART = RenderTypes.eyes(CyberFarm.id("textures/entity/resonance_glow.png"));
    private static final RenderType BEAM = RenderTypes.eyes(CyberFarm.id("textures/entity/stabilizer_beam.png"));

    private static final int SIDES = 8;
    /** Plinth: a wide lower step, a narrower upper one with the basin sunk in it. */
    private static final float PLINTH = 1.5F;
    private static final float STEP = 0.7F;
    private static final float UPPER = 1.25F;
    private static final float DECK = 1.0F;
    private static final float BASIN = 0.95F;
    private static final float BASIN_FLOOR = 0.78F;
    private static final float MAX_DEPTH = 0.2F;
    /** The pylons stand on the lower step at the diagonals, up to the crown. */
    private static final float PYLON_OUT = 1.2F;
    private static final float CROWN_LOW = 3.55F;
    private static final float CROWN_HIGH = 3.75F;
    private static final float CROWN_IN = 0.95F;
    private static final float CROWN_OUT = 1.4F;
    private static final float HUB = 0.3F;
    private static final float HUB_LOW = 3.3F;
    private static final float FIELD_RADIUS = 1.0F;
    private static final float CRYSTAL_Y = 2.2F;
    private static final float SEED_SIZE = 0.05F;
    private static final float FULL_SIZE = 0.4F;
    /** The crystal is taller than wide. */
    private static final float STRETCH = 1.4F;
    private static final float CONSOLE = PLINTH * Mth.cos(Mth.PI / SIDES);

    private static final int SHELL = 0xFFE3E9ED;
    private static final int DARK = 0xFF2A3138;
    private static final int STEEL = 0xFF5A6570;
    private static final int VIOLET = 0xFF8A4FD8;
    private static final int VIOLET_LIGHT = 0xFFD6B8FF;
    private static final int SCREEN = 0xFF2A1450;
    private static final int ACID = 0xC09BE04A;
    private static final int CRYSTAL_SHELL = 0xC0E6C8FF;
    private static final int CRYSTAL_HEART = 0xFFC890FF;

    /**
     * A Crystallizer Port, by its offset from the chamber's centre block, and what is attached to
     * each side of its block (by {@link Direction#get3DDataValue}): the texture of a CyberFarm pipe
     * whose arm reaches it, {@link #PLAIN} for anything else with a block entity, or null.
     */
    record Port(int dx, int dy, int dz, @Nullable Identifier[] links) {}

    /** A link to a hopper, a tank or another mod's pipe: plain white. */
    private static final Identifier PLAIN = CyberFarm.id("textures/entity/methane_chamber.png");
    /** The arm of a CyberFarm pipe: half as wide as it is, and the plain-tube columns of its texture. */
    private static final float LINK_HALF = 2 / 16.0F;
    /** A port's coupling: as big as a pipe's, the slate of its casing. */
    private static final float COUPLING = 3 / 16.0F;
    private static final int COUPLING_COLOR = 0xFF3A4550;
    private static final Map<Identifier, RenderType> LINK_TYPES = new HashMap<>();

    public static class State extends BlockEntityRenderState {
        boolean formed;
        List<Port> ports = List.of();
        float centreX;
        float centreZ;
        /** The console's direction around the axis, in radians from +x. */
        float consoleAngle;
        float growth;
        boolean done;
        float acid;
        float time;
        int light;
        @Nullable TextureAtlasSprite acidSprite;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrystallizerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.formed = entity.getBlockState().getValue(CrystallizerBlock.FORMED) && level != null;
        if (!state.formed) {
            return;
        }
        Direction facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.centreX = 0.5F - facing.getStepX();
        state.centreZ = 0.5F - facing.getStepZ();
        state.consoleAngle = (float) Math.atan2(facing.getStepZ(), facing.getStepX());
        state.time = level.getGameTime() + partialTick;
        state.growth = entity.growth(state.time);
        state.done = entity.hasCrystal();
        state.acid = entity.acidFill();
        BlockPos centre = entity.centre();
        state.light = LightCoordsUtil.getLightCoords(level, centre.above(2));
        List<Port> ports = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 3, 1))) {
            if (level.getBlockState(pos).is(ModBlocks.CRYSTALLIZER_PORT.get())) {
                // A pipe, cable, hopper or tank outside the chamber: anything with a block entity
                Identifier[] links = new Identifier[6];
                for (Direction side : Direction.values()) {
                    BlockPos next = pos.relative(side);
                    boolean inside = Math.abs(next.getX() - centre.getX()) <= 1 && Math.abs(next.getZ() - centre.getZ()) <= 1
                            && next.getY() >= centre.getY() && next.getY() <= centre.getY() + 3;
                    BlockState neighbour = level.getBlockState(next);
                    if (inside || level.getBlockEntity(next) == null) {
                        continue;
                    }
                    if (neighbour.getBlock() instanceof NetworkPipeBlock) {
                        if (neighbour.getValue(NetworkPipeBlock.CONNECTIONS.get(side.getOpposite()))) {
                            links[side.get3DDataValue()] = BuiltInRegistries.BLOCK.getKey(neighbour.getBlock()).withPath(path -> "textures/block/" + path + ".png");
                        }
                    } else {
                        links[side.get3DDataValue()] = PLAIN;
                    }
                }
                ports.add(new Port(pos.getX() - centre.getX(), pos.getY() - centre.getY(), pos.getZ() - centre.getZ(), links));
            }
        }
        state.ports = ports;
        state.acidSprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(ModFluids.ACID.get().defaultFluidState()).stillMaterial().sprite();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        int light = state.light;
        boolean growing = state.growth > 0.0F;
        float time = state.time;
        float pulse = 0.75F + 0.25F * Mth.sin(time * (growing ? 0.4F : 0.1F));
        float size = growing ? Mth.lerp(state.growth, SEED_SIZE, FULL_SIZE) : FULL_SIZE;
        poseStack.pushPose();
        poseStack.translate(state.centreX, 0.0F, state.centreZ);

        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            // The stepped plinth and the basin sunk in its top
            prism(pose, buffer, PLINTH, 0.0F, STEP, false, SHELL, light);
            cap(pose, buffer, UPPER, PLINTH, STEP, true, SHELL, light);
            prism(pose, buffer, UPPER, STEP, DECK, false, STEEL, light);
            cap(pose, buffer, BASIN, UPPER, DECK, true, SHELL, light);
            prism(pose, buffer, BASIN, BASIN_FLOOR, DECK, true, DARK, light);
            cap(pose, buffer, 0.0F, BASIN, BASIN_FLOOR, true, DARK, light);
            // The floor emitter rising out of the acid
            prism(pose, buffer, 0.2F, BASIN_FLOOR, DECK + 0.15F, false, DARK, light);
            cap(pose, buffer, 0.0F, 0.2F, DECK + 0.15F, true, DARK, light);
            // Pylons: three tapering stages from the lower step to the crown
            for (int i = 0; i < 4; i++) {
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                float x = PYLON_OUT * Mth.cos(a);
                float z = PYLON_OUT * Mth.sin(a);
                box(pose, buffer, x - 0.17F, STEP, z - 0.17F, x + 0.17F, 2.0F, z + 0.17F, SHELL, light);
                box(pose, buffer, x - 0.13F, 2.0F, z - 0.13F, x + 0.13F, 3.0F, z + 0.13F, SHELL, light);
                box(pose, buffer, x - 0.09F, 3.0F, z - 0.09F, x + 0.09F, CROWN_LOW, z + 0.09F, STEEL, light);
            }
            // The crown, the hub and the spokes between them
            prism(pose, buffer, CROWN_OUT, CROWN_LOW, CROWN_HIGH, false, SHELL, light);
            prism(pose, buffer, CROWN_IN, CROWN_LOW, CROWN_HIGH, true, SHELL, light);
            cap(pose, buffer, CROWN_IN, CROWN_OUT, CROWN_HIGH, true, SHELL, light);
            cap(pose, buffer, CROWN_IN, CROWN_OUT, CROWN_LOW, false, SHELL, light);
            prism(pose, buffer, HUB, HUB_LOW, CROWN_HIGH, false, DARK, light);
            cap(pose, buffer, 0.0F, HUB, CROWN_HIGH, true, DARK, light);
            cap(pose, buffer, 0.0F, HUB, HUB_LOW, false, DARK, light);
            box(pose, buffer, HUB, 3.58F, -0.06F, CROWN_IN + 0.02F, 3.68F, 0.06F, STEEL, light);
            box(pose, buffer, -CROWN_IN - 0.02F, 3.58F, -0.06F, -HUB, 3.68F, 0.06F, STEEL, light);
            box(pose, buffer, -0.06F, 3.58F, HUB, 0.06F, 3.68F, CROWN_IN + 0.02F, STEEL, light);
            box(pose, buffer, -0.06F, 3.58F, -CROWN_IN - 0.02F, 0.06F, 3.68F, -HUB, STEEL, light);
        });

        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            int lens = growing ? scale(VIOLET_LIGHT, pulse) : VIOLET;
            prism(pose, buffer, PLINTH + 0.004F, 0.5F, 0.56F, false, VIOLET, full);
            prism(pose, buffer, CROWN_OUT + 0.004F, 3.62F, 3.68F, false, VIOLET, full);
            cap(pose, buffer, 0.0F, 0.12F, DECK + 0.152F, true, lens, full);
            cap(pose, buffer, 0.0F, 0.14F, HUB_LOW - 0.002F, false, lens, full);
            for (int i = 0; i < 4; i++) {
                // A light line up each pylon's inner face, and the node the arcs leap from
                float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
                float x = (PYLON_OUT - 0.175F) * Mth.cos(a);
                float z = (PYLON_OUT - 0.175F) * Mth.sin(a);
                box(pose, buffer, x - 0.025F, STEP + 0.1F, z - 0.025F, x + 0.025F, 1.9F, z + 0.025F, VIOLET, full);
                Vec3 node = node(i);
                float n = 0.05F;
                box(pose, buffer, (float) node.x - n, (float) node.y - n, (float) node.z - n, (float) node.x + n, (float) node.y + n,
                        (float) node.z + n, lens, full);
            }
            if (growing) {
                motes(pose, buffer, time, size, full);
            }
        });

        TextureAtlasSprite sprite = state.acidSprite;
        if (sprite != null && state.acid > 0.01F) {
            float surface = BASIN_FLOOR + 0.01F + MAX_DEPTH * state.acid;
            collector.submitCustomGeometry(poseStack, FLUID, (pose, buffer) -> pool(pose, buffer, sprite, surface));
        }

        // The crystal, and while it grows the beams feeding it and the arcs from the pylons
        if (growing || state.done) {
            if (growing) {
                int beam = scale(VIOLET_LIGHT, pulse);
                float reach = size * STRETCH;
                int burst = (int) (time / 2.0F);
                collector.submitCustomGeometry(poseStack, BEAM, (pose, buffer) -> {
                    ResonanceCoreRenderer.beam(pose, buffer, new Vec3(0, DECK + 0.15, 0), new Vec3(0, CRYSTAL_Y - reach, 0), beam);
                    ResonanceCoreRenderer.beam(pose, buffer, new Vec3(0, HUB_LOW, 0), new Vec3(0, CRYSTAL_Y + reach, 0), beam);
                    for (int i = 0; i < 4; i++) {
                        if (noise(burst, i + 100) > 0.0F) {
                            arc(pose, buffer, node(i), new Vec3(0, CRYSTAL_Y, 0), size, burst, i);
                        }
                    }
                });
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, CRYSTAL_Y + Mth.sin(time * 0.05F) * 0.03F, 0.0F);
            float spin = time * (growing ? 3.0F : 0.6F);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.scale(1.0F, STRETCH, 1.0F);
            collector.submitCustomGeometry(poseStack, CRYSTAL, (pose, buffer) -> ResonanceCoreRenderer.polyhedron(pose, buffer, size, CRYSTAL_SHELL));
            poseStack.mulPose(Axis.YP.rotationDegrees(-spin * 2.0F));
            float heart = size * 0.55F * (1.0F + Mth.sin(time * (growing ? 0.5F : 0.15F)) * 0.1F);
            collector.submitCustomGeometry(poseStack, HEART, (pose, buffer) -> ResonanceCoreRenderer.polyhedron(pose, buffer, heart, CRYSTAL_HEART));
            poseStack.popPose();
        }

        // The console on the plinth's front: +x now points out through it
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(-state.consoleAngle));
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) ->
                box(pose, buffer, CONSOLE - 0.02F, 0.1F, -0.34F, CONSOLE + 0.06F, 0.46F, 0.34F, DARK, light));
        float progress = state.done ? 1.0F : state.growth;
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            int full = LightCoordsUtil.FULL_BRIGHT;
            box(pose, buffer, CONSOLE + 0.061F, 0.26F, -0.28F, CONSOLE + 0.064F, 0.42F, 0.28F, SCREEN, full);
            box(pose, buffer, CONSOLE + 0.065F, 0.3F, -0.04F, CONSOLE + 0.067F, 0.38F, 0.04F, growing ? scale(VIOLET_LIGHT, pulse) : VIOLET, full);
            if (progress > 0.0F) {
                box(pose, buffer, CONSOLE + 0.061F, 0.14F, -0.28F, CONSOLE + 0.064F, 0.2F, -0.28F + 0.56F * progress, VIOLET_LIGHT, full);
            }
        });
        poseStack.popPose();

        for (Port port : state.ports) {
            socket(poseStack, collector, port, light);
            links(poseStack, collector, port, light);
        }

        // The energy field, seen from outside and from within, brighter while it works
        int alpha = (int) ((growing ? 0x50 : 0x30) * (0.8F + 0.2F * Mth.sin(time * 0.15F)));
        int field = alpha << 24 | 0xB890FF;
        collector.submitCustomGeometry(poseStack, FIELD, (pose, buffer) -> {
            prism(pose, buffer, FIELD_RADIUS, DECK, HUB_LOW + 0.2F, false, field, light);
            prism(pose, buffer, FIELD_RADIUS, DECK, HUB_LOW + 0.2F, true, field, light);
        });
        poseStack.popPose();
    }

    /**
     * What shows of a port, drawn like a node of the pipes: a coupling at its block's centre, dark
     * with a violet window on every side, set on the machine (in the plinth, against the pylon,
     * under the crown); a port in a roof corner reaches the pylon with a short stub. A port in the
     * floor also shows a violet socket on the plinth's facet, where the coupling is out of sight.
     */
    private static void socket(PoseStack poseStack, SubmitNodeCollector collector, Port port, int light) {
        int full = LightCoordsUtil.FULL_BRIGHT;
        if (port.dy() == 0) {
            poseStack.pushPose();
            // +x now points out towards the port's block
            poseStack.mulPose(Axis.YP.rotation(-(float) Math.atan2(port.dz(), port.dx())));
            float x = CONSOLE;
            float y = 0.32F;
            float s = 0.17F;
            collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> box(pose, buffer, x - 0.02F, y - s, -s, x + 0.04F, y + s, s, DARK, light));
            collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> ringOut(pose, buffer, x + 0.041F, y, full));
            poseStack.popPose();
        }
        float cx = port.dx();
        float cy = port.dy() + 0.5F;
        float cz = port.dz();
        float h = COUPLING;
        boolean roofCorner = port.dy() == 3 && port.dx() != 0 && port.dz() != 0;
        collector.submitCustomGeometry(poseStack, METAL, (pose, buffer) -> {
            box(pose, buffer, cx - h, cy - h, cz - h, cx + h, cy + h, cz + h, COUPLING_COLOR, light);
            if (roofCorner) {
                tube(pose, buffer, new Vec3(cx, cy, cz), new Vec3(port.dx() * 0.85, cy, port.dz() * 0.85), LINK_HALF, STEEL, light, 0, 1, 0, 1);
            }
        });
        // The window on each side, like the coloured square of a pipe's coupling
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> {
            float w = 0.07F;
            float o = h + 0.002F;
            box(pose, buffer, cx - w, cy - w, cz - o, cx + w, cy + w, cz + o, VIOLET, full);
            box(pose, buffer, cx - o, cy - w, cz - w, cx + o, cy + w, cz + w, VIOLET, full);
            box(pose, buffer, cx - w, cy - o, cz - w, cx + w, cy + o, cz + w, VIOLET, full);
        });
    }

    /**
     * For every side of a port's block with something attached: an arm of that pipe from the
     * coupling out to the side, where the pipe's own arm takes over, as a pipe's block draws it.
     */
    private static void links(PoseStack poseStack, SubmitNodeCollector collector, Port port, int light) {
        Vec3 centre = new Vec3(port.dx(), port.dy() + 0.5, port.dz());
        for (Direction side : Direction.values()) {
            Identifier texture = port.links()[side.get3DDataValue()];
            if (texture == null) {
                continue;
            }
            Vec3 normal = Vec3.atLowerCornerOf(side.getUnitVec3i());
            Vec3 from = centre.add(normal.scale(COUPLING));
            Vec3 to = centre.add(normal.scale(0.5));
            boolean plain = texture == PLAIN;
            RenderType type = plain ? METAL : LINK_TYPES.computeIfAbsent(texture, RenderTypes::entityCutout);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> tube(pose, buffer, from, to, LINK_HALF,
                    plain ? SHELL : 0xFFFFFFFF, light, plain ? 0.0F : 1 / 16.0F, plain ? 1.0F : 5 / 16.0F, plain ? 0.0F : 6 / 16.0F, plain ? 1.0F : 10 / 16.0F));
        }
    }

    /** A square tube between two points, {@code half} its half-width, its texture's (u0, v0)-(u1, v1) wrapped round its sides. */
    private static void tube(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, float half, int color, int light,
            float u0, float u1, float v0, float v1) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0E-6) {
            return;
        }
        dir = dir.normalize();
        Vec3 up = Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = dir.cross(up).normalize().scale(half);
        Vec3 v = dir.cross(u).normalize().scale(half);
        Vec3[] ends = {a, b};
        Vec3[][] corners = new Vec3[2][4];
        for (int e = 0; e < 2; e++) {
            corners[e][0] = ends[e].add(u).add(v);
            corners[e][1] = ends[e].subtract(u).add(v);
            corners[e][2] = ends[e].subtract(u).subtract(v);
            corners[e][3] = ends[e].add(u).subtract(v);
        }
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            Vec3 n = corners[0][i].add(corners[0][j]).scale(0.5).subtract(a).normalize();
            Vec3[] quad = {corners[0][i], corners[0][j], corners[1][j], corners[1][i]};
            float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
            for (int k = 0; k < 4; k++) {
                vertex(pose, buffer, (float) quad[k].x, (float) quad[k].y, (float) quad[k].z, uv[k][0], uv[k][1], color, light,
                        (float) n.x, (float) n.y, (float) n.z);
            }
        }
    }

    /** A lit square ring with a dot in it, on a face looking out along +x at {@code x}. */
    private static void ringOut(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, int light) {
        float r = 0.14F;
        float t = 0.045F;
        box(pose, buffer, x, y + r - t, -r, x + 0.003F, y + r, r, VIOLET_LIGHT, light);
        box(pose, buffer, x, y - r, -r, x + 0.003F, y - r + t, r, VIOLET_LIGHT, light);
        box(pose, buffer, x, y - r, -r, x + 0.003F, y + r, -r + t, VIOLET_LIGHT, light);
        box(pose, buffer, x, y - r, r - t, x + 0.003F, y + r, r, VIOLET_LIGHT, light);
        box(pose, buffer, x, y - 0.04F, -0.04F, x + 0.004F, y + 0.04F, 0.04F, VIOLET, light);
    }

    /** Where pylon {@code i}'s arcs leap from: a node on its inner face, under the crown. */
    private static Vec3 node(int i) {
        float a = Mth.HALF_PI * i + Mth.HALF_PI / 2;
        float r = PYLON_OUT - 0.12F;
        return new Vec3(r * Mth.cos(a), 3.35, r * Mth.sin(a));
    }

    /** A jagged arc from a pylon to the crystal's surface, its kinks changing with every burst. */
    private static void arc(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 crystal, float size, int burst, int pylon) {
        Vec3 to = crystal.add(from.subtract(crystal).normalize().scale(size));
        int kinks = 5;
        Vec3 last = from;
        for (int k = 1; k <= kinks; k++) {
            Vec3 next = from.lerp(to, (double) k / kinks);
            if (k < kinks) {
                float amount = 0.3F * Mth.sin(Mth.PI * k / kinks);
                next = next.add(noise(burst, pylon * 16 + k) * amount, noise(burst, pylon * 16 + k + 5) * amount,
                        noise(burst, pylon * 16 + k + 10) * amount);
            }
            ResonanceCoreRenderer.beam(pose, buffer, last, next, VIOLET_LIGHT, 0.02F);
            last = next;
        }
    }

    /** A steady pseudo-random number in -0.5..0.5 for the pair. */
    private static float noise(int a, int b) {
        int h = a * 73856093 ^ b * 19349663;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0F - 0.5F;
    }

    /** Motes of light spiralling in from the field to the growing crystal. */
    private static void motes(PoseStack.Pose pose, VertexConsumer buffer, float time, float size, int light) {
        for (int i = 0; i < 12; i++) {
            float phase = (time * 0.02F + i / 12.0F) % 1.0F;
            float r = size + (1.0F - phase) * (FIELD_RADIUS - 0.1F - size);
            float angle = i * 0.52F + time * 0.04F + phase * 4.0F;
            float x = r * Mth.cos(angle);
            float z = r * Mth.sin(angle);
            float y = CRYSTAL_Y + (i % 3 - 1) * 0.5F * (1.0F - phase);
            float s = 0.02F + 0.015F * (1.0F - phase);
            box(pose, buffer, x - s, y - s, z - s, x + s, y + s, z + s, VIOLET_LIGHT, light);
        }
    }

    /** The corner of an octagon of circumradius {@code r}: corners sit between the flat faces, which face the four sides. */
    private static float cornerX(float r, int i) {
        return r * Mth.cos(Mth.PI / SIDES + Mth.TWO_PI * i / SIDES);
    }

    private static float cornerZ(float r, int i) {
        return r * Mth.sin(Mth.PI / SIDES + Mth.TWO_PI * i / SIDES);
    }

    /** The sides of an octagonal prism, facing out or (for walls seen from within) in. */
    private static void prism(PoseStack.Pose pose, VertexConsumer buffer, float r, float y0, float y1, boolean inward, int color, int light) {
        for (int i = 0; i < SIDES; i++) {
            float x0 = cornerX(r, i), z0 = cornerZ(r, i), x1 = cornerX(r, i + 1), z1 = cornerZ(r, i + 1);
            float mid = Mth.TWO_PI * (i + 1) / SIDES;
            float nx = Mth.cos(mid) * (inward ? -1 : 1);
            float nz = Mth.sin(mid) * (inward ? -1 : 1);
            if (inward) {
                vertex(pose, buffer, x0, y0, z0, 0, 1, color, light, nx, 0, nz);
                vertex(pose, buffer, x1, y0, z1, 1, 1, color, light, nx, 0, nz);
                vertex(pose, buffer, x1, y1, z1, 1, 0, color, light, nx, 0, nz);
                vertex(pose, buffer, x0, y1, z0, 0, 0, color, light, nx, 0, nz);
            } else {
                vertex(pose, buffer, x1, y0, z1, 1, 1, color, light, nx, 0, nz);
                vertex(pose, buffer, x0, y0, z0, 0, 1, color, light, nx, 0, nz);
                vertex(pose, buffer, x0, y1, z0, 0, 0, color, light, nx, 0, nz);
                vertex(pose, buffer, x1, y1, z1, 1, 0, color, light, nx, 0, nz);
            }
        }
    }

    /** A flat octagonal ring (a whole octagon when {@code inner} is 0) at height {@code y}, facing up or down. */
    private static void cap(PoseStack.Pose pose, VertexConsumer buffer, float inner, float outer, float y, boolean up, int color, int light) {
        float ny = up ? 1 : -1;
        for (int i = 0; i < SIDES; i++) {
            float[][] quad = {
                    {cornerX(inner, i), cornerZ(inner, i)}, {cornerX(inner, i + 1), cornerZ(inner, i + 1)},
                    {cornerX(outer, i + 1), cornerZ(outer, i + 1)}, {cornerX(outer, i), cornerZ(outer, i)}};
            for (int k = 0; k < 4; k++) {
                float[] c = quad[up ? k : 3 - k];
                vertex(pose, buffer, c[0], y, c[1], c[0] / 3 + 0.5F, c[1] / 3 + 0.5F, color, light, 0, ny, 0);
            }
        }
    }

    /** The acid in the basin: its surface, the fluid's texture laid flat. */
    private static void pool(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, float surface) {
        int full = LightCoordsUtil.FULL_BRIGHT;
        float r = BASIN - 0.01F;
        for (int i = 0; i < SIDES; i++) {
            float x0 = cornerX(r, i), z0 = cornerZ(r, i), x1 = cornerX(r, i + 1), z1 = cornerZ(r, i + 1);
            vertex(pose, buffer, 0, surface, 0, sprite.getU(0.5F), sprite.getV(0.5F), ACID, full, 0, 1, 0);
            vertex(pose, buffer, 0, surface, 0, sprite.getU(0.5F), sprite.getV(0.5F), ACID, full, 0, 1, 0);
            vertex(pose, buffer, x1, surface, z1, sprite.getU(0.5F + x1 / 2), sprite.getV(0.5F + z1 / 2), ACID, full, 0, 1, 0);
            vertex(pose, buffer, x0, surface, z0, sprite.getU(0.5F + x0 / 2), sprite.getV(0.5F + z0 / 2), ACID, full, 0, 1, 0);
        }
    }

    private static int scale(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // The machine spans the 3x3x4 chamber behind the controller
    @Override
    public AABB getRenderBoundingBox(CrystallizerBlockEntity entity) {
        return new AABB(entity.centre()).inflate(1.6, 0.0, 1.6).expandTowards(0.0, 4.0, 0.0);
    }
}
