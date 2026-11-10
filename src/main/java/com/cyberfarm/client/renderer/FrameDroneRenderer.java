package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.client.ProjectionRenderer;
import com.cyberfarm.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import com.cyberfarm.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.cyberfarm.port.client.LevelRenderEvents.SubmitCustomGeometryEvent;

/**
 * The mini drones a lone small multiblock sends out when clicked: one for every Cyber Frame it lacks. Each lifts off the machine,
 * glides over its cell and projects the frame into it, or marks what is in the way, and goes home once the frame is there.
 * Client side only, seen by whoever clicked, and nothing to bump into.
 * <p>
 * shortcut: they fly through walls and hover under low ceilings just the same; a real path needs them to be entities, not worth it for a hint.
 * Each frame, per drone out: 26 boxes, and when projecting 14 translucent quads; nothing while none is out.
 */
public final class FrameDroneRenderer {
    /** See-through and writing no depth, so the beam never cuts what stands behind it. */
    private static final RenderType BEAM = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));
    private static final RenderType ATLAS = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private static final float PIXEL = 1 / 16.0F;
    /** In model pixels and ticks: the machine's top, the hover 10 px over the cells, climbing, gliding, spacing the take-offs. */
    private static final float HOME_Y = 16.0F, HOVER_Y = 26.0F, RISE = 8.0F, SPEED = 1.6F, STAGGER = 6.0F;
    /** Fading the projection in and out, and the hover's bob. */
    private static final float FADE_IN = 6.0F, UNPROJECT = 4.0F, BOB = 0.8F, BOB_PERIOD = 30.0F;
    /** The projection's pulse, and how far in from its cell it stays so it never fights the neighbours. */
    private static final float GHOST_LOW = 0.35F, GHOST_HIGH = 0.65F, PULSE_PERIOD = 40.0F, GHOST_INSET = 0.25F * PIXEL;
    /** The cone: half-width at the lens and at the ghost's top, brightness at both ends. */
    private static final float CONE_TOP = 1.2F, CONE_BOTTOM = 7.5F, CONE_LIT = 0.4F, CONE_DIM = 0.18F, CONE_EDGE = 0.2F;
    /** The drones stay out this long, and while the player stays this close. */
    private static final float MAX_TICKS = 1200.0F;
    private static final double RANGE = 24.0;
    /** The lens's height over the drone's feet. */
    private static final float LENS_Y = 2.1F;
    /** The real drone's chassis, smaller. */
    private static final float SCALE = 0.7F;

    private static final int SHELL = 0xFF395853, DARK = 0xFF233E3B, STEEL = 0xFF7C8989, STEEL_LIGHT = 0xFFBDC6C5, BAND = 0xFF5EC2B8, SCREEN = 0xFF071314;
    private static final int LAMP = 0xFFD1FEE7, LENS_OFF = 0xFF055B57, AMBER = 0xFFFFB347;

    private enum Cell { DONE, FREE, BLOCKED, USED }

    private static final class Drone {
        final BlockPos cell;
        final int index;
        final float glide;
        final float arrive;
        float leaveAt = -1.0F;

        Drone(BlockPos machine, BlockPos cell, int index) {
            this.cell = cell;
            this.index = index;
            this.glide = Mth.sqrt((float) machine.distSqr(cell)) * 16.0F / SPEED;
            this.arrive = index * STAGGER + RISE + glide;
        }

        void leave(float t) {
            if (leaveAt < 0.0F) {
                // Already on its way out: it gets there first, so it never jumps
                leaveAt = Math.max(t, arrive);
            }
        }

        boolean gone(float t) {
            return leaveAt >= 0.0F && t >= leaveAt + UNPROJECT + glide + RISE;
        }
    }

    private static final class Hint {
        final Level level;
        final BlockPos machine;
        final float start;
        final List<Drone> drones = new ArrayList<>();

        Hint(Level level, BlockPos machine, float start) {
            this.level = level;
            this.machine = machine;
            this.start = start;
        }
    }

    // One list for the one local player
    private static final List<Hint> HINTS = new ArrayList<>();

    private FrameDroneRenderer() {}

    /** A click on a lone machine: out they go for the frames it lacks, or a second click calls them back. */
    public static void clicked(Level level, BlockPos machine, BlockState state, SmallMultiblockBlock block) {
        float now = level.getGameTime();
        for (Hint hint : HINTS) {
            if (hint.level == level && hint.machine.equals(machine)) {
                hint.drones.forEach(drone -> drone.leave(now - hint.start));
                return;
            }
        }
        Hint hint = new Hint(level, machine, now);
        for (BlockPos cell : block.frameCells(state, machine)) {
            if (cellOf(level, cell) != Cell.DONE) {
                hint.drones.add(new Drone(machine, cell, hint.drones.size()));
            }
        }
        if (!hint.drones.isEmpty()) {
            HINTS.add(hint);
        }
    }

    private static Cell cellOf(Level level, BlockPos cell) {
        BlockState there = level.getBlockState(cell);
        if (there.is(ModBlocks.CYBER_FRAME.get())) {
            return there.getValue(CyberFrameBlock.FORMED) ? Cell.USED : Cell.DONE;
        }
        return there.is(ModBlocks.HIDDEN_FRAME.get()) ? Cell.USED : there.isAir() || there.canBeReplaced() ? Cell.FREE : Cell.BLOCKED;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        HINTS.removeIf(hint -> hint.level != level);
        if (HINTS.isEmpty() || level == null || minecraft.player == null) {
            return;
        }
        float now = level.getGameTime() + minecraft.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        TextureAtlasSprite frame = minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(ModBlocks.CYBER_FRAME.get().defaultBlockState());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (Hint hint : HINTS) {
            float t = now - hint.start;
            BlockState machine = level.getBlockState(hint.machine);
            boolean over = !(machine.getBlock() instanceof SmallMultiblockBlock) || machine.getValue(SmallMultiblockBlock.STAGE) != SmallMultiblockBlock.LOOSE
                    || t > MAX_TICKS || minecraft.player.distanceToSqr(Vec3.atCenterOf(hint.machine)) > RANGE * RANGE;
            for (Drone drone : hint.drones) {
                Cell cell = cellOf(level, drone.cell);
                if (over || cell == Cell.DONE) {
                    drone.leave(t);
                }
                draw(poseStack, collector, level, hint.machine, drone, cell, t, frame);
            }
            hint.drones.removeIf(drone -> drone.gone(t));
        }
        HINTS.removeIf(hint -> hint.drones.isEmpty());
        poseStack.popPose();
    }

    private static void draw(PoseStack poseStack, SubmitNodeCollector collector, Level level, BlockPos machine, Drone drone, Cell cell, float t, TextureAtlasSprite frame) {
        float start = drone.index * STAGGER;
        float leave = drone.leaveAt;
        float out;     // from the machine's centre, 0, to over the cell, 1
        float y;
        float projection;
        if (t < start) {
            return;
        } else if (leave >= 0.0F && t >= leave + UNPROJECT) {
            float back = t - leave - UNPROJECT;
            out = 1.0F - ease(back / drone.glide);
            y = HOVER_Y - (HOVER_Y - HOME_Y) * ease((back - drone.glide) / RISE);
            projection = 0.0F;
        } else if (t < start + RISE) {
            out = 0.0F;
            y = HOME_Y + (HOVER_Y - HOME_Y) * ease((t - start) / RISE);
            projection = 0.0F;
        } else if (t < drone.arrive) {
            out = ease((t - start - RISE) / drone.glide);
            y = HOVER_Y;
            projection = 0.0F;
        } else {
            out = 1.0F;
            float settled = t - drone.arrive;
            y = HOVER_Y + BOB * Mth.sin(Mth.TWO_PI * settled / BOB_PERIOD + drone.index) * Math.min(1.0F, settled / FADE_IN);
            projection = leave >= 0.0F ? 1.0F - (t - leave) / UNPROJECT : Math.min(1.0F, settled / FADE_IN);
        }
        float x = machine.getX() + 0.5F + (drone.cell.getX() - machine.getX()) * out;
        float z = machine.getZ() + 0.5F + (drone.cell.getZ() - machine.getZ()) * out;
        int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(x, machine.getY() + y * PIXEL, z));
        int lens = projection <= 0.0F ? LENS_OFF : cell == Cell.BLOCKED ? AMBER : cell == Cell.USED ? STEEL_LIGHT : LAMP;

        poseStack.pushPose();
        poseStack.translate(x, machine.getY() + y * PIXEL, z);
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        drone(poseStack, collector, t + drone.index, lens, light);
        if (cell == Cell.FREE && projection > 0.0F) {
            float bottom = HOME_Y - y;
            float alpha = projection;
            collector.submitCustomGeometry(poseStack, BEAM, (pose, buffer) -> {
                cone(pose, buffer, 1.0F, 0.0F, bottom, alpha);
                cone(pose, buffer, 0.0F, 1.0F, bottom, alpha);
            });
        }
        poseStack.popPose();

        if (projection <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(drone.cell.getX(), drone.cell.getY(), drone.cell.getZ());
        switch (cell) {
            case FREE -> {
                float pulse = GHOST_LOW + (GHOST_HIGH - GHOST_LOW) * (0.5F + 0.5F * Mth.sin(Mth.TWO_PI * t / PULSE_PERIOD));
                int colour = (int) (pulse * projection * 255.0F) << 24 | 0xFFFFFF;
                collector.submitCustomGeometry(poseStack, ATLAS, (pose, buffer) ->
                        ProjectionRenderer.cube(pose, buffer, frame, GHOST_INSET, GHOST_INSET, GHOST_INSET, 1.0F - 2.0F * GHOST_INSET, colour));
            }
            case BLOCKED -> cross(poseStack, collector);
            case USED -> {
                poseStack.scale(PIXEL, PIXEL, PIXEL);
                collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> outline(pose, buffer));
            }
            default -> { }
        }
        poseStack.popPose();
    }

    /** The mini drone at its feet's height: chassis, visor, band, antenna light and lens, then four arms with spinning blades. */
    private static void drone(PoseStack poseStack, SubmitNodeCollector collector, float time, int lens, int light) {
        float k = SCALE;
        int full = LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, -2.5F * k, 5.0F * k, -3.0F * k, 2.5F * k, 7.0F * k, 3.0F * k, SHELL, DARK, light);
            ModelParts.box(pose, buffer, -2.0F * k, 5.5F * k, 2.5F * k, 2.0F * k, 6.5F * k, 3.5F * k, SCREEN, SCREEN, light);
            ModelParts.box(pose, buffer, -1.8F * k, 6.7F * k, -1.8F * k, 1.8F * k, 8.3F * k, 1.8F * k, BAND, BAND, full);
            ModelParts.box(pose, buffer, -0.25F, 8.3F * k, -0.25F, 0.25F, 10.9F * k, 0.25F, STEEL, STEEL, light);
            ModelParts.box(pose, buffer, -0.6F * k, 10.9F * k, -0.6F * k, 0.6F * k, 12.1F * k, 0.6F * k, LAMP, LAMP, full);
            // The projector under the chassis, in three steps down to the lens
            step(pose, buffer, 2.0F, 3.0F, 3.5F, DARK, light);
            step(pose, buffer, 1.5F, 2.5F, 3.0F, STEEL, light);
            step(pose, buffer, 0.9F, LENS_Y, 2.5F, lens, full);
        });
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + 90.0F * i));
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
                ModelParts.box(pose, buffer, 2.5F * k, 6.5F * k, -0.5F * k, 6.5F * k, 7.5F * k, 0.5F * k, STEEL, STEEL, light);
                ModelParts.box(pose, buffer, 5.5F * k, 7.0F * k, -1.0F * k, 7.5F * k, 9.0F * k, 1.0F * k, DARK, DARK, light);
            });
            poseStack.translate(6.5F * k, 9.2F * k, 0.0F);
            poseStack.mulPose(Axis.YP.rotation(time * 2.0F));
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) ->
                    ModelParts.box(pose, buffer, -3.0F * k, 0.0F, -0.5F * k, 3.0F * k, 0.2F, 0.5F * k, STEEL_LIGHT, STEEL_LIGHT, light));
            poseStack.popPose();
        }
    }

    /** An octagon of three boxes. */
    private static void step(PoseStack.Pose pose, VertexConsumer buffer, float r, float y0, float y1, int colour, int light) {
        float narrow = r * 0.41F, wide = r * 0.7F;
        ModelParts.box(pose, buffer, -narrow, y0, -r, narrow, y1, r, colour, colour, light);
        ModelParts.box(pose, buffer, -r, y0, -narrow, r, y1, narrow, colour, colour, light);
        ModelParts.box(pose, buffer, -wide, y0, -wide, wide, y1, wide, colour, colour, light);
    }

    /** One plane of the cone of light, along (dx, dz), brightest down its middle; both windings, as this render type culls. */
    private static void cone(PoseStack.Pose pose, VertexConsumer buffer, float dx, float dz, float bottom, float alpha) {
        float top = LENS_Y;
        int midTop = lit(CONE_LIT * alpha), midBottom = lit(CONE_DIM * alpha), edgeTop = lit(CONE_LIT * CONE_EDGE * alpha), edgeBottom = lit(CONE_DIM * CONE_EDGE * alpha);
        for (float side : new float[] {-1.0F, 1.0F}) {
            light(pose, buffer, 0.0F, top, 0.0F, midTop);
            light(pose, buffer, side * dx * CONE_TOP, top, side * dz * CONE_TOP, edgeTop);
            light(pose, buffer, side * dx * CONE_BOTTOM, bottom, side * dz * CONE_BOTTOM, edgeBottom);
            light(pose, buffer, 0.0F, bottom, 0.0F, midBottom);
            light(pose, buffer, 0.0F, bottom, 0.0F, midBottom);
            light(pose, buffer, side * dx * CONE_BOTTOM, bottom, side * dz * CONE_BOTTOM, edgeBottom);
            light(pose, buffer, side * dx * CONE_TOP, top, side * dz * CONE_TOP, edgeTop);
            light(pose, buffer, 0.0F, top, 0.0F, midTop);
        }
    }

    private static int lit(float alpha) {
        return (int) (alpha * 255.0F) << 24 | 0xD1FEE7;
    }

    private static void light(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** An amber X lying on what is in the way. */
    private static void cross(PoseStack poseStack, SubmitNodeCollector collector) {
        int full = LightCoordsUtil.FULL_BRIGHT;
        poseStack.translate(0.5F, 1.01F, 0.5F);
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        for (float degrees : new float[] {45.0F, -45.0F}) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) ->
                    ModelParts.box(pose, buffer, -9.0F, 0.0F, -1.0F, 9.0F, 0.2F, 1.0F, AMBER, AMBER, full));
            poseStack.popPose();
        }
    }

    /** A steel outline just outside a frame another machine already took. */
    private static void outline(PoseStack.Pose pose, VertexConsumer buffer) {
        int full = LightCoordsUtil.FULL_BRIGHT;
        float a = -0.1F, b = 16.1F, w = 0.7F;
        for (float p : new float[] {a, b - w}) {
            for (float q : new float[] {a, b - w}) {
                ModelParts.box(pose, buffer, p, a, q, p + w, b, q + w, STEEL_LIGHT, STEEL_LIGHT, full);
                ModelParts.box(pose, buffer, a, p, q, b, p + w, q + w, STEEL_LIGHT, STEEL_LIGHT, full);
                ModelParts.box(pose, buffer, p, q, a, p + w, q + w, b, STEEL_LIGHT, STEEL_LIGHT, full);
            }
        }
    }
}
