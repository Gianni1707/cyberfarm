package com.cyberfarm.village;

import net.minecraft.util.Mth;

import com.cyberfarm.port.net.PortCodecs;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.screen.MechanistScreen;
import com.cyberfarm.village.MechanistDialogue.Choice;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;

/** A talk with the Drone Mechanist: his name, his lines in order, and the buttons at the end. */
public record MechanistDialoguePayload(int entity, Component name, List<Component> lines, List<Choice> choices) implements CustomPacketPayload {
    public static final Type<MechanistDialoguePayload> TYPE = new Type<>(CyberFarm.id("mechanist_dialogue"));
    private static final StreamCodec<FriendlyByteBuf, Choice> CHOICE = ByteBufCodecs.VAR_INT.map(
            id -> Choice.values()[Mth.clamp(id, 0, Choice.values().length - 1)], Choice::ordinal).cast();
    public static final StreamCodec<FriendlyByteBuf, MechanistDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MechanistDialoguePayload::entity,
            PortCodecs.COMPONENT, MechanistDialoguePayload::name,
            PortCodecs.COMPONENT.apply(ByteBufCodecs.list()), MechanistDialoguePayload::lines,
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
