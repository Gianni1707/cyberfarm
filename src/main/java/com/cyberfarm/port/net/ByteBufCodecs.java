package com.cyberfarm.port.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

/** The 26.x ByteBufCodecs the payloads use. */
public final class ByteBufCodecs {
    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(buf -> buf.readBoolean(), (buf, v) -> buf.writeBoolean(v));
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(buf -> buf.readFloat(), (buf, v) -> buf.writeFloat(v));
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(buf -> buf.readVarInt(), (buf, v) -> buf.writeVarInt(v));
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(buf -> buf.readInt(), (buf, v) -> buf.writeInt(v));
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(buf -> buf.readUtf(), (buf, v) -> buf.writeUtf(v));

    private ByteBufCodecs() {}

    public static <B, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list() {
        return element -> StreamCodec.of(buf -> {
            int size = buf.readVarInt();
            List<V> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(element.decode(buf));
            }
            return list;
        }, (buf, list) -> {
            buf.writeVarInt(list.size());
            for (V value : list) {
                element.encode(buf, value);
            }
        });
    }

    public static <V> StreamCodec<ByteBuf, V> idMapper(IdMap<V> map) {
        return StreamCodec.of(buf -> map.byIdOrThrow(buf.readVarInt()), (buf, v) -> buf.writeVarInt(map.getId(v)));
    }

    @SuppressWarnings("unchecked")
    public static <V> StreamCodec<ByteBuf, V> registry(ResourceKey<? extends Registry<V>> key) {
        Registry<V> registry = (Registry<V>) BuiltInRegistries.REGISTRY.get(key.location());
        return idMapper(registry);
    }
}
