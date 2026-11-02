package com.cyberfarm.port;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * A 26.x item data component on 1.20.1: a value kept in the stack's NBT under {@code key}. Ints
 * (energy, module masks) skip the codec so tooltips and charge bars read them without allocating.
 */
public final class DataComponentType<T> {
    private final String key;
    private final Codec<T> codec;
    private final boolean intValued;

    private DataComponentType(String key, Codec<T> codec, boolean intValued) {
        this.key = key;
        this.codec = codec;
        this.intValued = intValued;
    }

    public static <T> DataComponentType<T> of(String key, Codec<T> codec) {
        return new DataComponentType<>(key, codec, false);
    }

    public static DataComponentType<Integer> ofInt(String key) {
        return new DataComponentType<>(key, Codec.INT, true);
    }

    /** Itself: lets the 26.x {@code ModDataComponents.X.get()} calls stay as they are. */
    public DataComponentType<T> get() {
        return this;
    }

    public boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key);
    }

    @SuppressWarnings("unchecked")
    public @Nullable T get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return null;
        }
        if (intValued) {
            return (T) Integer.valueOf(tag.getInt(key));
        }
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
    }

    public T getOrDefault(ItemStack stack, T fallback) {
        T value = get(stack);
        return value != null ? value : fallback;
    }

    public void set(ItemStack stack, @Nullable T value) {
        if (value == null) {
            remove(stack);
        } else if (intValued) {
            stack.getOrCreateTag().putInt(key, (Integer) value);
        } else {
            Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
            if (encoded != null) {
                stack.getOrCreateTag().put(key, encoded);
            }
        }
    }

    public void remove(ItemStack stack) {
        stack.removeTagKey(key);
    }
}
