package com.cyberfarm.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.client.renderer.FrameDroneRenderer;
import com.cyberfarm.drone.DroneTask;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * What a Drone Dock is set up to do, shown in the world by the mini drones its projector also uses: they lift off the dock and take
 * up their places, four on the corners of the work area, whose blocks then light up from the corners inwards, and one over each
 * end of the route and over the taxi's landing spot, each lighting that block. Shown only while the dock screen's Show Area switch
 * is on, which goes off with the dock's drone. Only what its drone uses. They fly home when it goes off.
 * <p>
 * shortcut: drawn only, like the projector's drones; they fly through walls, and a real path needs them to be entities.
 * Client side. While shown, each frame: 26 boxes and 8 translucent quads a drone, and a quad per block of the area (at most 33x33);
 * the blocks are read once a second. Nothing while none is out.
 */
public final class DockAreaView {
    private static final int AREA = 0xD1FEE7;
    private static final int ROUTE_SOURCE = 0x5EC2B8;
    private static final int ROUTE_END = 0x00B894;
    private static final double FORGOTTEN = 256.0;
    private static final float PIXEL = 1.0F / 16.0F;
    /** In blocks over what it lights, and blocks a tick, but never longer than this many ticks on the way. */
    private static final float HOVER = 1.1F, SPEED = 0.5F, MAX_GLIDE = 40.0F;
    /** In ticks: the climb, between take-offs, and the light going out. */
    private static final float RISE = 16.0F, STAGGER = 5.0F, FADE = 8.0F;
    /** Blocks a tick the light spreads over the area from its corners, and how bright a block and the area's rim get. */
    private static final float SPREAD = 1.0F, BLOCK_ALPHA = 0.22F, RIM_ALPHA = 0.5F;
    private static final float CONE_ALPHA = 0.8F, CONE_WIDE = 9.0F;
    private static final int REREAD_TICKS = 20;
    private static final float INSET = 0.03F, LIFT = 0.02F;

    /** A drone's place: over this block, and the colour of its light. A corner hovers over whatever stands in that column of the area. */
    private record Spot(BlockPos over, int colour, boolean corner) {}

    private record Plan(List<Spot> spots, @Nullable BoundingBox area) {}

    /** A block lit: where the light lies, how far from the nearest corner, how bright. */
    private record Lit(int x, int z, float top, float distance, float alpha, int colour) {}

    private static final class View {
        final BlockPos dock;
        final Plan plan;
        final Level level;
        final float start;
        /** Over each spot, in blocks; set when the drones are sent out, so they never bob up and down with the crops. */
        final float[] heights;
        float leaveAt = -1.0F;
        List<Lit> lit = List.of();
        long readAt;

        View(Level level, BlockPos dock, Plan plan, float start) {
            this.level = level;
            this.dock = dock;
            this.plan = plan;
            this.start = start;
            heights = new float[plan.spots().size()];
            for (int i = 0; i < heights.length; i++) {
                BlockPos over = plan.spots().get(i).over();
                float top = plan.spots().get(i).corner() ? surface(level, over.getX(), over.getZ(), plan.area()) : over.getY() + 1.0F;
                heights[i] = (Float.isNaN(top) ? over.getY() + 1.0F : top) + HOVER;
            }
        }

        float rise() {
            return RISE;
        }

        float glide(int spot) {
            BlockPos over = plan.spots().get(spot).over();
            float dx = over.getX() - dock.getX();
            float dz = over.getZ() - dock.getZ();
            return Math.min(MAX_GLIDE, Mth.sqrt(dx * dx + dz * dz) / SPEED);
        }

        float arrive(int spot) {
            return spot * STAGGER + rise() + glide(spot);
        }

        /** The last one in place: only then the light starts to spread. */
        float allArrived() {
            float last = 0.0F;
            for (int i = 0; i < heights.length; i++) {
                last = Math.max(last, arrive(i));
            }
            return last;
        }

        boolean gone(float t) {
            return leaveAt >= 0.0F && t >= leaveAt + FADE + MAX_GLIDE + RISE;
        }
    }

    // One list for the one local player
    private static final List<View> VIEWS = new ArrayList<>();
    private static @Nullable BlockPos switchedOn;

    private DockAreaView() {}

    public static boolean isOn(BlockPos dock) {
        return dock.equals(switchedOn);
    }

    public static void toggle(BlockPos dock) {
        switchedOn = dock.equals(switchedOn) ? null : dock.immutable();
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Player player = minecraft.player;
        VIEWS.removeIf(view -> view.level != level);
        if (level == null || player == null) {
            return;
        }
        // Off once the dock has no drone: the next drone put on it must not send them out by itself
        if (switchedOn != null && (player.distanceToSqr(Vec3.atCenterOf(switchedOn)) > FORGOTTEN * FORGOTTEN
                || !(level.getBlockEntity(switchedOn) instanceof DroneDockBlockEntity dock) || dock.droneTasks() == 0)) {
            switchedOn = null;
        }
        long now = level.getGameTime();
        Set<Plan> wanted = new HashSet<>();
        if (switchedOn != null && level.getBlockEntity(switchedOn) instanceof DroneDockBlockEntity dock) {
            Plan plan = plan(dock);
            if (!plan.spots().isEmpty()) {
                wanted.add(plan);
                if (VIEWS.stream().noneMatch(view -> view.leaveAt < 0.0F && view.plan.equals(plan))) {
                    VIEWS.add(new View(level, switchedOn, plan, now));
                }
            }
        }
        for (View view : VIEWS) {
            float t = now - view.start;
            if (view.leaveAt < 0.0F && !wanted.contains(view.plan)) {
                view.leaveAt = Math.max(t, view.allArrived());
            }
            if (now >= view.readAt) {
                view.readAt = now + REREAD_TICKS;
                view.lit = readLit(view);
            }
        }
        VIEWS.removeIf(view -> view.gone(now - view.start));
    }

    private static Plan plan(DroneDockBlockEntity dock) {
        int tasks = dock.droneTasks();
        List<Spot> spots = new ArrayList<>();
        BoundingBox area = null;
        if ((tasks & ~DroneTask.NO_AREA_TASKS) != 0) {
            area = dock.getWorkArea();
            int y = area.minY() + 1;
            spots.add(new Spot(new BlockPos(area.minX(), y, area.minZ()), AREA, true));
            spots.add(new Spot(new BlockPos(area.maxX(), y, area.minZ()), AREA, true));
            spots.add(new Spot(new BlockPos(area.maxX(), y, area.maxZ()), AREA, true));
            spots.add(new Spot(new BlockPos(area.minX(), y, area.maxZ()), AREA, true));
        }
        if ((tasks & DroneTask.TRANSPORT.bit()) != 0 && dock.getTransportSource() != null && dock.getTransportDestination() != null) {
            spots.add(new Spot(dock.getTransportSource(), ROUTE_SOURCE, false));
            spots.add(new Spot(dock.getTransportDestination(), ROUTE_END, false));
        }
        if ((tasks & DroneTask.TAXI.bit()) != 0 && dock.getTaxiDestination() != null) {
            spots.add(new Spot(dock.getTaxiDestination(), AREA, false));
        }
        return new Plan(spots, area);
    }

    /** The top of what stands highest in this column of the area: crops and water count; NaN with nothing in it. */
    private static float surface(Level level, int x, int z, BoundingBox area) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = area.maxY(); y >= area.minY(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            FluidState fluid = state.getFluidState();
            VoxelShape shape = state.getShape(level, pos);
            float top = Math.max(shape.isEmpty() ? 0.0F : (float) shape.max(Direction.Axis.Y), fluid.isEmpty() ? 0.0F : fluid.getHeight(level, pos));
            if (top > 0.0F) {
                return y + top;
            }
        }
        return Float.NaN;
    }

    private static List<Lit> readLit(View view) {
        List<Lit> lit = new ArrayList<>();
        BoundingBox area = view.plan.area();
        if (area != null) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    float top = surface(view.level, x, z, area);
                    if (!Float.isNaN(top)) {
                        boolean rim = x == area.minX() || x == area.maxX() || z == area.minZ() || z == area.maxZ();
                        int fromX = Math.min(x - area.minX(), area.maxX() - x);
                        int fromZ = Math.min(z - area.minZ(), area.maxZ() - z);
                        lit.add(new Lit(x, z, top, Mth.sqrt(fromX * fromX + fromZ * fromZ), rim ? RIM_ALPHA : BLOCK_ALPHA, AREA));
                    }
                }
            }
        }
        for (Spot spot : view.plan.spots()) {
            if (!spot.corner()) {
                lit.add(new Lit(spot.over().getX(), spot.over().getZ(), spot.over().getY() + 1.0F, 0.0F, RIM_ALPHA, spot.colour()));
            }
        }
        return lit;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (VIEWS.isEmpty() || level == null) {
            return;
        }
        float now = level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (View view : VIEWS) {
            float t = now - view.start;
            float fade = view.leaveAt >= 0.0F ? 1.0F - (t - view.leaveAt) / FADE : 1.0F;
            for (int i = 0; i < view.heights.length; i++) {
                drone(poseStack, collector, level, view, i, t, fade);
            }
            float spread = (t - view.allArrived()) * SPREAD;
            if (fade > 0.0F && spread > -1.0F) {
                List<Lit> lit = view.lit;
                collector.submitCustomGeometry(poseStack, FrameDroneRenderer.BEAM, (pose, buffer) -> {
                    for (Lit block : lit) {
                        float alpha = block.alpha() * fade * Mth.clamp(spread - block.distance() + 1.0F, 0.0F, 1.0F);
                        if (alpha > 0.0F) {
                            sheet(pose, buffer, block, (int) (alpha * 255.0F) << 24 | block.colour());
                        }
                    }
                });
            }
        }
        poseStack.popPose();
    }

    /** Lifts off the dock, glides to its place and hovers there with its lens lit; on the way home, the same backwards. */
    private static void drone(PoseStack poseStack, SubmitNodeCollector collector, Level level, View view, int spot, float t, float fade) {
        float take = spot * STAGGER;
        if (t < take) {
            return;
        }
        float glide = view.glide(spot);
        float arrive = view.arrive(spot);
        float high = view.heights[spot];
        float low = view.dock.getY() + 1.0F;
        float out;
        float y;
        if (fade <= 0.0F) {
            float back = t - view.leaveAt - FADE;
            out = glide > 0.0F ? 1.0F - ease(back / glide) : 0.0F;
            y = Mth.lerp(ease((back - glide) / RISE), high, low);
        } else if (t < take + RISE) {
            out = 0.0F;
            y = Mth.lerp(ease((t - take) / RISE), low, high);
        } else if (t < arrive) {
            out = ease((t - take - RISE) / glide);
            y = high;
        } else {
            out = 1.0F;
            y = high + 0.05F * Mth.sin(t * 0.2F + spot) * Math.min(1.0F, (t - arrive) / 8.0F);
        }
        BlockPos over = view.plan.spots().get(spot).over();
        float x = Mth.lerp(out, view.dock.getX() + 0.5F, over.getX() + 0.5F);
        float z = Mth.lerp(out, view.dock.getZ() + 0.5F, over.getZ() + 0.5F);
        boolean placed = t >= arrive && fade > 0.0F;
        int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(x, y, z));
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        FrameDroneRenderer.drone(poseStack, collector, t + spot, placed ? 0xFF000000 | view.plan.spots().get(spot).colour() : FrameDroneRenderer.LENS_OFF, light);
        if (placed) {
            float settled = Math.min(1.0F, (t - arrive) / FADE) * fade;
            FrameDroneRenderer.beam(poseStack, collector, -HOVER / PIXEL, CONE_WIDE, CONE_ALPHA * settled);
        }
        poseStack.popPose();
    }

    /** The light lying on a block, a hair above it and inset, so each block still reads as one. */
    private static void sheet(PoseStack.Pose pose, VertexConsumer buffer, Lit block, int colour) {
        float x0 = block.x() + INSET;
        float x1 = block.x() + 1.0F - INSET;
        float z0 = block.z() + INSET;
        float z1 = block.z() + 1.0F - INSET;
        float y = block.top() + LIFT;
        vertex(pose, buffer, x0, y, z0, colour);
        vertex(pose, buffer, x0, y, z1, colour);
        vertex(pose, buffer, x1, y, z1, colour);
        vertex(pose, buffer, x1, y, z0, colour);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
