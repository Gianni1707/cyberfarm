package com.cyberfarm.drone;

import java.util.function.DoubleSupplier;

import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * How a drone flies: steered by hand in straight lines, with a few seconds of vanilla pathfinding
 * when it gets stuck, climbing over the terrain on long trips and landing gently on its pad.
 */
public final class DroneFlight {
    /** Within this horizontal distance of the dock the drone stops pathfinding and lands by hand. */
    public static final double LANDING_RADIUS = 2.5;
    /** The drone lines up this high above the pad before descending. */
    private static final double APPROACH_HEIGHT = 0.8;
    /** Beyond this horizontal distance the drone climbs over the terrain before heading home. */
    private static final double LONG_TRIP = 16.0;
    /** Cruise speed in blocks per tick of a 100% frame (3 blocks/s); the Basic frame flies at 80%. */
    private static final double BASE_SPEED = 0.15;
    /** Ticks pushing against a block before falling back to pathfinding... */
    private static final int STUCK_LIMIT = 20;
    /** ...and how long to pathfind before trying a straight line again. */
    private static final int PATHFIND_TICKS = 60;
    /** Blocks of air kept between the terrain and a hanging passenger's feet while cruising. */
    private static final int CLEARANCE = 4;

    private final DroneEntity drone;
    /** The frame's speed multiplier, read live because the modules change on setup and load. */
    private final DoubleSupplier frameSpeed;
    private int stuckTicks;
    private double returnAltitude = Double.NaN;
    private int pathfindTicks;

    public DroneFlight(DroneEntity drone, DoubleSupplier frameSpeed) {
        this.drone = drone;
        this.frameSpeed = frameSpeed;
    }

    /**
     * Flight altitude between two points: above the highest terrain sampled along the straight
     * line, plus room for a hanging passenger, and never below either end.
     */
    public static double cruiseAltitude(ServerLevel level, Vec3 from, Vec3 to) {
        double highest = Math.max(from.y, to.y);
        double length = from.subtract(to).horizontalDistance();
        int samples = Math.max(1, (int) (length / 4));
        for (int i = 0; i <= samples; i++) {
            Vec3 point = from.lerp(to, (double) i / samples);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(point.x), (int) Math.floor(point.z));
            highest = Math.max(highest, ground + CLEARANCE + DroneEntity.PASSENGER_DROP);
        }
        return Math.min(highest, level.getMaxY() - 2);
    }

    public double cruiseSpeed() {
        return BASE_SPEED * frameSpeed.getAsDouble();
    }

    /**
     * Flies in a straight line toward {@code target}, accelerating smoothly, easing off near it and
     * turning gradually. Call it every tick. Vanilla pathfinding re-plans between grid nodes and
     * zig-zags, so it is only used for a few seconds after the drone gets stuck against a block.
     */
    public void flyTowards(Vec3 target) {
        PathNavigation navigation = drone.getNavigation();
        if (pathfindTicks > 0) {
            pathfindTicks--;
            if (drone.tickCount % 10 == 0 || navigation.isDone()) {
                navigation.moveTo(target.x, target.y, target.z, frameSpeed.getAsDouble());
            }
            return;
        }
        navigation.stop();
        holdMoveControl();

        Vec3 delta = target.subtract(drone.position());
        double distance = delta.length();
        if (distance < 0.02) {
            drone.setDeltaMovement(Vec3.ZERO);
            return;
        }
        double speed = Math.min(cruiseSpeed(), distance * 0.3);
        Vec3 desired = delta.scale(speed / distance);
        drone.setDeltaMovement(drone.getDeltaMovement().lerp(desired, 0.5));
        if (desired.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(desired.z, desired.x) * Mth.RAD_TO_DEG) - 90.0F;
            drone.setYRot(Mth.approachDegrees(drone.getYRot(), yaw, 8.0F));
            drone.yBodyRot = drone.getYRot();
            drone.yHeadRot = drone.getYRot();
        }

        if (drone.horizontalCollision || (drone.verticalCollision && Math.abs(delta.y) > 0.5)) {
            if (++stuckTicks > STUCK_LIMIT) {
                stuckTicks = 0;
                pathfindTicks = PATHFIND_TICKS;
            }
        } else if (stuckTicks > 0) {
            stuckTicks--;
        }
    }

    /**
     * Brings the drone onto the pad at {@code home}: pathfinding up to a point above it, then a slow
     * vertical descent with speed easing off near the pad. Returns true once it is parked.
     * The vanilla flying move control always pushes at full speed, so it overshoots small
     * targets; the last metres are steered by hand instead.
     */
    public boolean flyHome(Vec3 home) {
        double horizontal = home.subtract(drone.position()).horizontalDistance();
        Vec3 approach = home.add(0.0, APPROACH_HEIGHT, 0.0);
        // Close to the dock but walled in (inside a structure a Builder is raising): climb out first
        if (horizontal > LANDING_RADIUS || isWalledIn(approach)) {
            if ((!Double.isNaN(returnAltitude) || horizontal > LONG_TRIP || isWalledIn(approach)) && drone.level() instanceof ServerLevel level) {
                // Long way home (e.g. after a taxi trip), or out of a quarry pit whose rim blocks
                // the straight line: climb over the terrain first, then cruise
                if (Double.isNaN(returnAltitude)) {
                    returnAltitude = cruiseAltitude(level, drone.position(), approach) - DroneEntity.PASSENGER_DROP;
                }
                flyTowards(drone.getY() < returnAltitude - 0.5
                        ? new Vec3(drone.getX(), returnAltitude, drone.getZ())
                        : new Vec3(home.x, returnAltitude, home.z));
            } else {
                flyTowards(approach);
            }
            return false;
        }

        returnAltitude = Double.NaN;
        pathfindTicks = 0;
        drone.getNavigation().stop();
        holdMoveControl();
        // Line up above the pad first, then descend straight down
        Vec3 target = horizontal > 0.2 ? new Vec3(home.x, Math.max(drone.getY(), home.y + APPROACH_HEIGHT), home.z) : home;
        Vec3 delta = target.subtract(drone.position());
        if (horizontal <= 0.2 && Math.abs(delta.y) < 0.03) {
            drone.setDeltaMovement(Vec3.ZERO);
            if (drone.position().distanceToSqr(home) > 1.0E-6) {
                drone.setPos(home.x, home.y, home.z);
            }
            return true;
        }
        double length = delta.length();
        double speed = Math.min(0.2, length * 0.35);
        drone.setDeltaMovement(delta.scale(speed / length));
        return false;
    }

    /** Whether no block stands on the straight line from the drone to {@code target}. */
    public boolean canFlyStraightTo(Vec3 target) {
        return drone.level().clip(new ClipContext(drone.position(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, drone))
                .getType() == HitResult.Type.MISS;
    }

    /**
     * Down in a pit: the way to {@code target} is blocked but the sky above is open, so climbing
     * out is safe (under trees or a cave roof the drone keeps steering around instead).
     */
    private boolean isWalledIn(Vec3 target) {
        return !canFlyStraightTo(target) && canFlyStraightTo(new Vec3(drone.getX(), Math.max(drone.getY(), target.y) + 1.0, drone.getZ()));
    }

    /** Stops the vanilla move control from steering, so the velocity set by hand is kept. */
    public void holdMoveControl() {
        drone.getMoveControl().setWantedPosition(drone.getX(), drone.getY(), drone.getZ(), 0.0);
    }
}
