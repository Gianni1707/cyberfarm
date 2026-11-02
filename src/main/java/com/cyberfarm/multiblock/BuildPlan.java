package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * A structure linked to a Drone Dock for its Builder drones: what is in the way, which blocks are
 * still missing and which drone is working on what. First the site is cleared, from the top down:
 * wrong blocks where the structure goes, anything in the spaces that must stay empty and, inside a
 * schematic's box above its bottom layer, the land and what grows on it. Blocks holding things (a
 * chest, a machine), fluids and unbreakable blocks are left for the player. Then it goes up one
 * layer at a time: a layer is finished before any block of the next one goes in, so the drones wait
 * for its blocks rather than skip ahead. Within a layer the block nearest the drone goes first; a
 * block is claimed by one drone at a time, so several drones can share the work.
 */
public final class BuildPlan {
    /** A claim lapses if its drone has not placed the block by then (out of power, picked up...). */
    private static final int CLAIM_TICKS = 600;

    private record Claim(UUID drone, long until) {}

    private final Structure structure;
    private final BlockPos origin;
    private final Direction facing;
    /**
     * Only the blocks a drone places: not the spaces that must stay empty (the player clears them),
     * nor the halves a door or bed adds by itself, nor blocks without an item (water, fire...).
     */
    private final List<Structure.Ghost> ghosts;
    /** The heights of the layers that have blocks to place, bottom first. */
    private final List<Integer> layers;
    private final Map<BlockPos, Claim> claims = new HashMap<>();
    /** Every place of the layout, the spaces that must stay empty too. */
    private final Map<BlockPos, Structure.Ghost> layout = new HashMap<>();
    /** A schematic's box above its bottom layer, where it has no block: the land there is cleared. */
    private final List<BlockPos> land = new ArrayList<>();

    public BuildPlan(Structure structure, BlockPos origin, Direction facing) {
        this.structure = structure;
        this.origin = origin.immutable();
        this.facing = facing;
        List<Structure.Ghost> all = structure.ghosts(origin, facing);
        this.ghosts = all.stream().filter(ghost -> ghost.show() != null && cost(ghost.show()) != Items.AIR).toList();
        this.layers = ghosts.stream().map(ghost -> ghost.pos().getY()).distinct().sorted().toList();
        all.forEach(ghost -> layout.put(ghost.pos(), ghost));
        if (structure instanceof Schematic && !all.isEmpty()) {
            int minX = all.stream().mapToInt(ghost -> ghost.pos().getX()).min().orElseThrow();
            int maxX = all.stream().mapToInt(ghost -> ghost.pos().getX()).max().orElseThrow();
            int minZ = all.stream().mapToInt(ghost -> ghost.pos().getZ()).min().orElseThrow();
            int maxZ = all.stream().mapToInt(ghost -> ghost.pos().getZ()).max().orElseThrow();
            int maxY = all.stream().mapToInt(ghost -> ghost.pos().getY()).max().orElseThrow();
            for (BlockPos pos : BlockPos.betweenClosed(minX, origin.getY() + 1, minZ, maxX, maxY, maxZ)) {
                if (!layout.containsKey(pos)) {
                    land.add(pos.immutable());
                }
            }
        }
    }

    /** The land and what grows on it: what may be cleared from a schematic's box. */
    private static boolean natural(BlockState state) {
        return state.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    /** What may be broken: no fluid, nothing holding things (a chest keeps its contents), nothing unbreakable. */
    private static boolean breakable(Level level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0;
    }

    /** Whether the block at {@code pos} must go before the structure is built. */
    public boolean inTheWay(Level level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (!breakable(level, pos, here)) {
            return false;
        }
        Structure.Ghost ghost = layout.get(pos);
        return ghost != null ? !ghost.ok().test(here) && !here.canBeReplaced() : natural(here);
    }

    /** Everything still in the way, the highest first. */
    public List<BlockPos> toClear(Level level) {
        List<BlockPos> clear = new ArrayList<>();
        for (BlockPos pos : layout.keySet()) {
            if (inTheWay(level, pos)) {
                clear.add(pos);
            }
        }
        for (BlockPos pos : land) {
            if (inTheWay(level, pos)) {
                clear.add(pos);
            }
        }
        clear.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        return clear;
    }

    /**
     * The next block for {@code claimant} to clear: among those in the way that nobody else is
     * breaking and it can reach, in the highest layer, the nearest to {@code from}. Null when there
     * is none.
     */
    public @Nullable BlockPos claimClearing(ServerLevel level, UUID claimant, Vec3 from, Predicate<BlockPos> canReach) {
        long now = level.getGameTime();
        claims.values().removeIf(claim -> claim.until() < now || claim.drone().equals(claimant));
        BlockPos best = null;
        for (BlockPos pos : toClear(level)) {
            if (best != null && pos.getY() < best.getY()) {
                break;
            }
            if (!claims.containsKey(pos) && canReach.test(pos) && (best == null || pos.distToCenterSqr(from) < best.distToCenterSqr(from))) {
                best = pos;
            }
        }
        if (best != null) {
            claims.put(best, new Claim(claimant, now + CLAIM_TICKS));
        }
        return best;
    }

    /** The item a block uses up: air for the half a door or bed places by itself, and for blocks with no item. */
    public static Item cost(BlockState state) {
        boolean upperHalf = state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        boolean bedHead = state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
        return upperHalf || bedHead ? Items.AIR : state.getBlock().asItem();
    }

    public Structure structure() {
        return structure;
    }

    public BlockPos origin() {
        return origin;
    }

    public Direction facing() {
        return facing;
    }

    /** Y of the structure's highest layer, empty spaces included. */
    public int top() {
        return structure.ghosts(origin, facing).stream().mapToInt(ghost -> ghost.pos().getY()).max().orElse(origin.getY());
    }

    public int layerCount() {
        return layers.size();
    }

    /** The current layer counted from 1 at the bottom, 0 when no block can go in. */
    public int layerNumber(Level level) {
        OptionalInt layer = currentLayer(level);
        return layer.isPresent() ? layers.indexOf(layer.getAsInt()) + 1 : 0;
    }

    /**
     * Whether a drone could place this block now, given the items: it is missing, nothing solid is
     * there, its support is there (torches, plates, lanterns...) and the drone has room above it.
     */
    private boolean workable(Level level, Structure.Ghost ghost) {
        BlockPos pos = ghost.pos();
        BlockState here = level.getBlockState(pos);
        return !ghost.ok().test(here) && here.canBeReplaced() && ghost.show().canSurvive(level, pos)
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * Height of the layer being built: the lowest one with a block a drone could place. Blocks it
     * cannot place (in the way, hanging from a layer above, no room) do not hold it up; they are left
     * for the player. Empty when no block can go in.
     */
    public OptionalInt currentLayer(Level level) {
        return ghosts.stream().filter(ghost -> workable(level, ghost)).mapToInt(ghost -> ghost.pos().getY()).min();
    }

    /** Blocks still to place, by item: the dock lets in only these. */
    public Map<Item, Integer> missing(Level level) {
        return missing(level, ghost -> true);
    }

    /**
     * Blocks the current layer still needs, by item, in the order the structure lists them; once no
     * layer is left for the drones, the blocks left for the player.
     */
    public Map<Item, Integer> missingInLayer(Level level) {
        OptionalInt layer = currentLayer(level);
        return layer.isPresent() ? missing(level, ghost -> ghost.pos().getY() == layer.getAsInt()) : missing(level);
    }

    private Map<Item, Integer> missing(Level level, Predicate<Structure.Ghost> which) {
        Map<Item, Integer> missing = new LinkedHashMap<>();
        for (Structure.Ghost ghost : ghosts) {
            if (which.test(ghost) && !ghost.ok().test(level.getBlockState(ghost.pos()))) {
                missing.merge(cost(ghost.show()), 1, Integer::sum);
            }
        }
        return missing;
    }

    public int remaining(Level level) {
        return (int) ghosts.stream().filter(ghost -> !ghost.ok().test(level.getBlockState(ghost.pos()))).count();
    }

    /** Wrong blocks the Builder cannot clear: they hold things, or are fluids or unbreakable. The player moves them. */
    public int obstacles(Level level) {
        return (int) layout.values().stream().filter(ghost -> {
            BlockState here = level.getBlockState(ghost.pos());
            return !ghost.ok().test(here) && !here.canBeReplaced() && !inTheWay(level, ghost.pos());
        }).count();
    }

    /**
     * The next block for {@code claimant}: in the current layer, the one nearest to {@code from}
     * among those nobody else is placing, that it can reach and place now and whose item {@code has}
     * accepts. Null when there is none, also while the layer waits for blocks.
     */
    public Structure.@Nullable Ghost claim(ServerLevel level, UUID claimant, Vec3 from, Predicate<BlockPos> canReach, Predicate<Item> has) {
        long now = level.getGameTime();
        claims.values().removeIf(claim -> claim.until() < now || claim.drone().equals(claimant));
        OptionalInt layer = currentLayer(level);
        if (layer.isEmpty()) {
            return null;
        }
        Structure.Ghost best = null;
        for (Structure.Ghost ghost : ghosts) {
            BlockPos pos = ghost.pos();
            if (pos.getY() != layer.getAsInt() || claims.containsKey(pos) || !workable(level, ghost) || !canReach.test(pos)
                    || !has.test(cost(ghost.show())) || !canPlace(level, ghost)) {
                continue;
            }
            if (best == null || pos.distToCenterSqr(from) < best.pos().distToCenterSqr(from)) {
                best = ghost;
            }
        }
        if (best != null) {
            claims.put(best.pos(), new Claim(claimant, now + CLAIM_TICKS));
        }
        return best;
    }

    /** Whether the block can go in now: its support is there (torches, plates...) and no one stands in the way. */
    public boolean canPlace(Level level, Structure.Ghost ghost) {
        BlockState state = ghost.show();
        return state.canSurvive(level, ghost.pos()) && level.isUnobstructed(state, ghost.pos(), CollisionContext.empty());
    }

    public void release(BlockPos pos) {
        claims.remove(pos);
    }
}
