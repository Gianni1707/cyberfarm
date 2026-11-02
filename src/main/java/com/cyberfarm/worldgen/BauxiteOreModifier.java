package com.cyberfarm.worldgen;

import com.cyberfarm.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Adds the bauxite ore unless the config turns it off. Forge 1.20.1 biome modifiers have no load
 * conditions, so the option is read here, when the world loads (the 26.x condition did the same).
 */
public record BauxiteOreModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features, GenerationStep.Decoration step) implements BiomeModifier {
    public static final Codec<BauxiteOreModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BauxiteOreModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(BauxiteOreModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(BauxiteOreModifier::step))
            .apply(i, BauxiteOreModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && Config.BAUXITE_GENERATION.get() && biomes.contains(biome)) {
            features.forEach(feature -> builder.getGenerationSettings().addFeature(step, feature));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
