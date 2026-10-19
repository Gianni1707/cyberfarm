package com.cyberfarm.radiation;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Radiation of every contaminated chunk of a dimension, saved with the world. */
public class RadiationData extends SavedData {
    private record Entry(long chunk, float level) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("chunk").forGetter(Entry::chunk),
                Codec.FLOAT.fieldOf("level").forGetter(Entry::level)).apply(i, Entry::new));
    }

    public static final Codec<RadiationData> CODEC = Entry.CODEC.listOf().xmap(RadiationData::new, RadiationData::entries);
    public static final SavedDataType<RadiationData> TYPE = new SavedDataType<>(CyberFarm.id("radiation"), RadiationData::new, CODEC);

    /** Kept per tick of decay: the level falls by 2% plus a little, so small leaks fade within minutes. */
    private static final float DECAY_FACTOR = 0.98F;
    private static final float DECAY_FLOOR = 0.1F;

    private final Long2FloatOpenHashMap levels = new Long2FloatOpenHashMap();

    public RadiationData() {}

    private RadiationData(List<Entry> entries) {
        entries.forEach(entry -> levels.put(entry.chunk(), entry.level()));
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>(levels.size());
        for (Long2FloatMap.Entry entry : levels.long2FloatEntrySet()) {
            list.add(new Entry(entry.getLongKey(), entry.getFloatValue()));
        }
        return list;
    }

    public static RadiationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public float level(BlockPos pos) {
        return levels.get(ChunkPos.pack(pos));
    }

    public void add(BlockPos pos, float amount) {
        add(ChunkPos.pack(pos), amount);
    }

    public void add(long chunk, float amount) {
        if (amount > 0) {
            levels.addTo(chunk, amount);
            setDirty();
        }
    }

    /** The contaminated chunks, as packed {@link ChunkPos} longs with their level. */
    public Long2FloatOpenHashMap levels() {
        return levels;
    }

    public void decay() {
        if (levels.isEmpty()) {
            return;
        }
        var iterator = levels.long2FloatEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Long2FloatMap.Entry entry = iterator.next();
            float next = entry.getFloatValue() * DECAY_FACTOR - DECAY_FLOOR;
            if (next <= 0) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }
        setDirty();
    }
}
