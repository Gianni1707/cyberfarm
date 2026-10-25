package com.cyberfarm.multiblock;

import java.util.List;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The server's schematics, sent to a player who joins and to everyone after /reload. */
public record SchematicsPayload(List<Schematic> schematics) implements CustomPacketPayload {
    public static final Type<SchematicsPayload> TYPE = new Type<>(CyberFarm.id("schematics"));
    public static final StreamCodec<ByteBuf, SchematicsPayload> STREAM_CODEC = Schematic.STREAM_CODEC.apply(ByteBufCodecs.list())
            .map(SchematicsPayload::new, SchematicsPayload::schematics);

    @Override
    public Type<SchematicsPayload> type() {
        return TYPE;
    }

    public static void handle(SchematicsPayload payload, IPayloadContext context) {
        Schematics.set(payload.schematics());
    }
}
