package com.cyberfarm.port.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The vanilla STREAM_CODEC fields of 26.x (BlockPos.STREAM_CODEC and so on), which 1.20.1 lacks. */
public final class PortCodecs {
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(buf -> buf.readBlockPos(), (buf, v) -> buf.writeBlockPos(v));
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(buf -> buf.readResourceLocation(), (buf, v) -> buf.writeResourceLocation(v));
    public static final StreamCodec<ByteBuf, Component> COMPONENT = StreamCodec.of(buf -> buf.readComponent(), (buf, v) -> buf.writeComponent(v));

    private PortCodecs() {}
}
