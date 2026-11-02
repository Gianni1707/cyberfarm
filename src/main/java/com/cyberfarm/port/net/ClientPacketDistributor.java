package com.cyberfarm.port.net;

/** The 26.x client-side sender. */
public final class ClientPacketDistributor {
    private ClientPacketDistributor() {}

    public static void sendToServer(CustomPacketPayload payload) {
        PortNetwork.sendToServer(payload);
    }
}
