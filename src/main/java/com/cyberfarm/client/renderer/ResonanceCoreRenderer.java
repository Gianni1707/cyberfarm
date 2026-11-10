package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.StabilizerPylonBlock;
import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;
import com.mojang.math.Axis;

import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.BlockEntityRenderer;
import com.cyberfarm.port.client.BlockEntityRenderState;
import com.cyberfarm.port.client.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import com.cyberfarm.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The floating crystal of a Resonance Core: a slowly turning rhombic dodecahedron of glass panels
 * around a glowing heart that grows with the charge. A bigger shell makes a bigger crystal, filling the vanished cells.
 * Online it spins, shines white and breathes as one solid, closing as the drones' waves land; unstable it slows, dims and
 * flickers violet, and its breath lurches with the orbit.
 * <p>
 * Every linked Stabilizer Pylon column sends a drone: it lifts off the column's cradle, glides out and circles the crystal,
 * rising and falling, and sends rings of waves from its belly to the crystal. Unstable, the orbit slows and lurches, the drones
 * tilt and the waves turn violet.
 * Each frame, per drone: FrameDroneRenderer's 26 boxes and up to 3 rings of 2 quads, so up to 4 drones and 24 quads while the core is in view.
 */
public class ResonanceCoreRenderer extends BlockEntityRenderer<ResonanceCoreBlockEntity, ResonanceCoreRenderer.State> {
    private static final RenderType SHELL = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/resonance_crystal.png"));
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/resonance_glow.png"));
    private static final RenderType WAVE = RenderTypes.eyes(CyberFarm.id("textures/entity/stabilizer_ring.png"));

    /** Crystal size of a lone core; with a shell it fills most of the cube. */
    private static final float BASE_SIZE = 0.45F;
    private static final float SHELL_FILL = 1.3F;
    private static final float BEAM_WIDTH = 0.06F;

    private static final float PIXEL = 1 / 16.0F;
    /** The drones are half as big again as the mini drones, so they read from afar. */
    private static final float DRONE_SIZE = 1.5F;
    /** Over the drone's feet, the lens on its belly the waves leave from. */
    private static final float DRONE_LENS = 2.1F * DRONE_SIZE * PIXEL;
    /** In ticks: rising off the cradle, then gliding to the orbit. */
    private static final float LIFT_TICKS = 30.0F, GLIDE_TICKS = 50.0F, LIFT_HEIGHT = 14.0F * PIXEL;
    /** The orbit: high enough over the crystal's centre that the drone's lowest point stays clear of it, a little outside the shell, never closer than a block to a column. */
    private static final float ORBIT_RISE = 20.0F * PIXEL, ORBIT_OUT = 10.0F * PIXEL;
    /** Radians a tick: a lap in six seconds; unstable, much slower and in lurches. Reaching a new speed takes this many ticks. */
    private static final float ORBIT_SPEED = 0.0524F, UNSTABLE_SPEED = 0.4F, SPEED_RAMP = 40.0F;
    private static final float BOB = 6.5F * PIXEL, JITTER = 4.0F * PIXEL, TILT = 12.0F;
    /** In ticks: one breath and one train of waves every half lap, so unstable they lurch with the orbit. A ring flies this long, this far behind the one before. */
    private static final float BREATH_TICKS = Mth.PI / ORBIT_SPEED, RING_FLIGHT = 40.0F, RING_GAP = 6.0F;
    private static final int RINGS = 3;
    /** Half a ring's size, leaving the lens; and on reaching a shell's crystal or a lone one. */
    private static final float RING_START = 3.0F * PIXEL, RING_SHELL = 18.0F * PIXEL, RING_LONE = 10.0F * PIXEL;
    /** How many ticks before the crystal is closed each side's first ring lands: opposite drones first, so it is squeezed from both sides. */
    private static final float[] RING_LEAD = {12.0F, 6.0F, 9.0F, 3.0F};
    private static final int LENS_ONLINE = 0xFFD1FEE7;

    private static final int SHELL_ONLINE = 0xD0FFFFFF;
    private static final int SHELL_OFFLINE = 0xA08890A0;
    private static final int HEART_ONLINE = 0xFFFFFFFF;
    private static final int HEART_UNSTABLE = 0xFFB070FF;
    private static final int WAVE_ONLINE = 0xFF9FE8FF;
    private static final int WAVE_UNSTABLE = 0xFFB070FF;

    /**
     * The 12 rhombus faces of a rhombic dodecahedron with axis tips at 1 and cube corners at 1/2:
     * each face joins two axis tips (on different axes) through the two cube corners between them.
     * Per face: four corners in outward winding order, then the outward normal.
     */
    private static final float[][][] FACES = buildFaces();

    public static class State extends BlockEntityRenderState {
        int radius;
        int status;
        float fill;
        float time;
        final int[] pylonDistance = new int[4];
        final int[] pylonHeight = new int[4];
        float droneAngle;
        final float[] droneLaunched = new float[4];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ResonanceCoreBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.radius = entity.radius();
        state.status = entity.status();
        state.fill = entity.fillPercent() / 100.0F;
        state.time = entity.getLevel() != null ? entity.getLevel().getGameTime() + partialTick : 0.0F;
        boolean first = Float.isNaN(entity.droneTime);
        float dt = first ? 0.0F : Mth.clamp(state.time - entity.droneTime, 0.0F, 5.0F);
        entity.droneTime = state.time;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int i = side.get2DDataValue();
            state.pylonDistance[i] = entity.pylonDistance(side);
            state.pylonHeight[i] = entity.pylonHeight(side);
            boolean linked = state.pylonDistance[i] > 0;
            if (linked && !entity.droneLinked[i]) {
                // A column just linked: its drone takes off now. One already linked when the core loaded is up there already
                entity.droneLaunched[i] = first ? Float.NaN : state.time;
            }
            entity.droneLinked[i] = linked;
            state.droneLaunched[i] = entity.droneLaunched[i];
        }
        // The orbit turns on steadily online; unstable it lurches, slow then quick, never back
        boolean online = state.status == ResonanceCoreBlockEntity.STATUS_ONLINE;
        float target = online ? ORBIT_SPEED : ORBIT_SPEED * UNSTABLE_SPEED * (1.0F + 1.2F * Math.max(0.0F, Mth.sin(state.time * 0.35F)));
        entity.droneSpeed += Mth.clamp(target - entity.droneSpeed, -ORBIT_SPEED / SPEED_RAMP * dt, ORBIT_SPEED / SPEED_RAMP * dt);
        if (first) {
            entity.droneSpeed = target;
        }
        entity.droneAngle = (entity.droneAngle + entity.droneSpeed * dt) % Mth.TWO_PI;
        state.droneAngle = entity.droneAngle;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean online = state.status == ResonanceCoreBlockEntity.STATUS_ONLINE;
        float time = state.time;
        float fullSize = state.radius == 0 ? BASE_SIZE : (state.radius + 0.5F) * SHELL_FILL;
        // 1 when closed. A crystal filling its shell can only close from there; a lone one is small and needs more to show at all
        float closed = 0.5F + 0.5F * Mth.cos(2.0F * state.droneAngle);
        float size = fullSize * (state.radius == 0 ? 1.35F - 0.6F * closed : 1.0F - 0.1F * closed);
        // Lone crystals bob; a crystal filling its shell stays put
        float crystalY = ResonanceCoreBlockEntity.crystalHeight(state.radius) + (state.radius == 0 ? Mth.sin(time * 0.05F) * 0.06F : 0.0F);
        // An unstable core stutters: its light drops out now and then
        float flicker = online ? 1.0F : (Mth.sin(time * 0.9F) * Mth.sin(time * 0.37F) > 0.3F ? 0.25F : 0.8F);

        poseStack.pushPose();
        poseStack.translate(0.5F, crystalY, 0.5F);
        // Big crystals turn slowly, like heavy things do
        float spin = time * (online ? 1.2F : 0.25F) / (1 + state.radius);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
        collector.submitCustomGeometry(poseStack, SHELL,
                (pose, buffer) -> polyhedron(pose, buffer, size, online ? SHELL_ONLINE : SHELL_OFFLINE));
        // The heart turns the other way and grows with the charge
        poseStack.mulPose(Axis.YP.rotationDegrees(-spin * 2.5F));
        float heart = size * 0.53F * (0.55F + 0.45F * state.fill);
        int heartColor = scale(online ? HEART_ONLINE : HEART_UNSTABLE, flicker * (0.55F + 0.45F * closed));
        collector.submitCustomGeometry(poseStack, GLOW, (pose, buffer) -> polyhedron(pose, buffer, heart, heartColor));
        poseStack.popPose();

        int waveColor = scale(online ? WAVE_ONLINE : WAVE_UNSTABLE, flicker);
        Vec3 crystal = new Vec3(0.5, crystalY, 0.5);
        float radius = (state.radius + 0.5F) * Mth.SQRT_OF_TWO + ORBIT_OUT;
        for (int distance : state.pylonDistance) {
            if (distance > 0) {
                radius = Math.min(radius, distance - 1.0F);
            }
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int i = side.get2DDataValue();
            int distance = state.pylonDistance[i];
            if (distance > 0) {
                drone(poseStack, collector, state, side, i, distance, radius, crystal, fullSize, online, waveColor);
            }
        }
    }

    /** One column's drone: on its way from the cradle to the orbit, or circling the crystal and sending it waves. */
    private static void drone(PoseStack poseStack, SubmitNodeCollector collector, State state, Direction side, int i, int distance,
            float radius, Vec3 crystal, float crystalSize, boolean online, int waveColor) {
        float time = state.time;
        // Its place in the orbit: the column's own bearing, turned with the orbit, so the drones keep evenly apart
        float angle = (float) Math.atan2(side.getStepZ(), side.getStepX()) + state.droneAngle;
        float wobble = online ? 0.0F : JITTER * Mth.sin(time * 0.9F + i);
        // A wave running round the orbit: the drones climb one after the other, however many there are
        Vec3 slot = new Vec3(0.5 + Mth.cos(angle) * (radius + wobble), crystal.y + ORBIT_RISE + BOB * Mth.sin(angle + state.droneAngle),
                0.5 + Mth.sin(angle) * (radius + wobble));
        Vec3 pad = new Vec3(0.5 + side.getStepX() * distance, state.pylonHeight[i] - 1 + StabilizerPylonBlock.PAD_TOP, 0.5 + side.getStepZ() * distance);
        float flown = Float.isNaN(state.droneLaunched[i]) ? Float.MAX_VALUE : time - state.droneLaunched[i];
        Vec3 at;
        if (flown < LIFT_TICKS) {
            at = pad.add(0.0, LIFT_HEIGHT * ease(flown / LIFT_TICKS), 0.0);
        } else if (flown < LIFT_TICKS + GLIDE_TICKS) {
            at = pad.add(0.0, LIFT_HEIGHT, 0.0).lerp(slot, ease((flown - LIFT_TICKS) / GLIDE_TICKS));
        } else {
            at = slot;
        }
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(crystal.x - at.x, crystal.z - at.z)));
        if (!online) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(TILT * Mth.sin(time * 0.5F + i)));
        }
        poseStack.scale(PIXEL * DRONE_SIZE, PIXEL * DRONE_SIZE, PIXEL * DRONE_SIZE);
        FrameDroneRenderer.drone(poseStack, collector, time + i, online ? LENS_ONLINE : WAVE_UNSTABLE, state.lightCoords);
        poseStack.popPose();
        if (flown >= LIFT_TICKS + GLIDE_TICKS) {
            Vec3 lens = at.add(0.0, DRONE_LENS, 0.0);
            waves(poseStack, collector, lens, crystal, crystalSize, state.radius == 0, state.droneAngle, RING_LEAD[i], waveColor);
        }
    }

    /** A drone's train of rings, each growing and fading as it flies from the lens to just short of the crystal's faces. */
    private static void waves(PoseStack poseStack, SubmitNodeCollector collector, Vec3 lens, Vec3 crystal, float crystalSize, boolean lone,
            float orbitAngle, float lead, int color) {
        float clock = orbitAngle / ORBIT_SPEED;
        Vec3 path = crystal.subtract(lens);
        double flight = path.length() - crystalSize * (lone ? 0.9F : 0.71F) - 2.0F * PIXEL;
        Vec3 along = path.normalize();
        Vec3 across = along.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 sideways = across.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : across.normalize();
        Vec3 up = along.cross(sideways).normalize();
        collector.submitCustomGeometry(poseStack, WAVE, (pose, buffer) -> {
            for (int ring = 0; ring < RINGS; ring++) {
                float age = Mth.positiveModulo(clock + lead - ring * RING_GAP + RING_FLIGHT, BREATH_TICKS);
                if (age < RING_FLIGHT) {
                    float flown = age / RING_FLIGHT;
                    float half = Mth.lerp(flown, RING_START, lone ? RING_LONE : RING_SHELL);
                    Vec3 centre = lens.add(along.scale(flight * flown));
                    Vec3 a = sideways.scale(half);
                    Vec3 b = up.scale(half);
                    // Eyes blend by adding, so a ring fades by going dark
                    int fade = scale(color, Math.min(ease(flown / 0.15F), ease((1.0F - flown) / 0.15F)));
                    twoSided(pose, buffer, new Vec3[] {centre.subtract(a).subtract(b), centre.add(a).subtract(b), centre.add(a).add(b), centre.subtract(a).add(b)}, fade);
                }
            }
        });
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    static void polyhedron(PoseStack.Pose pose, VertexConsumer buffer, float size, int color) {
        float[][] uv = {{0.0F, 0.5F}, {0.5F, 0.0F}, {1.0F, 0.5F}, {0.5F, 1.0F}};
        for (float[][] face : FACES) {
            float[] normal = face[4];
            for (int i = 0; i < 4; i++) {
                float[] corner = face[i];
                buffer.addVertex(pose, corner[0] * size, corner[1] * size, corner[2] * size).setColor(color)
                        .setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                        .setNormal(pose, normal[0], normal[1], normal[2]);
            }
        }
    }

    static void beam(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, int color) {
        beam(pose, buffer, from, to, color, BEAM_WIDTH);
    }

    /** Two crossed strips from {@code from} to {@code to}, each drawn both ways so it shows from any side. */
    static void beam(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, int color, float width) {
        Vec3 along = to.subtract(from).normalize();
        Vec3 across = along.cross(new Vec3(0.0, 1.0, 0.0));
        across = across.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : across.normalize();
        Vec3 up = along.cross(across).normalize();
        for (Vec3 side : new Vec3[] {across.scale(width), up.scale(width)}) {
            Vec3[] corners = {from.subtract(side), from.add(side), to.add(side), to.subtract(side)};
            twoSided(pose, buffer, corners, color);
        }
    }

    private static void twoSided(PoseStack.Pose pose, VertexConsumer buffer, Vec3[] corners, int color) {
        float[][] uv = {{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                int index = pass == 0 ? i : 3 - i;
                Vec3 corner = corners[index];
                buffer.addVertex(pose, (float) corner.x, (float) corner.y, (float) corner.z).setColor(color)
                        .setUv(uv[index][0], uv[index][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
            }
        }
    }

    private static int scale(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (color & 0xFF000000) | r << 16 | g << 8 | b;
    }

    private static float[][][] buildFaces() {
        float[][][] faces = new float[12][][];
        int n = 0;
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 3; b++) {
                int c = 3 - a - b;
                for (int sa = -1; sa <= 1; sa += 2) {
                    for (int sb = -1; sb <= 1; sb += 2) {
                        float[] tipA = new float[3];
                        float[] tipB = new float[3];
                        float[] up = new float[3];
                        float[] down = new float[3];
                        tipA[a] = sa;
                        tipB[b] = sb;
                        up[a] = down[a] = sa * 0.5F;
                        up[b] = down[b] = sb * 0.5F;
                        up[c] = 0.5F;
                        down[c] = -0.5F;
                        float[] normal = new float[3];
                        normal[a] = sa * Mth.SQRT_OF_TWO / 2;
                        normal[b] = sb * Mth.SQRT_OF_TWO / 2;
                        float[][] face = {tipA, up, tipB, down, normal};
                        // Keep the corners counter-clockwise seen from outside
                        if (dot(cross(sub(up, tipA), sub(tipB, tipA)), normal) < 0) {
                            face = new float[][] {tipA, down, tipB, up, normal};
                        }
                        faces[n++] = face;
                    }
                }
            }
        }
        return faces;
    }

    private static float[] sub(float[] p, float[] q) {
        return new float[] {p[0] - q[0], p[1] - q[1], p[2] - q[2]};
    }

    private static float[] cross(float[] p, float[] q) {
        return new float[] {p[1] * q[2] - p[2] * q[1], p[2] * q[0] - p[0] * q[2], p[0] * q[1] - p[1] * q[0]};
    }

    private static float dot(float[] p, float[] q) {
        return p[0] * q[0] + p[1] * q[1] + p[2] * q[2];
    }

    // The drones start from the farthest pylons: keep drawing while any of them is in view
    @Override
    public AABB getRenderBoundingBox(ResonanceCoreBlockEntity entity) {
        int reach = ResonanceCoreBlockEntity.maxDistance(ResonanceCoreBlockEntity.MAX_TIER) + 1;
        return new AABB(entity.getBlockPos()).inflate(reach, 0.0, reach).expandTowards(0.0, 2 * ResonanceCoreBlockEntity.MAX_TIER + 1, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
