package com.cyberfarm.multiblock;

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
 * A structure linked to a Drone Dock for its Builder drones: which blocks are still missing and
 * which drone is placing what. It goes up one layer at a time: a layer is finished before any block
 * of the next one goes in, so the drones wait for its blocks rather than skip ahead. Within a layer
 * the block nearest the drone goes first; a block is claimed by one drone at a time, so several
 * drones can share the work.
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

    public BuildPlan(Structure structure, BlockPos origin, Direction facing) {
        this.structure = structure;
        this.origin = origin.immutable();
        this.facing = facing;
        this.ghosts = structure.ghosts(origin, facing).stream().filter(ghost -> ghost.show() != null && cost(ghost.show()) != Items.AIR).toList();
        this.layers = ghosts.stream().map(ghost -> ghost.pos().getY()).distinct().sorted().toList();
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

    /** Wrong blocks the Builder cannot place over, and blocks in spaces that must stay empty: the Builder never breaks blocks. */
    public int obstacles(Level level) {
        return (int) structure.ghosts(origin, facing).stream().filter(ghost -> {
            BlockState here = level.getBlockState(ghost.pos());
            return !ghost.ok().test(here) && !here.canBeReplaced();
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
