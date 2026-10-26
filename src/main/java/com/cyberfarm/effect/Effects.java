package com.cyberfarm.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Short visual moments, played on the clients of the players near them. */
public final class Effects {
    /** A block printed into place: a shell of light closing in on it and a scan line running down it. */
    public static final int PRINT = 0;
    /** A structure coming together: a ring of light rising through it, its edges lighting up behind it. */
    public static final int REVEAL = 1;
    /** A drone at work on a block: a short beam of light from it to the block. */
    public static final int BEAM = 2;

    private Effects() {}

    public static void print(ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(pos), new EffectPayload(PRINT, pos, pos, -1));
    }

    public static void beam(ServerLevel level, Entity drone, BlockPos target) {
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(target), new EffectPayload(BEAM, target, target, drone.getId()));
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
        PacketDistributor.sendToPlayersTrackingChunk(server, ChunkPos.containing(from), new EffectPayload(REVEAL, from, new BlockPos(maxX, maxY, maxZ), -1));
    }
}
