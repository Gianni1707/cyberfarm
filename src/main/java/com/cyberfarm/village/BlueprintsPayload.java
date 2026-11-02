package com.cyberfarm.village;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;
import com.cyberfarm.port.net.StreamCodec;

import io.netty.buffer.ByteBuf;

/** The blueprints a player knows, sent to that player's client when they log in or learn one. */
public record BlueprintsPayload(Blueprints blueprints) implements CustomPacketPayload {
    public static final Type<BlueprintsPayload> TYPE = new Type<>(CyberFarm.id("blueprints"));
    public static final StreamCodec<ByteBuf, BlueprintsPayload> STREAM_CODEC = Blueprints.STREAM_CODEC.map(BlueprintsPayload::new, BlueprintsPayload::blueprints);

    @Override
    public Type<BlueprintsPayload> type() {
        return TYPE;
    }

    public static void handle(BlueprintsPayload payload, IPayloadContext context) {
        Blueprints.client = payload.blueprints();
    }
}
