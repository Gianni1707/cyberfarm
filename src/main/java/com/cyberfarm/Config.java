package com.cyberfarm;

import net.neoforged.neoforge.common.ModConfigSpec;

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

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
