package com.cyberfarm.trinket;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.WarpOverlay;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;

/** To a player hanging from a Taxi drone that warps: show the loading screen while the terrain comes in. */
public record WarpPayload() implements CustomPacketPayload {
    public static final Type<WarpPayload> TYPE = new Type<>(CyberFarm.id("warp"));
    public static final StreamCodec<ByteBuf, WarpPayload> STREAM_CODEC = StreamCodec.unit(new WarpPayload());

    @Override
    public Type<WarpPayload> type() {
        return TYPE;
    }

    public static void handle(WarpPayload payload, IPayloadContext context) {
        WarpOverlay.start();
    }
}
