package com.cyberfarm.entity;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The pilot pressed the exit key: the mech opens up and lets them out. */
public record MechExitPayload() implements CustomPacketPayload {
    public static final Type<MechExitPayload> TYPE = new Type<>(CyberFarm.id("mech_exit"));
    public static final MechExitPayload INSTANCE = new MechExitPayload();
    public static final StreamCodec<ByteBuf, MechExitPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<MechExitPayload> type() {
        return TYPE;
    }

    public static void handle(MechExitPayload payload, IPayloadContext context) {
        if (context.player().getVehicle() instanceof MechEntity mech) {
            mech.requestExit(context.player());
        }
    }
}
