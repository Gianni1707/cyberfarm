package com.cyberfarm.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.cyberfarm.port.net.PacketDistributor;
import org.jspecify.annotations.Nullable;

/** Short visual moments, played on the clients of the players near them. */
public final class Effects {
    /** A block printed into place: a shell of light closing in on it and a scan line running down it. */
    public static final int PRINT = 0;
    /** A structure coming together: a ring of light rising through it, its edges lighting up behind it. */
    public static final int REVEAL = 1;
    /** A drone at work on a block: a short beam of light from it to the block. */
    public static final int BEAM = 2;
    /** A laser shot: a red beam from a Guard drone (the source) or a Laser Turret (at {@code to}) to the mob at {@code from}. */
    public static final int LASER = 3;
    /** A Planter drone sprays its tank's seed mix on a block: green drops from the pump under the hull. */
    public static final int SPRAY_SEEDS = 4;
    /** A Fertilizer drone sprays its canister's contents on a plant: white drops from the pump under the hull. */
    public static final int SPRAY_FERTILIZER = 5;
    /** A Breeder drone drops feed on an animal: golden drops from the hay rack under the hull. */
    public static final int SPRAY_FEED = 6;

    private Effects() {}

    public static void spray(ServerLevel level, Entity drone, int kind, BlockPos target) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(target), new EffectPayload(kind, target, target, drone.getId()));
    }

    public static void print(ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new EffectPayload(PRINT, pos, pos, -1));
    }

    public static void beam(ServerLevel level, Entity drone, BlockPos target) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(target), new EffectPayload(BEAM, target, target, drone.getId()));
    }

    public static void laser(ServerLevel level, BlockPos target, BlockPos turret, @Nullable Entity drone) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(target), new EffectPayload(LASER, target, turret, drone != null ? drone.getId() : -1));
    }

    /** Plays the reveal over the box holding all {@code blocks}; called once, as the structure forms. */
    public static void reveal(Level level, Iterable<BlockPos> blocks) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (minX > maxX) {
            return;
        }
        BlockPos from = new BlockPos(minX, minY, minZ);
        PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(from), new EffectPayload(REVEAL, from, new BlockPos(maxX, maxY, maxZ), -1));
    }
}
