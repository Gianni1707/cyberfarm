package com.cyberfarm.client.renderer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.HologramTableBlockEntity;
import com.cyberfarm.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Hologram Table's projection: the land around the table in relief, in its map colours shaded
 * like a vanilla map and touched with cyan, inside a frame of light with a scan line climbing through
 * it; the Drone Docks as pillars of light; the drones as dots moving live with a fading trail,
 * coloured by what they do; players as white arrows, hostile mobs as red dots, N/E/S/W on its edges
 * and a floating line that counts it all up. It wakes when a player comes near: the beam reaches up
 * and the land rises from the middle out like a wave, and folds away when they leave. While on, a
 * radar pulse sweeps out from the table now and then. Right-click the table to zoom out or in. All
 * from what the client already knows (loaded chunks, their block entities, the entities it tracks),
 * so nothing is sent over the network but the zoom.
 */
public class HologramRenderer implements BlockEntityRenderer<HologramTableBlockEntity, HologramRenderer.State> {
    /**
     * The projection is at least this wide, a little wider than the table, and shows a block at least
     * this big: zoomed out it grows instead of shrinking the land to a blur. Then its height above the
     * table's base.
     */
    private static final float MIN_WIDTH = 1.6F;
    private static final float BLOCK = MIN_WIDTH / 49;
    private static final float FLOOR = 0.85F;
    /** The top of the table's plate, where the beam leaves the emitter. */
    private static final float EMITTER = 0.32F;
    /** Taller land is cut off at this many blocks above the floor. */
    private static final int MAX_RELIEF = 24;
    private static final int REFRESH_TICKS = 40;
    /** A player this close wakes the table; it takes this long to rise, or to fold away. */
    private static final double WAKE_DISTANCE = 6.0;
    private static final float BOOT_TICKS = 30.0F;
    /** A radar pulse every so often, sweeping out to the corners of the map. */
    private static final float PULSE_PERIOD = 80.0F;
    private static final float PULSE_TICKS = 40.0F;
    /** The scan line climbs through the relief this often. */
    private static final float SCAN_TICKS = 60.0F;
    /** Drone trails: a point every two ticks, the last ten. */
    private static final int TRAIL_STEP = 2;
    private static final int TRAIL_LENGTH = 10;
    private static final float TEXT_SCALE = 0.006F;
    private static final RenderType HOLOGRAM = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final int CYAN = 0x2FE0FF;
    private static final int AMBER = 0xFFB347;
    private static final int PALE = 0xC9D3DC;
    private static final int RED = 0xFF4040;
    private static final int WHITE = 0xFFFFFF;
    // N, E, S, W in a small pixel font, read with north at the top
    private static final String[] LETTER_N = {"1..1", "11.1", "1.11", "1..1", "1..1"};
    private static final String[] LETTER_E = {"111", "1..", "11.", "1..", "111"};
    private static final String[] LETTER_S = {".11", "1..", ".1.", "..1", "11."};
    private static final String[] LETTER_W = {"1...1", "1...1", "1.1.1", "11.11", "1...1"};
    private static final float PIXEL = 0.012F;

    /**
     * The map's layout for a range: {@code cells} columns a side, each {@code step} blocks wide (two
     * past 32, so the columns to draw stay as many), how wide the whole map and one block show, and
     * how tall a block shows: never less than at 24 blocks, so the hills still read as hills.
     */
    public record Grid(int radius, int step, int cells, float width, float scale, float rise) {
        static Grid of(int radius) {
            int step = radius > 32 ? 2 : 1;
            int cells = 2 * (radius / step) + 1;
            float width = Math.max(MIN_WIDTH, cells * step * BLOCK);
            float scale = width / (cells * step);
            return new Grid(radius, step, cells, width, scale, Math.max(scale, BLOCK));
        }

        /** The map's north-west corner, from the table's. */
        float offset() {
            return 0.5F - width / 2;
        }

        /** Where a point {@code offset} blocks from the table's corner lands on the projection. */
        float local(double offset) {
            return offset() + (float) (offset + radius) * scale;
        }

        float cornerDistance() {
            return radius * Mth.SQRT_OF_TWO;
        }
    }

    /** A marker on the map: where, what colour, and for players which way they look. */
    private record Mark(Vec3 at, int colour, float yaw) {}

    public static class State extends BlockEntityRenderState {
        Grid grid = Grid.of(24);
        float[] tops = new float[0];
        int[] colours = new int[0];
        float peak;
        List<Vec3> docks = List.of();
        final List<Mark> drones = new ArrayList<>();
        final List<Mark> trails = new ArrayList<>();
        final List<Mark> players = new ArrayList<>();
        final List<Mark> mobs = new ArrayList<>();
        int working;
        float time;
        /** 0 asleep, 1 fully risen. */
        float power;
        /** How far out the radar pulse has gone, in blocks; negative between pulses. */
        float pulse;
    }

    private final Font font;

    public HologramRenderer(Font font) {
        this.font = font;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HologramTableBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.drones.clear();
        state.trails.clear();
        state.players.clear();
        state.mobs.clear();
        state.working = 0;
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        float time = now + partialTick;
        state.time = time;
        BlockPos centre = entity.getBlockPos();
        // Rises while a player is near, folds away when they leave
        Player viewer = Minecraft.getInstance().player;
        boolean near = viewer != null && viewer.distanceToSqr(Vec3.atCenterOf(centre)) < WAKE_DISTANCE * WAKE_DISTANCE;
        float elapsed = entity.lastFrame == 0.0F ? 0.0F : Mth.clamp(time - entity.lastFrame, 0.0F, 5.0F);
        entity.lastFrame = time;
        entity.power = Mth.clamp(entity.power + (near ? elapsed : -elapsed) / BOOT_TICKS, 0.0F, 1.0F);
        state.power = entity.power;
        if (state.power <= 0.0F) {
            return;
        }
        Grid grid = Grid.of(entity.radius());
        if (grid.radius() != entity.mapRadius || now - entity.mappedAt >= REFRESH_TICKS || now < entity.mappedAt) {
            map(entity, level, grid);
            entity.mappedAt = now;
        }
        state.grid = grid;
        state.tops = entity.mapTops;
        state.colours = entity.mapColours;
        state.peak = entity.mapPeak;
        state.docks = entity.mapDocks;
        float phase = time % PULSE_PERIOD;
        state.pulse = phase < PULSE_TICKS ? phase / PULSE_TICKS * grid.cornerDistance() : -1.0F;

        AABB area = new AABB(centre).inflate(grid.radius(), 96, grid.radius());
        boolean sample = now != entity.trailTick && now % TRAIL_STEP == 0;
        Set<Integer> seen = new HashSet<>();
        for (DroneEntity drone : level.getEntitiesOfClass(DroneEntity.class, area)) {
            int colour = switch (drone.getStatus()) {
                case DroneEntity.STATUS_WORKING -> CYAN;
                case DroneEntity.STATUS_RETURNING -> AMBER;
                default -> PALE;
            };
            if (drone.getStatus() == DroneEntity.STATUS_WORKING) {
                state.working++;
            }
            Vec3 at = onMap(entity, grid, drone.getPosition(partialTick));
            state.drones.add(new Mark(at, colour, 0.0F));
            ArrayDeque<Vec3> trail = entity.trails.computeIfAbsent(drone.getId(), id -> new ArrayDeque<>());
            if (sample) {
                trail.addLast(at);
                if (trail.size() > TRAIL_LENGTH) {
                    trail.removeFirst();
                }
            }
            seen.add(drone.getId());
            int i = 0;
            for (Vec3 point : trail) {
                // Older points fade out
                int alpha = 0x18 + 0x70 * ++i / TRAIL_LENGTH;
                state.trails.add(new Mark(point, alpha << 24 | colour, 0.0F));
            }
        }
        if (sample) {
            entity.trails.keySet().retainAll(seen);
            entity.trailTick = now;
        }
        for (Player player : level.players()) {
            if (area.contains(player.position())) {
                state.players.add(new Mark(onMap(entity, grid, player.getPosition(partialTick)), WHITE, player.getViewYRot(partialTick)));
            }
        }
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, area, mob -> mob instanceof Enemy && mob.isAlive())) {
            state.mobs.add(new Mark(onMap(entity, grid, mob.getPosition(partialTick)), RED, 0.0F));
        }
    }

    /** Where a point in the world shows on the projection, its height above the floor. */
    private static Vec3 onMap(HologramTableBlockEntity entity, Grid grid, Vec3 at) {
        BlockPos centre = entity.getBlockPos();
        float height = Mth.clamp((float) (at.y - entity.mapLowest), 0.0F, MAX_RELIEF + 8);
        return new Vec3(grid.local(at.x - centre.getX()), FLOOR + height * grid.rise(), grid.local(at.z - centre.getZ()));
    }

    /** Reads the land around the table: the top of each column, its map colour, and the docks. */
    private static void map(HologramTableBlockEntity entity, Level level, Grid grid) {
        BlockPos centre = entity.getBlockPos();
        int cells = grid.cells();
        int[] heights = new int[cells * cells];
        MapColor[] mapColours = new MapColor[cells * cells];
        int lowest = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < cells; dz++) {
            for (int dx = 0; dx < cells; dx++) {
                int x = centre.getX() - grid.radius() + dx * grid.step();
                int z = centre.getZ() - grid.radius() + dz * grid.step();
                int index = dz * cells + dx;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    heights[index] = Integer.MIN_VALUE;
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                pos.set(x, top, z);
                heights[index] = top;
                mapColours[index] = level.getBlockState(pos).getMapColor(level, pos);
            }
        }
        // The floor is the ground most of the map stands on, not its deepest pit: one ravine or
        // riverbed in range would lift all the rest up into a wall of columns
        int[] known = Arrays.stream(heights).filter(h -> h != Integer.MIN_VALUE).sorted().toArray();
        if (known.length > 0) {
            lowest = known[known.length / 10];
        }
        float[] tops = new float[cells * cells];
        int[] colours = new int[cells * cells];
        float peak = FLOOR;
        for (int i = 0; i < tops.length; i++) {
            if (heights[i] == Integer.MIN_VALUE) {
                tops[i] = Float.NaN;
                continue;
            }
            tops[i] = FLOOR + (Mth.clamp(heights[i] - lowest, 0, MAX_RELIEF) + 1) * grid.rise();
            peak = Math.max(peak, tops[i]);
            // As on a vanilla map: lighter where the land rises from the north, darker where it falls
            int north = i >= cells && heights[i - cells] != Integer.MIN_VALUE ? heights[i - cells] : heights[i];
            MapColor.Brightness brightness = heights[i] > north ? MapColor.Brightness.HIGH
                    : heights[i] < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
            colours[i] = tint(mapColours[i].calculateARGBColor(brightness));
        }
        List<Vec3> docks = new ArrayList<>();
        int radius = grid.radius();
        for (int cx = (centre.getX() - radius) >> 4; cx <= (centre.getX() + radius) >> 4; cx++) {
            for (int cz = (centre.getZ() - radius) >> 4; cz <= (centre.getZ() + radius) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos dock = blockEntity.getBlockPos();
                    int dx = dock.getX() - centre.getX();
                    int dz = dock.getZ() - centre.getZ();
                    if (blockEntity instanceof DroneDockBlockEntity && Math.abs(dx) <= radius && Math.abs(dz) <= radius) {
                        float top = tops[(dz + radius) / grid.step() * cells + (dx + radius) / grid.step()];
                        docks.add(new Vec3(grid.local(dx + 0.5), Float.isNaN(top) ? FLOOR : top, grid.local(dz + 0.5)));
                    }
                }
            }
        }
        if (entity.mapRadius != grid.radius()) {
            // Trails were drawn at the old scale
            entity.trails.clear();
        }
        entity.mapRadius = grid.radius();
        entity.mapTops = tops;
        entity.mapColours = colours;
        entity.mapPeak = peak;
        entity.mapDocks = List.copyOf(docks);
        entity.mapLowest = lowest == Integer.MAX_VALUE ? centre.getY() : lowest;
    }

    /** A map colour with a touch of the hologram's cyan, enough to look projected and still read as land. */
    private static int tint(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 9 / 10 + (CYAN >> 16 & 0xFF) / 10;
        int g = (rgb >> 8 & 0xFF) * 9 / 10 + (CYAN >> 8 & 0xFF) / 10;
        int b = (rgb & 0xFF) * 9 / 10 + (CYAN & 0xFF) / 10;
        return r << 16 | g << 8 | b;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.tops.length == 0 || state.power <= 0.0F) {
            return;
        }
        float power = state.power;
        // Markers, trails, the frame and the compass fade in once the land has risen
        float overlay = Mth.clamp((power - 0.7F) / 0.3F, 0.0F, 1.0F);
        // A gentle flicker, so it reads as a projection and not as solid blocks
        float flicker = 0.9F + 0.1F * Mth.sin(state.time * 0.6F) * Mth.sin(state.time * 0.23F);
        int landAlpha = (int) (0xD0 * flicker) << 24;
        Grid grid = state.grid;
        float[] shown = risenTops(state, power);
        collector.submitCustomGeometry(poseStack, HOLOGRAM, (pose, buffer) -> {
            beam(pose, buffer, grid, (int) (0x30 * flicker * Math.min(1.0F, power * 3.0F)) << 24 | CYAN, Math.min(1.0F, power * 2.0F));
            for (int dz = 0; dz < grid.cells(); dz++) {
                for (int dx = 0; dx < grid.cells(); dx++) {
                    column(pose, buffer, state, shown, dx, dz, landAlpha);
                }
            }
            if (overlay <= 0.0F) {
                return;
            }
            int markAlpha = (int) (0xFF * overlay);
            frame(pose, buffer, grid, FLOOR, 0.006F, (int) (0xB0 * overlay * flicker) << 24 | CYAN);
            scanLine(pose, buffer, state, overlay);
            compass(pose, buffer, grid, (int) (0xC0 * overlay) << 24 | CYAN);
            for (Vec3 dock : state.docks) {
                box(pose, buffer, (float) dock.x - 0.012F, (float) dock.y, (float) dock.z - 0.012F,
                        (float) dock.x + 0.012F, (float) dock.y + 0.18F, (float) dock.z + 0.012F, (int) (0xE0 * overlay) << 24 | CYAN);
            }
            for (Mark point : state.trails) {
                float x = (float) point.at().x;
                float y = (float) point.at().y + 0.012F;
                float z = (float) point.at().z;
                int alpha = (int) ((point.colour() >>> 24) * overlay) << 24;
                box(pose, buffer, x - 0.006F, y - 0.006F, z - 0.006F, x + 0.006F, y + 0.006F, z + 0.006F, alpha | point.colour() & 0xFFFFFF);
            }
            for (Mark drone : state.drones) {
                float x = (float) drone.at().x;
                float y = (float) drone.at().y;
                float z = (float) drone.at().z;
                // A drone the pulse sweeps over flashes brighter
                float size = pulsed(state, x, z) ? 0.026F : 0.018F;
                box(pose, buffer, x - size, y, z - size, x + size, y + 0.03F, z + size, markAlpha << 24 | drone.colour());
                // A faint line down to the floor, to read its height
                box(pose, buffer, x - 0.002F, FLOOR, z - 0.002F, x + 0.002F, y, z + 0.002F, (int) (0x50 * overlay) << 24 | drone.colour());
            }
            for (Mark mob : state.mobs) {
                float x = (float) mob.at().x;
                float y = (float) mob.at().y;
                float z = (float) mob.at().z;
                box(pose, buffer, x - 0.014F, y, z - 0.014F, x + 0.014F, y + 0.028F, z + 0.014F, markAlpha << 24 | RED);
            }
            for (Mark player : state.players) {
                arrow(pose, buffer, player.at(), player.yaw(), markAlpha << 24 | WHITE);
            }
        });
        if (overlay > 0.0F) {
            summary(state, poseStack, collector, camera, overlay);
        }
    }

    /** The floating line over the map that counts it all up, always turned towards the viewer. */
    private void summary(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float overlay) {
        FormattedCharSequence text = Component.translatable("gui.cyberfarm.hologram.summary", state.docks.size(), state.drones.size(),
                state.working, state.mobs.size(), state.grid.radius()).getVisualOrderText();
        poseStack.pushPose();
        poseStack.translate(0.5F, Math.max(state.peak, FLOOR + 0.3F) + 0.22F, 0.5F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        int alpha = Math.max(0x10, (int) (0xFF * overlay)) << 24;
        collector.submitText(poseStack, -font.width(text) / 2.0F, 0.0F, text, false, Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT, alpha | CYAN, (int) (0x50 * overlay) << 24, 0);
        poseStack.popPose();
    }

    /** Each column's top as the land rises: from the middle out, like a wave. */
    private static float[] risenTops(State state, float power) {
        int cells = state.grid.cells();
        int half = cells / 2;
        float[] shown = new float[cells * cells];
        for (int dz = 0; dz < cells; dz++) {
            for (int dx = 0; dx < cells; dx++) {
                int index = dz * cells + dx;
                float top = state.tops[index];
                float distance = Mth.sqrt((dx - half) * (dx - half) + (dz - half) * (dz - half)) / (half * Mth.SQRT_OF_TWO);
                float rise = Mth.clamp((power * 1.5F - distance) / 0.5F, 0.0F, 1.0F);
                rise = 1.0F - (1.0F - rise) * (1.0F - rise);
                shown[index] = Float.isNaN(top) || rise <= 0.0F ? Float.NaN : FLOOR + (top - FLOOR) * rise;
            }
        }
        return shown;
    }

    /** Whether the radar pulse is passing over this point of the map. */
    private static boolean pulsed(State state, float x, float z) {
        if (state.pulse < 0.0F) {
            return false;
        }
        float dx = (x - 0.5F) / state.grid.scale();
        float dz = (z - 0.5F) / state.grid.scale();
        return Math.abs(Mth.sqrt(dx * dx + dz * dz) - state.pulse) < 1.6F * state.grid.step();
    }

    /** The light rising from the emitter in the table's top towards the floor of the projection; {@code reach} 0-1 as it boots. */
    private static void beam(PoseStack.Pose pose, VertexConsumer buffer, Grid grid, int colour, float reach) {
        float high = EMITTER + (FLOOR - EMITTER) * reach;
        float half = grid.width() / 2 * reach;
        float a = 0.5F - half;
        float b = 0.5F + half;
        float[][] corners = {{a, a}, {b, a}, {b, b}, {a, b}};
        for (int i = 0; i < 4; i++) {
            float[] from = corners[i];
            float[] to = corners[(i + 1) % 4];
            vertex(pose, buffer, 0.47F, EMITTER, 0.47F, colour);
            vertex(pose, buffer, 0.53F, EMITTER, 0.53F, colour);
            vertex(pose, buffer, to[0], high, to[1], colour);
            vertex(pose, buffer, from[0], high, from[1], colour);
        }
    }

    /** A thin square of light round the edge of the map at height {@code y}. */
    private static void frame(PoseStack.Pose pose, VertexConsumer buffer, Grid grid, float y, float thick, int colour) {
        float a = grid.offset() - thick;
        float b = grid.offset() + grid.width() + thick;
        box(pose, buffer, a, y, a, b, y + thick, a + thick, colour);
        box(pose, buffer, a, y, b - thick, b, y + thick, b, colour);
        box(pose, buffer, a, y, a, a + thick, y + thick, b, colour);
        box(pose, buffer, b - thick, y, a, b, y + thick, b, colour);
    }

    /** Where the scan line is now, climbing from the floor to just over the tallest column. */
    private static float scanHeight(State state) {
        float progress = state.time % SCAN_TICKS / SCAN_TICKS;
        return FLOOR + (Math.max(state.peak, FLOOR + 0.1F) - FLOOR + 0.04F) * progress;
    }

    /**
     * A bright rim that climbs through the relief, like a scanner; the columns it passes light up
     * in {@link #column}. No sheet across the middle: a translucent sheet hides what is drawn after it.
     */
    private static void scanLine(PoseStack.Pose pose, VertexConsumer buffer, State state, float overlay) {
        float progress = state.time % SCAN_TICKS / SCAN_TICKS;
        // Fades in at the bottom and out at the top
        float fade = Mth.sin(progress * Mth.PI) * overlay;
        frame(pose, buffer, state.grid, scanHeight(state), 0.004F, (int) (0xC0 * fade) << 24 | CYAN);
    }

    /** N, E, S and W just outside the edges of the map, on its floor. */
    private static void compass(PoseStack.Pose pose, VertexConsumer buffer, Grid grid, int colour) {
        float a = grid.offset();
        float b = a + grid.width();
        float middle = 0.5F;
        float gap = 0.03F;
        letter(pose, buffer, LETTER_N, middle, a - gap - 2.5F * PIXEL, colour);
        letter(pose, buffer, LETTER_S, middle, b + gap + 2.5F * PIXEL, colour);
        letter(pose, buffer, LETTER_W, a - gap - 3.0F * PIXEL, middle, colour);
        letter(pose, buffer, LETTER_E, b + gap + 2.0F * PIXEL, middle, colour);
    }

    /** A pixel-font letter centred on ({@code cx}, {@code cz}), lying on the floor, its top towards north. */
    private static void letter(PoseStack.Pose pose, VertexConsumer buffer, String[] rows, float cx, float cz, int colour) {
        float y = FLOOR + 0.001F;
        float left = cx - rows[0].length() * PIXEL / 2;
        float top = cz - rows.length * PIXEL / 2;
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                if (rows[row].charAt(col) == '1') {
                    float x0 = left + col * PIXEL;
                    float z0 = top + row * PIXEL;
                    quad(pose, buffer, x0, y, z0, x0 + PIXEL, y, z0, x0 + PIXEL, y, z0 + PIXEL, x0, y, z0 + PIXEL, colour);
                }
            }
        }
    }

    /** A flat arrow pointing the way a player looks. */
    private static void arrow(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, float yaw, int colour) {
        float radians = yaw * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(radians);
        float fz = Mth.cos(radians);
        float x = (float) at.x;
        float y = (float) at.y + 0.02F;
        float z = (float) at.z;
        float tipX = x + fx * 0.045F;
        float tipZ = z + fz * 0.045F;
        float backX = x - fx * 0.02F;
        float backZ = z - fz * 0.02F;
        float sideX = -fz * 0.025F;
        float sideZ = fx * 0.025F;
        quad(pose, buffer, tipX, y, tipZ, backX + sideX, y, backZ + sideZ, x, y, z, backX - sideX, y, backZ - sideZ, colour);
    }

    /** One column of land: its top, and its sides down to lower neighbours or the floor. */
    private static void column(PoseStack.Pose pose, VertexConsumer buffer, State state, float[] shown, int dx, int dz, int alpha) {
        int cells = state.grid.cells();
        float top = shown[dz * cells + dx];
        if (Float.isNaN(top)) {
            return;
        }
        float width = state.grid.step() * state.grid.scale();
        float x0 = state.grid.offset() + dx * width;
        float z0 = state.grid.offset() + dz * width;
        float x1 = x0 + width;
        float z1 = z0 + width;
        int rgb = state.colours[dz * cells + dx];
        // Lit where the radar pulse passes, and where the scan line crosses the top of the column
        boolean scanned = state.power >= 1.0F && Math.abs(top - scanHeight(state)) < width;
        if (pulsed(state, x0 + width / 2, z0 + width / 2) || scanned) {
            rgb = lighter(rgb);
        }
        int colour = alpha | rgb;
        int side = alpha | darker(rgb);
        quad(pose, buffer, x0, top, z0, x1, top, z0, x1, top, z1, x0, top, z1, colour);
        float north = below(shown, cells, dx, dz - 1, top);
        float south = below(shown, cells, dx, dz + 1, top);
        float west = below(shown, cells, dx - 1, dz, top);
        float east = below(shown, cells, dx + 1, dz, top);
        if (north < top) {
            quad(pose, buffer, x0, north, z0, x1, north, z0, x1, top, z0, x0, top, z0, side);
        }
        if (south < top) {
            quad(pose, buffer, x0, south, z1, x1, south, z1, x1, top, z1, x0, top, z1, side);
        }
        if (west < top) {
            quad(pose, buffer, x0, west, z0, x0, west, z1, x0, top, z1, x0, top, z0, side);
        }
        if (east < top) {
            quad(pose, buffer, x1, east, z0, x1, east, z1, x1, top, z1, x1, top, z0, side);
        }
    }

    /** How far down the side facing a neighbour goes: to its top, or to the floor at the edge. */
    private static float below(float[] shown, int cells, int dx, int dz, float top) {
        if (dx < 0 || dz < 0 || dx >= cells || dz >= cells) {
            return FLOOR;
        }
        float neighbour = shown[dz * cells + dx];
        return Float.isNaN(neighbour) ? FLOOR : Math.min(neighbour, top);
    }

    /** Halfway to white: where the radar pulse is passing. */
    private static int lighter(int rgb) {
        return ((rgb >> 16 & 0xFF) + 255) / 2 << 16 | ((rgb >> 8 & 0xFF) + 255) / 2 << 8 | ((rgb & 0xFF) + 255) / 2;
    }

    private static int darker(int rgb) {
        return (rgb >> 16 & 0xFF) * 3 / 4 << 16 | (rgb >> 8 & 0xFF) * 3 / 4 << 8 | (rgb & 0xFF) * 3 / 4;
    }

    private static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int colour) {
        quad(pose, buffer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, colour);
        quad(pose, buffer, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, colour);
        quad(pose, buffer, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, colour);
        quad(pose, buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, colour);
        quad(pose, buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, colour);
        quad(pose, buffer, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, colour);
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

    @Override
    public AABB getRenderBoundingBox(HologramTableBlockEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(1.5, 2.5, 1.5);
    }
}
