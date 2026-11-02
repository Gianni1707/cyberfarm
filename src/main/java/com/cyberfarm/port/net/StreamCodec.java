package com.cyberfarm.port.net;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function6;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The 26.x StreamCodec on 1.20.1: reads and writes a value on a FriendlyByteBuf. {@code B} only keeps
 * the 26.x declarations compiling; every buffer here is a FriendlyByteBuf.
 */
public interface StreamCodec<B, V> {
    V decode(FriendlyByteBuf buf);

    void encode(FriendlyByteBuf buf, V value);

    static <B, V> StreamCodec<B, V> of(Function<FriendlyByteBuf, V> decoder, java.util.function.BiConsumer<FriendlyByteBuf, V> encoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(FriendlyByteBuf buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(FriendlyByteBuf buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        StreamCodec<B, V> self = this;
        return of(buf -> to.apply(self.decode(buf)), (buf, value) -> self.encode(buf, from.apply(value)));
    }

    default <O> StreamCodec<B, O> apply(Function<StreamCodec<B, V>, StreamCodec<B, O>> operation) {
        return operation.apply(this);
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of(buf -> value, (buf, v) -> {});
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            BiFunction<T1, T2, C> factory) {
        return of(buf -> factory.apply(c1.decode(buf), c2.decode(buf)), (buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
        });
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            Function3<T1, T2, T3, C> factory) {
        return of(buf -> factory.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf)), (buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
        });
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            Function4<T1, T2, T3, T4, C> factory) {
        return of(buf -> factory.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf)), (buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1,
            StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3,
            StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5,
            StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            Function6<T1, T2, T3, T4, T5, T6, C> factory) {
        return of(buf -> factory.apply(c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf), c5.decode(buf), c6.decode(buf)), (buf, v) -> {
            c1.encode(buf, g1.apply(v));
            c2.encode(buf, g2.apply(v));
            c3.encode(buf, g3.apply(v));
            c4.encode(buf, g4.apply(v));
            c5.encode(buf, g5.apply(v));
            c6.encode(buf, g6.apply(v));
        });
    }
}
