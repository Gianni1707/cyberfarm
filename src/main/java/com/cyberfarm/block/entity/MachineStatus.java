package com.cyberfarm.block.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a machine is doing, as its clients see it: whether it works, the FE it made and used per
 * tick over the last second, its buffer, and how far its current batch is (-1 when it has none).
 * Drives the ambient particles and the overlay shown when a player looks at it.
 */
public record MachineStatus(boolean working, float made, float used, int energy, int capacity, float progress) {
    public static final MachineStatus IDLE = new MachineStatus(false, 0.0F, 0.0F, 0, 0, -1.0F);
    public static final StreamCodec<ByteBuf, MachineStatus> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MachineStatus::working,
            ByteBufCodecs.FLOAT, MachineStatus::made,
            ByteBufCodecs.FLOAT, MachineStatus::used,
            ByteBufCodecs.VAR_INT, MachineStatus::energy,
            ByteBufCodecs.VAR_INT, MachineStatus::capacity,
            ByteBufCodecs.FLOAT, MachineStatus::progress,
            MachineStatus::new);
}
