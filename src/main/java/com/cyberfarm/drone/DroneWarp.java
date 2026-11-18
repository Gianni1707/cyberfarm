package com.cyberfarm.drone;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.trinket.WarpPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A Taxi drone's warp, for trips too long to fly (the user's design): it flies a little, then jumps to
 * {@link #ARRIVE_SHORT} blocks short of where it is going and flies the rest. The spot's chunk is loaded first with a
 * ticket of its own, and the jump waits until it ticks, so the drone never lands in an unloaded chunk; a rider sees a
 * loading screen meanwhile (WarpOverlay).
 */
public final class DroneWarp {
    /** Farther than this, a Taxi leg warps. */
    public static final double DISTANCE = 128.0;
    /** How far short of the target it comes out, and how long it flies before jumping. */
    public static final double ARRIVE_SHORT = 14.0;
    public static final int FLY_FIRST = 60;
    /** What a warped block costs, against a flown one (the user's call: by the distance, but a little less). */
    public static final double WARP_SHARE = 1.0 / 3.0;
    /** How long a warp's chunk stays loaded: the time to land and park, or to fly the last stretch. */
    private static final int KEEP_TICKS = 400;

    /** Its own tickets, all dropped when the world loads: a warp never outlives the session. */
    public static final TicketController TICKETS = new TicketController(CyberFarm.id("drone_warp"), (level, tickets) -> {
        for (BlockPos owner : java.util.List.copyOf(tickets.getBlockTickets().keySet())) {
            tickets.removeAllTickets(owner);
        }
    });
    /**
     * The chunks this keeps loaded, by level, with the game time they are let go. By the level object, not its
     * dimension: a world reopened in the same game has new levels, and must not think the old session's chunks kept.
     */
    private static final Map<ServerLevel, Map<Long, Long>> KEPT = new WeakHashMap<>();

    private DroneWarp() {}

    /** The spot a warp towards {@code target} comes out at: short of it on the way from {@code from}, at height {@code y}. */
    public static Vec3 exit(Vec3 from, Vec3 target, double y) {
        Vec3 flat = new Vec3(target.x - from.x, 0.0, target.z - from.z);
        double length = flat.length();
        Vec3 back = length < 1.0E-3 ? Vec3.ZERO : flat.scale(Math.min(ARRIVE_SHORT, length) / length);
        return new Vec3(target.x - back.x, y, target.z - back.z);
    }

    /**
     * Warps the drone, with its rider, to short of {@code target} at height {@code y}. Returns false while the spot's
     * chunk is still loading (it has been asked for): the caller flies on and asks again next tick.
     */
    public static boolean tryWarp(DroneEntity drone, ServerLevel level, Vec3 target, double y) {
        Vec3 to = exit(drone.position(), target, y);
        keep(level, BlockPos.containing(to));
        if (!level.isPositionEntityTicking(BlockPos.containing(to))) {
            return false;
        }
        Vec3 from = drone.position();
        // The jump costs what flying it would, at a warp's share
        double jumped = new Vec3(to.x - from.x, 0.0, to.z - from.z).length();
        drone.useEnergy((int) Math.ceil(jumped / (drone.cruiseSpeed() * 0.9) * drone.flightCost() * WARP_SHARE));
        sparks(level, from);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.7F, 1.8F);
        for (var passenger : drone.getPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new WarpPayload());
            }
        }
        drone.teleportTo(to.x, to.y, to.z);
        drone.setDeltaMovement(Vec3.ZERO);
        sparks(level, to);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.7F, 1.8F);
        return true;
    }

    /** The blocks of a leg {@code length} long that a trip's charge counts: all those flown, a share of those warped. */
    public static double costDistance(double length) {
        return length <= DISTANCE ? length : DISTANCE + (length - DISTANCE) * WARP_SHARE;
    }

    /** Keeps the chunk at {@code pos} loaded and ticking for a while (longer if asked again). */
    public static void keep(ServerLevel level, BlockPos pos) {
        long chunk = ChunkPos.pack(pos);
        Map<Long, Long> kept = KEPT.computeIfAbsent(level, key -> new HashMap<>());
        if (!kept.containsKey(chunk)) {
            TICKETS.forceChunk(level, owner(chunk), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
        }
        kept.put(chunk, level.getGameTime() + KEEP_TICKS);
    }

    /** Lets go of the chunks whose time is up. */
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<Long, Long> kept = KEPT.get(level);
            if (kept == null) {
                continue;
            }
            long now = level.getGameTime();
            kept.entrySet().removeIf(entry -> {
                if (entry.getValue() > now) {
                    return false;
                }
                long chunk = entry.getKey();
                TICKETS.forceChunk(level, owner(chunk), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                return true;
            });
        }
    }

    /** A ticket's owner is the chunk's own corner, the same when it is taken and when it is let go. */
    private static BlockPos owner(long chunk) {
        return new BlockPos(ChunkPos.getX(chunk) << 4, 0, ChunkPos.getZ(chunk) << 4);
    }

    private static void sparks(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.6, 0.6, 0.6, 0.1);
    }
}
