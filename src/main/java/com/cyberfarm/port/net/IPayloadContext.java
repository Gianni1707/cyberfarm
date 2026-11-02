package com.cyberfarm.port.net;

import net.minecraft.world.entity.player.Player;

/** The 26.x payload context: who the message is about. Handlers already run on the main thread. */
public final class IPayloadContext {
    private final Player player;

    IPayloadContext(Player player) {
        this.player = player;
    }

    /** The sender on the server, the local player on the client. */
    public Player player() {
        return player;
    }

    public void enqueueWork(Runnable work) {
        work.run();
    }
}
