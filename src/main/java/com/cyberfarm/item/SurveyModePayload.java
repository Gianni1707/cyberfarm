package com.cyberfarm.item;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sneak + mouse wheel with the Survey Tool in hand: the client asks for the next (1) or previous (-1) mode. */
public record SurveyModePayload(int step) implements CustomPacketPayload {
    public static final Type<SurveyModePayload> TYPE = new Type<>(CyberFarm.id("survey_mode"));
    public static final StreamCodec<ByteBuf, SurveyModePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SurveyModePayload::new, SurveyModePayload::step);

    @Override
    public Type<SurveyModePayload> type() {
        return TYPE;
    }

    public static void handle(SurveyModePayload payload, IPayloadContext context) {
        ItemStack stack = context.player().getMainHandItem();
        if (stack.getItem() instanceof SurveyToolItem) {
            SurveyToolItem.cycleMode(context.player(), stack, Integer.signum(payload.step()));
        }
    }
}
