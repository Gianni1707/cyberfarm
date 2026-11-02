package com.cyberfarm.multiblock;

import java.util.List;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;

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
