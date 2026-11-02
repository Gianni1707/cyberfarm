package com.cyberfarm.port;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** The 26.x save reader, reading straight from a CompoundTag. */
public final class ValueInput {
    private static final ValueInput EMPTY = new ValueInput(new CompoundTag());

    private final CompoundTag tag;

    private ValueInput(CompoundTag tag) {
        this.tag = tag;
    }

    public static ValueInput of(CompoundTag tag) {
        return new ValueInput(tag);
    }

    public CompoundTag tag() {
        return tag;
    }

    public Optional<ValueInput> child(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(tag.getCompound(key))) : Optional.empty();
    }

    public ValueInput childOrEmpty(String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? new ValueInput(tag.getCompound(key)) : EMPTY;
    }

    public int getIntOr(String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public long getLongOr(String key, long fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
    }

    public float getFloatOr(String key, float fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
    }

    public boolean getBooleanOr(String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }

    public String getStringOr(String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public Optional<String> getString(String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = tag.get(key);
        return value == null ? Optional.empty() : codec.parse(NbtOps.INSTANCE, value).result();
    }
}
