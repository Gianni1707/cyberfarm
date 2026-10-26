package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.SwarmDockBlockEntity;
import com.cyberfarm.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Brings the Drone Dock and the Builder Swarm Dock to life from the drones the client already sees:
 * lights chase round the landing ring while a drone flies home, arcs of energy jump from the corner
 * beacons to a drone charging on the pad, and on a Swarm Dock each landing ring lights up while a
 * Mini Drone stands on it.
 */
public class DockRenderer<T extends DroneDockBlockEntity> implements BlockEntityRenderer<T, DockRenderer.State> {
    // Additive and full-bright, so the lights glow on the dark pad instead of being shaded like a surface
    private static final RenderType LIGHT = RenderTypes.eyes(CyberFarm.id("textures/entity/hologram.png"));
    private static final int CYAN = 0x2FE0FF;
    private static final int PALE = 0xB8F6FF;
    /** The pad's top, the corner beacons' lamps and the drone's middle above its feet. */
    private static final float PAD = 6.0F / 16.0F;
    private static final float BEACON = 9.0F / 16.0F;
    private static final float DRONE_MIDDLE = 0.2F;
    /** How far off a drone on its way home starts the landing lights. */
    private static final double HOMING_RANGE = 12.0;

    public static class State extends BlockEntityRenderState {
        float time;
        boolean homing;
        boolean swarm;
        /** Charging drones, relative to the dock's corner. */
        final List<Vec3> charging = new ArrayList<>();
        /** Which of the Swarm Dock's four rings have a drone on them, one bit each. */
        int parked;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.charging.clear();
        state.homing = false;
        state.parked = 0;
        state.swarm = entity instanceof SwarmDockBlockEntity;
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        state.time = level.getGameTime() + partialTick;
        BlockPos pos = entity.getBlockPos();
        AABB pad = new AABB(pos).expandTowards(0.0, 1.5, 0.0);
        for (DroneEntity drone : level.getEntitiesOfClass(DroneEntity.class, new AABB(pos).inflate(HOMING_RANGE))) {
            Vec3 at = drone.getPosition(partialTick);
            if (drone.getStatus() == DroneEntity.STATUS_RETURNING) {
                state.homing = true;
            }
            if (!pad.contains(at)) {
                continue;
            }
            Vec3 local = at.subtract(pos.getX(), pos.getY(), pos.getZ());
            if (state.swarm) {
                state.parked |= 1 << ((local.x > 0.5 ? 1 : 0) + (local.z > 0.5 ? 2 : 0));
            }
            if (drone.getStatus() == DroneEntity.STATUS_CHARGING) {
                state.charging.add(local.add(0.0, DRONE_MIDDLE, 0.0));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.homing && state.charging.isEmpty() && state.parked == 0) {
            return;
        }
        collector.submitCustomGeometry(poseStack, LIGHT, (pose, buffer) -> {
            if (state.homing && !state.swarm) {
                // Three lights chasing round the landing ring
                for (int i = 0; i < 3; i++) {
                    float angle = state.time * 0.25F + i * Mth.TWO_PI / 3;
                    float x = 0.5F + Mth.cos(angle) * 0.38F;
                    float z = 0.5F + Mth.sin(angle) * 0.38F;
                    flat(pose, buffer, x - 0.035F, z - 0.035F, x + 0.035F, z + 0.035F, PAD + 0.003F, 0xF0 << 24 | PALE);
                }
            }
            for (int corner = 0; corner < 4; corner++) {
                if ((state.parked & 1 << corner) != 0) {
                    // Where the drone parks, and the ring painted on the pad under it
                    float cx = 0.5F + (float) ((corner & 1) == 0 ? -DroneEntity.CORNER_OFFSET : DroneEntity.CORNER_OFFSET);
                    float cz = 0.5F + (float) ((corner & 2) == 0 ? -DroneEntity.CORNER_OFFSET : DroneEntity.CORNER_OFFSET);
                    ring(pose, buffer, cx, cz, 2.3F / 16, 1.0F / 16, PAD + 0.003F, 0xFF << 24 | PALE);
                }
            }
            // A fresh shape for the arcs every two ticks, so they crackle
            int crackle = (int) (state.time / 2);
            for (int d = 0; d < state.charging.size(); d++) {
                Vec3 drone = state.charging.get(d);
                RandomSource random = RandomSource.create(crackle * 31L + d);
                for (int corner = 0; corner < 4; corner++) {
                    // Not every beacon at once
                    if (random.nextInt(3) == 0) {
                        continue;
                    }
                    float bx = (corner & 1) == 0 ? 1.5F / 16 : 14.5F / 16;
                    float bz = (corner & 2) == 0 ? 1.5F / 16 : 14.5F / 16;
                    arc(pose, buffer, random, new Vec3(bx, BEACON, bz), drone, 0xC0 << 24 | PALE);
                }
            }
        });
    }

    /** A jagged line of light from {@code from} to {@code to}, in six crooked steps. */
    private static void arc(PoseStack.Pose pose, VertexConsumer buffer, RandomSource random, Vec3 from, Vec3 to, int colour) {
        Vec3 last = from;
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            Vec3 next = from.lerp(to, i / (float) steps);
            if (i < steps) {
                next = next.add((random.nextFloat() - 0.5F) * 0.12F, (random.nextFloat() - 0.5F) * 0.12F, (random.nextFloat() - 0.5F) * 0.12F);
            }
            segment(pose, buffer, last, next, 0.012F, colour);
            last = next;
        }
    }

    /** A thin beam between two points: two crossed strips, so it shows from any side. */
    public static void segment(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, float width, int colour) {
        Vector3f dir = new Vector3f((float) (b.x - a.x), (float) (b.y - a.y), (float) (b.z - a.z));
        if (dir.lengthSquared() < 1.0E-6F) {
            return;
        }
        dir.normalize();
        Vector3f u = new Vector3f(dir).cross(Math.abs(dir.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)).normalize(width);
        Vector3f v = new Vector3f(dir).cross(u).normalize(width);
        for (Vector3f side : new Vector3f[] {u, v}) {
            vertex(pose, buffer, (float) a.x - side.x, (float) a.y - side.y, (float) a.z - side.z, colour);
            vertex(pose, buffer, (float) a.x + side.x, (float) a.y + side.y, (float) a.z + side.z, colour);
            vertex(pose, buffer, (float) b.x + side.x, (float) b.y + side.y, (float) b.z + side.z, colour);
            vertex(pose, buffer, (float) b.x - side.x, (float) b.y - side.y, (float) b.z - side.z, colour);
        }
    }

    /** A flat ring round ({@code cx}, {@code cz}), in twelve pieces. */
    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float cx, float cz, float radius, float thick, float y, int colour) {
        int pieces = 12;
        for (int i = 0; i < pieces; i++) {
            float a0 = i * Mth.TWO_PI / pieces;
            float a1 = (i + 1) * Mth.TWO_PI / pieces;
            float r0 = radius - thick;
            vertex(pose, buffer, cx + Mth.cos(a0) * r0, y, cz + Mth.sin(a0) * r0, colour);
            vertex(pose, buffer, cx + Mth.cos(a0) * radius, y, cz + Mth.sin(a0) * radius, colour);
            vertex(pose, buffer, cx + Mth.cos(a1) * radius, y, cz + Mth.sin(a1) * radius, colour);
            vertex(pose, buffer, cx + Mth.cos(a1) * r0, y, cz + Mth.sin(a1) * r0, colour);
        }
    }

    private static void flat(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, float y, int colour) {
        vertex(pose, buffer, x0, y, z0, colour);
        vertex(pose, buffer, x0, y, z1, colour);
        vertex(pose, buffer, x1, y, z1, colour);
        vertex(pose, buffer, x1, y, z0, colour);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(T entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0.0, 1.5, 0.0);
    }
}
