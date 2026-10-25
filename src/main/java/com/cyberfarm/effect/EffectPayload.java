package com.cyberfarm.effect;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.EffectRenderer;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A visual effect to play over the blocks from {@code from} to {@code to}: see {@link Effects}. */
public record EffectPayload(int kind, BlockPos from, BlockPos to) implements CustomPacketPayload {
    public static final Type<EffectPayload> TYPE = new Type<>(CyberFarm.id("effect"));
    public static final StreamCodec<ByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EffectPayload::kind,
            BlockPos.STREAM_CODEC, EffectPayload::from,
            BlockPos.STREAM_CODEC, EffectPayload::to,
            EffectPayload::new);

    @Override
    public Type<EffectPayload> type() {
        return TYPE;
    }

    public static void handle(EffectPayload payload, IPayloadContext context) {
        EffectRenderer.add(payload.kind(), payload.from(), payload.to());
    }
}
