package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.client.ProjectionRenderer;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.multiblock.Structure;
import com.cyberfarm.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * The drones an unfinished multiblock sends out when its machine is clicked. A small one sends a mini drone for every Cyber Frame it
 * lacks; a big one sends bigger drones, each over a group of the blocks it lacks. Each lifts off the machine, glides over its place and
 * projects those blocks there, or marks what is in the way, and goes home once they are placed.
 * A pinned Multiblock Projector sends mini drones the same way, from the player. Client side only, seen by whoever clicked, and nothing to bump into.
 * <p>
 * shortcut: they fly through walls and hover under low ceilings just the same; a real path needs them to be entities, not worth it for a hint.
 * Each frame, per drone out: 26 boxes, and when projecting 8 translucent quads and 6 for each block; nothing while none is out.
 */
public final class FrameDroneRenderer {
    /** See-through and writing no depth, so the beam never cuts what stands behind it. */
    public static final RenderType BEAM = RenderTypes.eyes(CyberFarm.id("textures/entity/methane_chamber_glow.png"));
    private static final RenderType ATLAS = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private static final float PIXEL = 1 / 16.0F;
    /** In blocks: how far over its blocks a drone hovers. In ticks: climbing, gliding a pixel, spacing the take-offs. */
    private static final float HOVER = 10.0F * PIXEL, RISE = 8.0F, RISE_SPEED = 2.0F, SPEED = 1.6F, STAGGER = 6.0F;
    /** Fading the projection in and out, and the hover's bob. */
    private static final float FADE_IN = 6.0F, UNPROJECT = 4.0F, BOB = 0.8F, BOB_PERIOD = 30.0F;
    /** The projection's pulse, and how far in from its cell it stays so it never fights the neighbours. */
    private static final float GHOST_LOW = 0.35F, GHOST_HIGH = 0.65F, PULSE_PERIOD = 40.0F, GHOST_INSET = 0.25F * PIXEL;
    /** The cone: half-width at the lens and, at least, at the blocks' top; opacity at both ends and how much of it reaches the edges. */
    private static final float CONE_TOP = 1.2F, CONE_BOTTOM = 7.5F, CONE_LIT = 0.4F, CONE_DIM = 0.18F, CONE_EDGE = 0.2F;
    /** The drones stay out this long, and while the player stays this close. */
    private static final float MAX_TICKS = 1200.0F;
    private static final double RANGE = 24.0, BIG_RANGE = 48.0;
    /** The real drone's chassis, smaller, the lens's height over the drone's feet; a big structure's are this much bigger. */
    private static final float SCALE = 0.7F, LENS_Y = 2.1F, BIG = 1.5F;
    /** A big structure's groups: they start this wide and grow until there are no more drones than this. */
    private static final int GROUP = 3, MAX_DRONES = 8;

    private static final int SHELL = 0xFF395853, DARK = 0xFF233E3B, STEEL = 0xFF7C8989, STEEL_LIGHT = 0xFFBDC6C5, BAND = 0xFF5EC2B8, SCREEN = 0xFF071314;
    private static final int LAMP = 0xFFD1FEE7, AMBER = 0xFFFFB347;
    public static final int LENS_OFF = 0xFF055B57;

    /** A pinned projection stops being kept up this long after its projector was last seen pinned there. */
    private static final float PROJECTOR_GRACE = 5.0F;

    private enum Cell { DONE, FREE, BLOCKED, USED }

    /** What a projector's drones are showing. */
    private record Job(Structure structure, BlockPos controller, Direction facing) {}

    /** A block a drone asks for: what goes there (null: nothing) and whether what is there now is right. */
    private record Target(BlockPos pos, @Nullable BlockState show, Predicate<BlockState> ok) {}

    private static final class Drone {
        final List<Target> targets;
        final int index;
        final float x;
        final float z;
        /** Over the machine's bottom, in blocks: the hover's height, and the top of its blocks. */
        final float hover;
        final float top;
        final float coneBottom;
        final float glide;
        final float rise;
        /** When it takes off, after the hint began. */
        final float start;
        final float arrive;
        float leaveAt = -1.0F;

        Drone(BlockPos machine, List<Target> targets, int index) {
            this(machine, targets, index, index * STAGGER);
        }

        Drone(BlockPos machine, List<Target> targets, int index, float start) {
            this.targets = targets;
            this.index = index;
            this.start = start;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (Target target : targets) {
                minX = Math.min(minX, target.pos().getX());
                maxX = Math.max(maxX, target.pos().getX());
                minZ = Math.min(minZ, target.pos().getZ());
                maxZ = Math.max(maxZ, target.pos().getZ());
                maxY = Math.max(maxY, target.pos().getY());
            }
            this.x = (minX + maxX + 1) / 2.0F;
            this.z = (minZ + maxZ + 1) / 2.0F;
            this.top = maxY + 1 - machine.getY();
            this.hover = top + HOVER;
            this.coneBottom = Math.max(CONE_BOTTOM, (Math.max(maxX - minX, maxZ - minZ) + 1) * 8.0F);
            float dx = x - machine.getX() - 0.5F, dz = z - machine.getZ() - 0.5F;
            this.glide = Mth.sqrt(dx * dx + dz * dz) * 16.0F / SPEED;
            this.rise = Math.max(RISE, Math.abs(hover - 1.0F) * 16.0F / RISE_SPEED);
            this.arrive = start + rise + glide;
        }

        void leave(float t) {
            if (leaveAt < 0.0F) {
                // Already on its way out: it gets there first, so it never jumps
                leaveAt = Math.max(t, arrive);
            }
        }

        boolean gone(float t) {
            return leaveAt >= 0.0F && t >= leaveAt + UNPROJECT + glide + rise;
        }
    }

    private static final class Hint {
        final Level level;
        final BlockPos machine;
        final float start;
        final float size;
        final double range;
        final List<Drone> drones = new ArrayList<>();
        /** Which of the structure's sizes it shows; called back, it only has to get home. */
        int sizeIndex;
        boolean closing;
        /** A projector's: what it shows, and when it was last pinned there. */
        @Nullable Job job;
        float seen;

        Hint(Level level, BlockPos machine, float start, float size, double range) {
            this.level = level;
            this.machine = machine;
            this.start = start;
            this.size = size;
            this.range = range;
        }
    }

    // One list for the one local player
    private static final List<Hint> HINTS = new ArrayList<>();

    private FrameDroneRenderer() {}

    /** A second click calls them back. */
    private static boolean recalled(Level level, BlockPos machine, float now) {
        for (Hint hint : HINTS) {
            if (hint.level == level && hint.machine.equals(machine) && !hint.closing) {
                hint.closing = true;
                hint.drones.forEach(drone -> drone.leave(now - hint.start));
                return true;
            }
        }
        return false;
    }

    /** A click on a lone small multiblock: a drone goes out for each frame it lacks. */
    public static void clicked(Level level, BlockPos machine, BlockState state, SmallMultiblockBlock block) {
        float now = level.getGameTime();
        if (recalled(level, machine, now)) {
            return;
        }
        BlockState frame = ModBlocks.CYBER_FRAME.get().defaultBlockState();
        Predicate<BlockState> plain = there -> there.is(ModBlocks.CYBER_FRAME.get()) && !there.getValue(CyberFrameBlock.FORMED);
        Hint hint = new Hint(level, machine, now, 1.0F, RANGE);
        for (BlockPos cell : block.frameCells(state, machine)) {
            Target target = new Target(cell, frame, plain);
            if (cellOf(level, target) != Cell.DONE) {
                hint.drones.add(new Drone(machine, List.of(target), hint.drones.size()));
            }
        }
        if (!hint.drones.isEmpty()) {
            HINTS.add(hint);
        }
    }

    /**
     * A click on the controller of an unfinished big structure: bigger drones go out, each for a group of the blocks it lacks.
     * With several sizes they start from the one with the most blocks already placed, the smallest if none, so the ground never decides;
     * each further click shows the next size, and after the largest they come home.
     */
    public static void clickedStructure(Level level, BlockPos controller, BlockState state, Blueprint... sizes) {
        float now = level.getGameTime();
        int shown = -1;
        for (Hint hint : HINTS) {
            if (hint.level == level && hint.machine.equals(controller) && !hint.closing) {
                shown = hint.sizeIndex;
            }
        }
        if (shown >= 0) {
            recalled(level, controller, now);
            if (shown + 1 < sizes.length) {
                launch(level, controller, state, sizes, shown + 1, now);
            }
            return;
        }
        int best = 0;
        int mostPlaced = -1;
        for (int i = 0; i < sizes.length; i++) {
            int placed = 0;
            for (Structure.Ghost ghost : sizes[i].ghosts(controller, state.getValue(MachineBlock.FACING))) {
                if (ghost.show() != null && !ghost.pos().equals(controller) && ghost.ok().test(level.getBlockState(ghost.pos()))) {
                    placed++;
                }
            }
            if (placed > mostPlaced) {
                mostPlaced = placed;
                best = i;
            }
        }
        launch(level, controller, state, sizes, best, now);
    }

    private static void launch(Level level, BlockPos controller, BlockState state, Blueprint[] sizes, int index, float now) {
        List<Target> missing = new ArrayList<>();
        for (Structure.Ghost ghost : sizes[index].ghosts(controller, state.getValue(MachineBlock.FACING))) {
            if (!ghost.ok().test(level.getBlockState(ghost.pos()))) {
                missing.add(new Target(ghost.pos(), ghost.show(), ghost.ok()));
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        int wrong = (int) missing.stream().filter(target -> cellOf(level, target) == Cell.BLOCKED).count();
        Minecraft.getInstance().player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.status", sizes[index].displayName(), missing.size() - wrong, wrong));
        Hint hint = new Hint(level, controller, now, BIG, BIG_RANGE);
        hint.sizeIndex = index;
        for (List<Target> group : groups(missing)) {
            hint.drones.add(new Drone(controller, group, hint.drones.size()));
        }
        HINTS.add(hint);
    }

    /**
     * Called every frame while a projector is pinned, with the blocks its structure still lacks: mini drones leave the player, each
     * for a group of them, and more follow for blocks that go missing later. They go home when it stops being pinned there.
     */
    public static void project(Level level, BlockPos player, Structure structure, BlockPos controller, Direction facing, List<Structure.Ghost> missing) {
        float now = level.getGameTime();
        Job job = new Job(structure, controller, facing);
        Hint hint = null;
        for (Hint other : HINTS) {
            if (other.level == level && job.equals(other.job)) {
                hint = other;
            }
        }
        if (hint == null) {
            if (missing.isEmpty()) {
                return;
            }
            hint = new Hint(level, player, now, 1.0F, Double.MAX_VALUE);
            hint.job = job;
            HINTS.add(hint);
        }
        hint.seen = now;
        Set<BlockPos> covered = new HashSet<>();
        for (Drone drone : hint.drones) {
            drone.targets.forEach(target -> covered.add(target.pos()));
        }
        List<Target> uncovered = new ArrayList<>();
        for (Structure.Ghost ghost : missing) {
            if (!covered.contains(ghost.pos())) {
                uncovered.add(new Target(ghost.pos(), ghost.show(), ghost.ok()));
            }
        }
        if (!uncovered.isEmpty()) {
            float t = now - hint.start;
            int order = 0;
            for (List<Target> group : groups(uncovered)) {
                hint.drones.add(new Drone(hint.machine, group, hint.drones.size(), t + order++ * STAGGER));
            }
        }
    }

    private static List<List<Target>> groups(List<Target> targets) {
        for (int size = GROUP; ; size++) {
            Map<Long, List<Target>> groups = new LinkedHashMap<>();
            for (Target target : targets) {
                BlockPos pos = target.pos();
                long key = BlockPos.asLong(Math.floorDiv(pos.getX(), size), Math.floorDiv(pos.getY(), size), Math.floorDiv(pos.getZ(), size));
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(target);
            }
            if (groups.size() <= MAX_DRONES) {
                return new ArrayList<>(groups.values());
            }
        }
    }

    private static Cell cellOf(Level level, Target target) {
        BlockState there = level.getBlockState(target.pos());
        if (target.ok().test(there)) {
            return Cell.DONE;
        }
        if (there.is(ModBlocks.CYBER_FRAME.get()) || there.is(ModBlocks.HIDDEN_FRAME.get())) {
            return Cell.USED;
        }
        return target.show() != null && (there.isAir() || there.canBeReplaced()) ? Cell.FREE : Cell.BLOCKED;
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Whether the machine has what it needed, or isn't there any more. */
    private static boolean over(BlockState machine) {
        if (machine.getBlock() instanceof SmallMultiblockBlock) {
            return machine.getValue(SmallMultiblockBlock.STAGE) != SmallMultiblockBlock.LOOSE;
        }
        return !(machine.getBlock() instanceof MachineBlock) || machine.hasProperty(CyberFrameBlock.FORMED) && machine.getValue(CyberFrameBlock.FORMED);
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        HINTS.removeIf(hint -> hint.level != level);
        if (HINTS.isEmpty() || level == null || minecraft.player == null) {
            return;
        }
        float now = level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        BlockStateModelSet models = minecraft.getModelManager().getBlockStateModelSet();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (Hint hint : HINTS) {
            float t = now - hint.start;
            boolean over = hint.job != null ? now - hint.seen > PROJECTOR_GRACE : over(level.getBlockState(hint.machine)) || t > MAX_TICKS
                    || minecraft.player.distanceToSqr(Vec3.atCenterOf(hint.machine)) > hint.range * hint.range;
            for (Drone drone : hint.drones) {
                List<Target> lacking = new ArrayList<>();
                List<Cell> cells = new ArrayList<>();
                for (Target target : drone.targets) {
                    Cell cell = cellOf(level, target);
                    if (cell != Cell.DONE) {
                        lacking.add(target);
                        cells.add(cell);
                    }
                }
                if (over || lacking.isEmpty()) {
                    drone.leave(t);
                }
                draw(poseStack, collector, level, hint, drone, lacking, cells, t, models);
            }
            hint.drones.removeIf(drone -> drone.gone(t));
        }
        HINTS.removeIf(hint -> hint.drones.isEmpty());
        poseStack.popPose();
    }

    private static void draw(PoseStack poseStack, SubmitNodeCollector collector, Level level, Hint hint, Drone drone, List<Target> lacking, List<Cell> cells, float t, BlockStateModelSet models) {
        BlockPos machine = hint.machine;
        float start = drone.start;
        float leave = drone.leaveAt;
        float out;     // from the machine's centre, 0, to over its blocks, 1
        float y;
        float projection;
        if (t < start) {
            return;
        } else if (leave >= 0.0F && t >= leave + UNPROJECT) {
            float back = t - leave - UNPROJECT;
            out = drone.glide > 0.0F ? 1.0F - ease(back / drone.glide) : 0.0F;
            y = drone.hover - (drone.hover - 1.0F) * ease((back - drone.glide) / drone.rise);
            projection = 0.0F;
        } else if (t < start + drone.rise) {
            out = 0.0F;
            y = 1.0F + (drone.hover - 1.0F) * ease((t - start) / drone.rise);
            projection = 0.0F;
        } else if (t < drone.arrive) {
            out = ease((t - start - drone.rise) / drone.glide);
            y = drone.hover;
            projection = 0.0F;
        } else {
            out = 1.0F;
            float settled = t - drone.arrive;
            y = drone.hover + BOB * PIXEL * Mth.sin(Mth.TWO_PI * settled / BOB_PERIOD + drone.index) * Math.min(1.0F, settled / FADE_IN);
            projection = leave >= 0.0F ? 1.0F - (t - leave) / UNPROJECT : Math.min(1.0F, settled / FADE_IN);
        }
        float x = machine.getX() + 0.5F + (drone.x - machine.getX() - 0.5F) * out;
        float z = machine.getZ() + 0.5F + (drone.z - machine.getZ() - 0.5F) * out;
        int light = LevelRenderer.getLightCoords(level, BlockPos.containing(x, machine.getY() + y, z));
        boolean anyFree = cells.contains(Cell.FREE);
        int lens = projection <= 0.0F ? LENS_OFF : cells.contains(Cell.BLOCKED) ? AMBER : anyFree ? LAMP : STEEL_LIGHT;

        poseStack.pushPose();
        poseStack.translate(x, machine.getY() + y, z);
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        poseStack.pushPose();
        poseStack.scale(hint.size, hint.size, hint.size);
        drone(poseStack, collector, t + drone.index, lens, light);
        poseStack.popPose();
        if (anyFree && projection > 0.0F) {
            float bottom = (drone.top - y) * 16.0F;
            float lensY = LENS_Y * hint.size;
            float wide = drone.coneBottom;
            float alpha = projection;
            collector.submitCustomGeometry(poseStack, BEAM, (pose, buffer) -> {
                cone(pose, buffer, 1.0F, 0.0F, lensY, bottom, wide, alpha);
                cone(pose, buffer, 0.0F, 1.0F, lensY, bottom, wide, alpha);
            });
        }
        poseStack.popPose();

        if (projection <= 0.0F) {
            return;
        }
        float pulse = GHOST_LOW + (GHOST_HIGH - GHOST_LOW) * (0.5F + 0.5F * Mth.sin(Mth.TWO_PI * t / PULSE_PERIOD));
        int ghost = (int) (pulse * projection * 255.0F) << 24 | 0xFFFFFF;
        collector.submitCustomGeometry(poseStack, ATLAS, (pose, buffer) -> {
            for (int i = 0; i < lacking.size(); i++) {
                Target target = lacking.get(i);
                if (cells.get(i) == Cell.FREE) {
                    BlockPos pos = target.pos();
                    ProjectionRenderer.cube(pose, buffer, models.getParticleMaterial(target.show()).sprite(), pos.getX() + GHOST_INSET, pos.getY() + GHOST_INSET, pos.getZ() + GHOST_INSET,
                            1.0F - 2.0F * GHOST_INSET, ghost);
                }
            }
        });
        for (int i = 0; i < lacking.size(); i++) {
            Cell cell = cells.get(i);
            if (cell == Cell.FREE) {
                continue;
            }
            BlockPos pos = lacking.get(i).pos();
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            if (cell == Cell.BLOCKED) {
                cross(poseStack, collector);
            } else {
                poseStack.scale(PIXEL, PIXEL, PIXEL);
                collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> outline(pose, buffer));
            }
            poseStack.popPose();
        }
    }

    /** The mini drone at its feet's height: chassis, visor, band, antenna on its stem, a projector in three steps, then four arms with spinning blades. */
    public static void drone(PoseStack poseStack, SubmitNodeCollector collector, float time, int lens, int light) {
        float k = SCALE;
        int full = LightCoordsUtil.FULL_BRIGHT;
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            ModelParts.box(pose, buffer, -2.5F * k, 5.0F * k, -3.0F * k, 2.5F * k, 7.0F * k, 3.0F * k, SHELL, DARK, light);
            ModelParts.box(pose, buffer, -2.0F * k, 5.5F * k, 2.5F * k, 2.0F * k, 6.5F * k, 3.5F * k, SCREEN, SCREEN, light);
            ModelParts.box(pose, buffer, -1.8F * k, 6.7F * k, -1.8F * k, 1.8F * k, 8.3F * k, 1.8F * k, BAND, BAND, full);
            ModelParts.box(pose, buffer, -0.25F, 8.3F * k, -0.25F, 0.25F, 10.9F * k, 0.25F, STEEL, STEEL, light);
            ModelParts.box(pose, buffer, -0.6F * k, 10.9F * k, -0.6F * k, 0.6F * k, 12.1F * k, 0.6F * k, LAMP, LAMP, full);
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

    /** The cone of light under a drone, in pixels from its feet, reaching down to {@code bottom}: for what else mini drones light. */
    public static void beam(PoseStack poseStack, SubmitNodeCollector collector, float bottom, float wide, float alpha) {
        collector.submitCustomGeometry(poseStack, BEAM, (pose, buffer) -> {
            cone(pose, buffer, 1.0F, 0.0F, LENS_Y, bottom, wide, alpha);
            cone(pose, buffer, 0.0F, 1.0F, LENS_Y, bottom, wide, alpha);
        });
    }

    /** An octagon of three boxes. */
    private static void step(PoseStack.Pose pose, VertexConsumer buffer, float r, float y0, float y1, int colour, int light) {
        float narrow = r * 0.41F, wide = r * 0.7F;
        ModelParts.box(pose, buffer, -narrow, y0, -r, narrow, y1, r, colour, colour, light);
        ModelParts.box(pose, buffer, -r, y0, -narrow, r, y1, narrow, colour, colour, light);
        ModelParts.box(pose, buffer, -wide, y0, -wide, wide, y1, wide, colour, colour, light);
    }

    /** One plane of the cone of light, along (dx, dz), brightest down its middle; both windings, as this render type culls. */
    private static void cone(PoseStack.Pose pose, VertexConsumer buffer, float dx, float dz, float top, float bottom, float wide, float alpha) {
        int midTop = lit(CONE_LIT * alpha), midBottom = lit(CONE_DIM * alpha), edgeTop = lit(CONE_LIT * CONE_EDGE * alpha), edgeBottom = lit(CONE_DIM * CONE_EDGE * alpha);
        for (float side : new float[] {-1.0F, 1.0F}) {
            light(pose, buffer, 0.0F, top, 0.0F, midTop);
            light(pose, buffer, side * dx * CONE_TOP, top, side * dz * CONE_TOP, edgeTop);
            light(pose, buffer, side * dx * wide, bottom, side * dz * wide, edgeBottom);
            light(pose, buffer, 0.0F, bottom, 0.0F, midBottom);
            light(pose, buffer, 0.0F, bottom, 0.0F, midBottom);
            light(pose, buffer, side * dx * wide, bottom, side * dz * wide, edgeBottom);
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
