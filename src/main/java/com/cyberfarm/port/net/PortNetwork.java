package com.cyberfarm.port.net;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.cyberfarm.CyberFarm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Every CyberFarm payload travels as one SimpleChannel message: a small index naming the payload,
 * then the payload written by its own StreamCodec. Registered in the order of {@link #registrar}
 * calls, so client and server agree on the indexes.
 */
public final class PortNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CyberFarm.id("main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private record Entry<T extends CustomPacketPayload>(StreamCodec<?, T> codec, BiConsumer<T, IPayloadContext> handler, boolean toServer) {}

    private static final List<Entry<?>> ENTRIES = new ArrayList<>();
    private static final Map<CustomPacketPayload.Type<?>, Integer> INDEX = new IdentityHashMap<>();

    /** Wraps a payload with its index for the channel. */
    private record Message(int index, CustomPacketPayload payload) {}

    private PortNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Message.class, 0)
                .encoder(PortNetwork::encode)
                .decoder(PortNetwork::decode)
                .consumerMainThread(PortNetwork::handle)
                .add();
    }

    public static Registrar registrar(String version) {
        return new Registrar();
    }

    public static final class Registrar {
        public <T extends CustomPacketPayload> Registrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec, BiConsumer<T, IPayloadContext> handler) {
            return add(type, new Entry<>(codec, handler, true));
        }

        public <T extends CustomPacketPayload> Registrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<?, T> codec, BiConsumer<T, IPayloadContext> handler) {
            return add(type, new Entry<>(codec, handler, false));
        }

        private Registrar add(CustomPacketPayload.Type<?> type, Entry<?> entry) {
            INDEX.put(type, ENTRIES.size());
            ENTRIES.add(entry);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private static void encode(Message message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.index());
        ((Entry<CustomPacketPayload>) ENTRIES.get(message.index())).codec().encode(buf, message.payload());
    }

    private static Message decode(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        return new Message(index, ENTRIES.get(index).codec().decode(buf));
    }

    @SuppressWarnings("unchecked")
    private static void handle(Message message, Supplier<NetworkEvent.Context> context) {
        Entry<CustomPacketPayload> entry = (Entry<CustomPacketPayload>) ENTRIES.get(message.index());
        boolean onServer = context.get().getDirection() == NetworkDirection.PLAY_TO_SERVER;
        // A payload only goes the way it was registered for
        if (entry.toServer() != onServer) {
            return;
        }
        Player player = onServer ? context.get().getSender() : ClientPlayer.get();
        if (player != null) {
            entry.handler().accept(message.payload(), new IPayloadContext(player));
        }
    }

    private static Message wrap(CustomPacketPayload payload) {
        Integer index = INDEX.get(payload.type());
        if (index == null) {
            throw new IllegalStateException("Unregistered payload " + payload.type().id());
        }
        return new Message(index, payload);
    }

    static void sendToServer(CustomPacketPayload payload) {
        CHANNEL.sendToServer(wrap(payload));
    }

    static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), wrap(payload));
    }

    static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunk(chunk.x, chunk.z)), wrap(payload));
    }
}
