package com.cyberfarm.energy;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.menu.TransmitterDishMenu;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.cyberfarm.port.net.IPayloadContext;

/** A new name typed in the Transmitter Dish's GUI, for the network of the menu the player has open. */
public record DishRenamePayload(int containerId, String name) implements CustomPacketPayload {
    public static final Type<DishRenamePayload> TYPE = new Type<>(CyberFarm.id("dish_rename"));
    public static final StreamCodec<ByteBuf, DishRenamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DishRenamePayload::containerId,
            ByteBufCodecs.stringUtf8(DishNetworks.MAX_NAME), DishRenamePayload::name,
            DishRenamePayload::new);

    @Override
    public Type<DishRenamePayload> type() {
        return TYPE;
    }

    public static void handle(DishRenamePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TransmitterDishMenu menu
                && menu.containerId == payload.containerId() && menu.stillValid(player)) {
            menu.rename(player, payload.name());
        }
    }
}
