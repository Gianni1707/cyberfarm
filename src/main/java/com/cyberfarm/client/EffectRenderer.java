package com.cyberfarm.client;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.renderer.DockRenderer;
import com.cyberfarm.effect.Effects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.cyberfarm.port.client.LevelRenderEvents.ExtractLevelRenderStateEvent;
import com.cyberfarm.port.client.LevelRenderEvents.SubmitCustomGeometryEvent;

/**
 * Draws the short moments of {@link Effects}: a block printed into place by a Builder drone, a
 * structure revealed as it forms, a drone's beam on the block it works, a laser shot. Each lives for a fixed
 * number of ticks from when it arrived.
 */
public final class EffectRenderer {
    private static final RenderType LIGHT = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final int LAMP = 0xD1FEE7;
    private static final int PALE = 0xFFFFFF;
    private static final int LASER_RED = 0xFF2A1F;
    private static final int LASER_CORE = 0xFFD8D0;
    private static final float PRINT_TICKS = 12.0F;
    private static final float REVEAL_TICKS = 32.0F;
    private static final float BEAM_TICKS = 8.0F;
    private static final float LASER_TICKS = 6.0F;
    private static final float SPRAY_TICKS = 16.0F;
    private static final int SPRAY_DROPS = 20;
    private static final int SEED_GREEN = 0x9DFFB8;
    private static final int SEED_DARK = 0x3FBF6F;
    private static final int FERTILIZER_WHITE = 0xEFF5F0;
    private static final int FERTILIZER_GREY = 0xA6B5B2;
    private static final int FEED_STRAW = 0xE6CF7A;
    private static final int FEED_HAY = 0xC9A84A;
    /** Height above the drone's feet of the nozzles on its pump, where the spray leaves it. */
    private static final double NOZZLES = 3.0 / 16.0;

    private record Effect(int kind, BlockPos from, BlockPos to, int source, long start) {}

    /** An effect at {@code t} (0 to 1) through its life; {@code origin} is where its source entity is now, or null. */
    private record Frame(Effect effect, float t, Vec3 origin) {}

    // shortcut: effects kept in statics between payload, extract and submit; fine for the one local player
    private static final List<Effect> effects = new ArrayList<>();
    private static List<Frame> frames = List.of();

    private EffectRenderer() {}

    public static void add(int kind, BlockPos from, BlockPos to, int source) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        effects.add(new Effect(kind, from, to, source, level.getGameTime()));
        RandomSource random = level.getRandom();
        if (kind == Effects.LASER) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, from.getX() + 0.2 + random.nextDouble() * 0.6, from.getY() + 0.2 + random.nextDouble() * 0.6,
                        from.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
            }
            level.addParticle(ParticleTypes.SMOKE, from.getX() + 0.5, from.getY() + 0.6, from.getZ() + 0.5, 0.0, 0.02, 0.0);
        } else if (kind == Effects.BEAM) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, from.getX() + 0.5, from.getY() + 0.6, from.getZ() + 0.5, 0.0, 0.0, 0.0);
        } else if (kind == Effects.PRINT) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, from.getX() + random.nextDouble(), from.getY() + random.nextDouble(),
                        from.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
        } else if (kind == Effects.REVEAL) {
            // Motes of light rising off the structure's base as the ring sets off
            for (int i = 0; i < 24; i++) {
                double x = from.getX() + random.nextDouble() * (to.getX() - from.getX() + 1);
                double z = from.getZ() + random.nextDouble() * (to.getZ() - from.getZ() + 1);
                level.addParticle(ParticleTypes.END_ROD, x, from.getY() + 0.1, z, 0.0, 0.05 + random.nextDouble() * 0.05, 0.0);
            }
        }
    }

    public static void extract(ExtractLevelRenderStateEvent event) {
        frames = List.of();
        if (effects.isEmpty()) {
            return;
        }
        float time = event.getLevel().getGameTime() + Minecraft.getInstance().getPartialTick();
        List<Frame> now = new ArrayList<>();
        // A level change or a long pause can leave effects from another time: those go too
        float partialTick = Minecraft.getInstance().getPartialTick();
        effects.removeIf(effect -> {
            float t = (time - effect.start()) / switch (effect.kind()) {
                case Effects.PRINT -> PRINT_TICKS;
                case Effects.BEAM -> BEAM_TICKS;
                case Effects.LASER -> LASER_TICKS;
                case Effects.SPRAY_SEEDS, Effects.SPRAY_FERTILIZER, Effects.SPRAY_FEED -> SPRAY_TICKS;
                default -> REVEAL_TICKS;
            };
            if (t < 0.0F || t >= 1.0F) {
                return true;
            }
            Entity source = effect.source() >= 0 ? event.getLevel().getEntity(effect.source()) : null;
            // A drone's beam goes with its drone; a turret's laser has none
            if ((effect.kind() == Effects.BEAM || effect.source() >= 0) && source == null) {
                return true;
            }
            now.add(new Frame(effect, t, source == null ? null : source.getPosition(partialTick)));
            return false;
        });
        frames = now;
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        if (frames.isEmpty()) {
            return;
        }
        List<Frame> toDraw = frames;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, LIGHT, (pose, buffer) -> {
            for (Frame frame : toDraw) {
                switch (frame.effect().kind()) {
                    case Effects.PRINT -> print(pose, buffer, frame.effect().from(), frame.t());
                    case Effects.BEAM -> beam(pose, buffer, frame.origin(), frame.effect().from(), frame.t());
                    case Effects.LASER -> laser(pose, buffer, frame.origin() != null ? frame.origin().add(0.0, 0.1, 0.0) : turretMuzzle(frame.effect().to()),
                            frame.effect().from(), frame.t());
                    case Effects.SPRAY_SEEDS -> spray(pose, buffer, frame.origin(), frame.effect().from(), frame.t(), SEED_GREEN, SEED_DARK);
                    case Effects.SPRAY_FERTILIZER -> spray(pose, buffer, frame.origin(), frame.effect().from(), frame.t(), FERTILIZER_WHITE, FERTILIZER_GREY);
                    case Effects.SPRAY_FEED -> spray(pose, buffer, frame.origin(), frame.effect().from(), frame.t(), FEED_STRAW, FEED_HAY);
                    default -> reveal(pose, buffer, frame.effect().from(), frame.effect().to(), frame.t());
                }
            }
        });
        poseStack.popPose();
    }

    /** A beam from under the drone to the middle of the block, with a soft halo, fading out. */
    private static void beam(PoseStack.Pose pose, VertexConsumer buffer, Vec3 drone, BlockPos target, float t) {
        Vec3 from = drone.add(0.0, 0.1, 0.0);
        Vec3 to = Vec3.atCenterOf(target);
        float fade = 1.0F - t * t;
        DockRenderer.segment(pose, buffer, from, to, 0.012F, (int) (0xF0 * fade) << 24 | PALE);
        DockRenderer.segment(pose, buffer, from, to, 0.035F, (int) (0x50 * fade) << 24 | LAMP);
    }

    /** Drops leaving the pump one after the other, falling faster as they go and fanning out over the block. */
    private static void spray(PoseStack.Pose pose, VertexConsumer buffer, Vec3 drone, BlockPos target, float t, int light, int dark) {
        Vec3 nozzle = drone.add(0.0, NOZZLES, 0.0);
        Vec3 to = Vec3.atBottomCenterOf(target).add(0.0, 0.1, 0.0);
        for (int i = 0; i < SPRAY_DROPS; i++) {
            float p = (t - 0.45F * i / SPRAY_DROPS) / 0.55F;
            if (p <= 0.0F || p >= 1.0F) {
                continue;
            }
            // The same fan every time: the golden angle spreads the drops evenly round the block
            float angle = i * 2.4F;
            float spread = 0.08F + 0.34F * (i * 7 % SPRAY_DROPS) / SPRAY_DROPS;
            float x = Mth.lerp(p, (float) nozzle.x, (float) to.x + Mth.cos(angle) * spread);
            float y = Mth.lerp(p * p, (float) nozzle.y, (float) to.y);
            float z = Mth.lerp(p, (float) nozzle.z, (float) to.z + Mth.sin(angle) * spread);
            float size = 0.02F + 0.012F * (i % 3);
            box(pose, buffer, x - size, y - size, z - size, x + size, y + size, z + size,
                    (int) (0xE8 * (1.0F - p * p)) << 24 | (i % 3 == 0 ? dark : light));
        }
    }

    /** A Laser Turret's beam leaves from the middle of its head. */
    private static Vec3 turretMuzzle(BlockPos turret) {
        return Vec3.atBottomCenterOf(turret).add(0.0, 0.66, 0.0);
    }

    /** A hot white core in a red glow, fading fast. */
    private static void laser(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, BlockPos target, float t) {
        Vec3 to = Vec3.atCenterOf(target);
        float fade = 1.0F - t;
        DockRenderer.segment(pose, buffer, from, to, 0.02F, (int) (0xF8 * fade) << 24 | LASER_CORE);
        DockRenderer.segment(pose, buffer, from, to, 0.06F, (int) (0x70 * fade) << 24 | LASER_RED);
    }

    /** A shell of light closing in on the block and fading, and a scan line running down it. */
    private static void print(PoseStack.Pose pose, VertexConsumer buffer, BlockPos pos, float t) {
        float grow = 0.18F * (1.0F - t) * (1.0F - t);
        float x0 = pos.getX() - grow;
        float y0 = pos.getY() - grow;
        float z0 = pos.getZ() - grow;
        float size = 1.0F + 2 * grow;
        box(pose, buffer, x0, y0, z0, x0 + size, y0 + size, z0 + size, (int) (0x70 * (1.0F - t)) << 24 | LAMP);
        float y = pos.getY() + 1.02F - 1.04F * t;
        ring(pose, buffer, pos.getX() - 0.02F, pos.getZ() - 0.02F, pos.getX() + 1.02F, pos.getZ() + 1.02F, y, 0.04F,
                (int) (0xE0 * (1.0F - t * 0.6F)) << 24 | PALE);
    }

    /**
     * A bright ring rising from the base of the structure to its top, a softer halo round it, and the
     * box's corner edges lighting up behind it; all of it fades away at the end.
     */
    private static void reveal(PoseStack.Pose pose, VertexConsumer buffer, BlockPos from, BlockPos to, float t) {
        float x0 = from.getX() - 0.05F;
        float z0 = from.getZ() - 0.05F;
        float x1 = to.getX() + 1.05F;
        float z1 = to.getZ() + 1.05F;
        float bottom = from.getY();
        float top = to.getY() + 1.0F;
        // Eased so it slows as it reaches the top
        float rise = 1.0F - (1.0F - t) * (1.0F - t);
        float y = bottom + (top - bottom) * Math.min(1.0F, rise * 1.15F);
        float fade = t < 0.8F ? 1.0F : (1.0F - t) / 0.2F;
        ring(pose, buffer, x0, z0, x1, z1, y, 0.08F, (int) (0xF0 * fade) << 24 | PALE);
        ring(pose, buffer, x0 - 0.1F, z0 - 0.1F, x1 + 0.1F, z1 + 0.1F, y - 0.06F, 0.2F, (int) (0x50 * fade) << 24 | LAMP);
        int edge = (int) (0xB0 * fade) << 24 | LAMP;
        float w = 0.05F;
        for (float[] corner : new float[][] {{x0, z0}, {x1 - w, z0}, {x0, z1 - w}, {x1 - w, z1 - w}}) {
            box(pose, buffer, corner[0], bottom, corner[1], corner[0] + w, y, corner[1] + w, edge);
        }
        if (y >= top - 0.01F) {
            // Once the ring is at the top, the top edges light up too
            ring(pose, buffer, x0, z0, x1, z1, top, w, edge);
        }
    }

    /** A flat square ring of light at height {@code y}, {@code thick} wide. */
    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, float y, float thick, int colour) {
        float h = Mth.clamp(thick / 4, 0.01F, 0.03F);
        box(pose, buffer, x0, y, z0, x1, y + h, z0 + thick, colour);
        box(pose, buffer, x0, y, z1 - thick, x1, y + h, z1, colour);
        box(pose, buffer, x0, y, z0 + thick, x0 + thick, y + h, z1 - thick, colour);
        box(pose, buffer, x1 - thick, y, z0 + thick, x1, y + h, z1 - thick, colour);
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int colour) {
        quad(pose, buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, colour);
        quad(pose, buffer, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, colour);
        quad(pose, buffer, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, colour);
        quad(pose, buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, colour);
        quad(pose, buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, colour);
        quad(pose, buffer, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, colour);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz, int colour) {
        vertex(pose, buffer, ax, ay, az, colour);
        vertex(pose, buffer, bx, by, bz, colour);
        vertex(pose, buffer, cx, cy, cz, colour);
        vertex(pose, buffer, dx, dy, dz, colour);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
