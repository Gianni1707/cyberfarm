package com.cyberfarm.village;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.village.MechanistDialogue.Choice;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import com.cyberfarm.port.net.IPayloadContext;

/** The player pressed one of the buttons at the end of a talk with the Mechanist. */
public record MechanistChoicePayload(int entity, int choice) implements CustomPacketPayload {
    public static final Type<MechanistChoicePayload> TYPE = new Type<>(CyberFarm.id("mechanist_choice"));
    public static final StreamCodec<ByteBuf, MechanistChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MechanistChoicePayload::entity,
            ByteBufCodecs.VAR_INT, MechanistChoicePayload::choice,
            MechanistChoicePayload::new);

    @Override
    public Type<MechanistChoicePayload> type() {
        return TYPE;
    }

    public static void handle(MechanistChoicePayload payload, IPayloadContext context) {
        Choice[] choices = Choice.values();
        if (context.player() instanceof ServerPlayer player && payload.choice() >= 0 && payload.choice() < choices.length) {
            MechanistDialogue.choose(player, payload.entity(), choices[payload.choice()]);
        }
    }
}
