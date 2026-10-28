package com.cyberfarm;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("nuclear");
    }

    public static final ModConfigSpec.BooleanValue MELTDOWN_DESTROYS_BLOCKS = BUILDER
            .comment("Whether a reactor meltdown breaks blocks. When false the explosion only damages entities.")
            .define("meltdownDestroysBlocks", true);

    public static final ModConfigSpec.DoubleValue MELTDOWN_EXPLOSION_POWER = BUILDER
            .comment("Explosion power of a reactor meltdown (TNT = 4.0). Kept moderate on purpose: dangerous, not world-ending.")
            .defineInRange("meltdownExplosionPower", 6.0, 0.0, 20.0);

    public static final ModConfigSpec.BooleanValue RADIATION_WORLD_EFFECTS = BUILDER
            .comment("Whether contaminated land withers: grass dries to dirt, plants wilt, leaves fall.")
            .define("radiationWorldEffects", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("drones");
    }

    public static final ModConfigSpec.BooleanValue ANCHOR_ENABLED = BUILDER
            .comment("Whether a dock with an Anchor Addon drone keeps the chunks its drone works in loaded.")
            .define("anchorEnabled", true);

    public static final ModConfigSpec.IntValue ANCHOR_ENERGY_PER_CHUNK = BUILDER
            .comment("FE per tick a dock pays for each chunk it keeps loaded.")
            .defineInRange("anchorEnergyPerChunk", 4, 0, 1000);

    public static final ModConfigSpec.IntValue ANCHOR_MAX_CHUNKS = BUILDER
            .comment("Most chunks one dock keeps loaded: its own, around its drones, the route's ends, then the work area.")
            .defineInRange("anchorMaxChunks", 25, 1, 256);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("world");
    }

    public static final ModConfigSpec.BooleanValue BAUXITE_GENERATION = BUILDER
            .comment("Whether bauxite ore generates. Turn it off when another mod already adds an aluminum ore: our recipes take any c:ingots/aluminum, so their aluminum works.",
                    "Takes effect when a world is loaded.")
            .define("bauxiteGeneration", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    /** Datapack condition {@code cyberfarm:bauxite_generation}: keeps the bauxite biome modifier out when the option is off. */
    public record BauxiteGenerationCondition() implements ICondition {
        public static final BauxiteGenerationCondition INSTANCE = new BauxiteGenerationCondition();
        public static final MapCodec<BauxiteGenerationCondition> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            return BAUXITE_GENERATION.getAsBoolean();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
