package com.cyberfarm.effect;

import com.cyberfarm.port.net.PortCodecs;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.EffectRenderer;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;

/**
 * A visual effect to play over the blocks from {@code from} to {@code to}, coming from the entity
 * {@code source} if any (-1 for none): see {@link Effects}.
 */
public record EffectPayload(int kind, BlockPos from, BlockPos to, int source) implements CustomPacketPayload {
    public static final Type<EffectPayload> TYPE = new Type<>(CyberFarm.id("effect"));
    public static final StreamCodec<ByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EffectPayload::kind,
            PortCodecs.BLOCK_POS, EffectPayload::from,
            PortCodecs.BLOCK_POS, EffectPayload::to,
            ByteBufCodecs.VAR_INT, EffectPayload::source,
            EffectPayload::new);

    @Override
    public Type<EffectPayload> type() {
        return TYPE;
    }

    public static void handle(EffectPayload payload, IPayloadContext context) {
        EffectRenderer.add(payload.kind(), payload.from(), payload.to(), payload.source());
    }
}
