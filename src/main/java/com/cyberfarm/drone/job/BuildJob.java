package com.cyberfarm.drone.job;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.multiblock.BuildPlan;
import com.cyberfarm.multiblock.Structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Builder Module: places the missing blocks of the structure linked to the dock (with the
 * Multiblock Projector), lowest layer first. The blocks come from the dock, one kind per trip;
 * the drone never breaks anything in the way. The plan goes up one layer at a time, so everything
 * above the layer being built is empty: the drone rises out of the layer, then flies straight to
 * the block. Only when something stands in that straight line does it go like a crane, up over the
 * whole structure and across. Each leg starts a little before the last one ends, so it turns in a
 * smooth curve instead of stopping at every corner.
 */
public class BuildJob extends DroneJob {
    public static final int ENERGY_COST = 20;
    /** Blocks of air between the top of the structure and the drone while it crosses over. */
    private static final double CLEARANCE = 2.0;
    /** Ticks a climb or a crossing may take before the drone gives the block up. */
    private static final int LEG_LIMIT = 200;
    /** How far ahead of a corner the next leg starts; each leg also aims this far past its end, so the drone keeps its speed. */
    private static final double CORNER = 0.5;

    private enum Phase { RISE, CLIMB, CROSS, APPROACH }

    private final BuildPlan plan;
    private final Structure.Ghost ghost;
    private final Vec3 hover;
    private final double crossY;
    private Phase phase = Phase.RISE;
    private int legTicks;

    private BuildJob(BuildPlan plan, Structure.Ghost ghost) {
        this.plan = plan;
        this.ghost = ghost;
        this.hover = Vec3.atCenterOf(ghost.pos()).add(0.0, 1.0, 0.0);
        this.crossY = Math.max(plan.top() + CLEARANCE, hover.y);
    }

    public static @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        BuildPlan plan = dock.getBuildPlan();
        if (plan == null) {
            return null;
        }
        // Away from the dock only blocks whose item is in the cargo; on the dock also what it stocks
        Structure.Ghost ghost = plan.claim(level, drone.getUUID(), drone.position(), pos -> !drone.isUnreachable(pos),
                item -> drone.cargo().contains(stack -> stack.is(item)) || canLoadFromDock(drone, dock, stack -> stack.is(item)));
        if (ghost == null) {
            return null;
        }
        Item item = BuildPlan.cost(ghost.show());
        if (!drone.cargo().contains(stack -> stack.is(item))) {
            loadFromDock(drone, dock, stack -> stack.is(item), drone.transportCapacity());
        }
        return new BuildJob(plan, ghost);
    }

    @Override
    public Result tick(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        if (ghost.ok().test(level.getBlockState(ghost.pos()))) {
            plan.release(ghost.pos());
            return Result.DONE;
        }
        switch (phase) {
            case RISE -> {
                // Straight up out of the layer being built, onto the empty layer above it
                if (drone.getY() < hover.y - 0.1) {
                    drone.flyTowards(new Vec3(drone.getX(), hover.y + CORNER, drone.getZ()));
                } else {
                    nextLeg(drone.canFlyStraightTo(hover) ? Phase.APPROACH : Phase.CLIMB);
                }
            }
            case CLIMB -> {
                drone.flyTowards(new Vec3(drone.getX(), crossY + CORNER, drone.getZ()));
                if (drone.getY() >= crossY - CORNER) {
                    nextLeg(Phase.CROSS);
                }
            }
            case CROSS -> {
                drone.flyTowards(new Vec3(hover.x, crossY, hover.z));
                if (new Vec3(hover.x, drone.getY(), hover.z).distanceTo(drone.position()) < 3 * CORNER) {
                    nextLeg(Phase.APPROACH);
                }
            }
            case APPROACH -> {
                return switch (travelTo(drone, ghost.pos(), hover)) {
                    case MOVING -> Result.CONTINUE;
                    case FAILED -> giveUp(drone);
                    case ARRIVED -> place(drone, level);
                };
            }
        }
        return ++legTicks > LEG_LIMIT ? giveUp(drone) : Result.CONTINUE;
    }

    private void nextLeg(Phase next) {
        phase = next;
        legTicks = 0;
    }

    /** Skipped until the drone goes home, like any block it cannot reach. */
    private Result giveUp(DroneEntity drone) {
        drone.markUnreachable(ghost.pos());
        plan.release(ghost.pos());
        return Result.DONE;
    }

    private Result place(DroneEntity drone, ServerLevel level) {
        BlockPos pos = ghost.pos();
        plan.release(pos);
        // Something got there first, its support went away, or someone stands in the way: try again later
        if (!level.getBlockState(pos).canBeReplaced() || !plan.canPlace(level, ghost)) {
            return Result.DONE;
        }
        ItemStack item = drone.cargo().takeOne(stack -> stack.is(BuildPlan.cost(ghost.show())));
        if (item.isEmpty()) {
            return Result.RETURN;
        }
        BlockState state = ghost.show();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        state.getBlock().setPlacedBy(level, pos, state, null, item);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Effects.print(level, pos);
        Effects.beam(level, drone, pos);
        drone.useJobEnergy(ENERGY_COST);
        return Result.DONE;
    }

    @Override
    public void stop(DroneEntity drone, ServerLevel level) {
        plan.release(ghost.pos());
    }
}
