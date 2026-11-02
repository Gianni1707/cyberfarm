package com.cyberfarm.port.net;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Only loaded on the client, when a message from the server arrives. */
final class ClientPlayer {
    private ClientPlayer() {}

    static Player get() {
        return Minecraft.getInstance().player;
    }
}
