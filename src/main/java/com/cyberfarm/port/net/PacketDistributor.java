package com.cyberfarm.port.net;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/** The 26.x server-side senders. */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PortNetwork.sendToPlayer(player, payload);
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        PortNetwork.sendToPlayersTrackingChunk(level, chunk, payload);
    }
}
