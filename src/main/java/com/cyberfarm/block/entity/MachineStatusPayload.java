package com.cyberfarm.block.entity;

import com.cyberfarm.port.net.PortCodecs;

import com.cyberfarm.CyberFarm;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import com.cyberfarm.port.net.StreamCodec;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;

/** A machine's {@link MachineStatus}, sent each second it changes to the players who have its chunk loaded. */
public record MachineStatusPayload(BlockPos pos, MachineStatus status) implements CustomPacketPayload {
    public static final Type<MachineStatusPayload> TYPE = new Type<>(CyberFarm.id("machine_status"));
    public static final StreamCodec<ByteBuf, MachineStatusPayload> STREAM_CODEC = StreamCodec.composite(
            PortCodecs.BLOCK_POS, MachineStatusPayload::pos,
            MachineStatus.STREAM_CODEC, MachineStatusPayload::status,
            MachineStatusPayload::new);

    @Override
    public Type<MachineStatusPayload> type() {
        return TYPE;
    }

    public static void handle(MachineStatusPayload payload, IPayloadContext context) {
        if (context.player().level().getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine) {
            machine.status = payload.status();
            machine.statusTime = context.player().level().getGameTime();
        }
    }
}
