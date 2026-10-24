package com.cyberfarm.entity;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The pilot pressed the drill's or the blade's key: that tool comes out, or goes back. */
public record MechTogglePayload(int module) implements CustomPacketPayload {
    public static final Type<MechTogglePayload> TYPE = new Type<>(CyberFarm.id("mech_toggle"));
    public static final StreamCodec<ByteBuf, MechTogglePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(MechTogglePayload::new, MechTogglePayload::module);

    @Override
    public Type<MechTogglePayload> type() {
        return TYPE;
    }

    public static void handle(MechTogglePayload payload, IPayloadContext context) {
        MechModule[] modules = MechModule.values();
        if (context.player().getVehicle() instanceof MechEntity mech && payload.module() >= 0 && payload.module() < modules.length) {
            mech.toggleTool(modules[payload.module()]);
        }
    }
}
