package com.cyberfarm.port.net;

import net.minecraft.resources.ResourceLocation;

/** The 26.x payload: a message with a type id, sent over {@link PortNetwork}. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {}
}
