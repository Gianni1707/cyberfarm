package com.cyberfarm.village;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.screen.MechanistScreen;
import com.cyberfarm.village.MechanistDialogue.Choice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A talk with the Drone Mechanist: his name, his lines in order, and the buttons at the end. */
public record MechanistDialoguePayload(int entity, Component name, List<Component> lines, List<Choice> choices) implements CustomPacketPayload {
    public static final Type<MechanistDialoguePayload> TYPE = new Type<>(CyberFarm.id("mechanist_dialogue"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Choice> CHOICE = ByteBufCodecs.VAR_INT.map(
            id -> Choice.values()[Math.clamp(id, 0, Choice.values().length - 1)], Choice::ordinal).cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, MechanistDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MechanistDialoguePayload::entity,
            ComponentSerialization.STREAM_CODEC, MechanistDialoguePayload::name,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), MechanistDialoguePayload::lines,
            CHOICE.apply(ByteBufCodecs.list()), MechanistDialoguePayload::choices,
            MechanistDialoguePayload::new);

    @Override
    public Type<MechanistDialoguePayload> type() {
        return TYPE;
    }

    public static void handle(MechanistDialoguePayload payload, IPayloadContext context) {
        MechanistScreen.open(payload);
    }
}
