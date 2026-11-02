package com.cyberfarm;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only settings: sounds are played by the client. What each Muffler silences is chosen in
 * its GUI; its range and the list behind "Other machines" are set here.
 */
public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.push("muffler");
    }

    public static final ForgeConfigSpec.IntValue MUFFLER_RANGE = BUILDER
            .comment("Distance in blocks within which a Muffler silences sounds.")
            .defineInRange("range", 10, 1, 64);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_SOUNDS = BUILDER
            .comment("Sounds silenced by Mufflers with \"Other machines\" turned on, e.g. machines of other mods. Use a",
                    "mod id (\"mekanism\") for all of a mod's sounds, or a sound id (\"minecraft:block.furnace.fire_crackle\").")
            .defineListAllowEmpty("extraSounds", List.of(
                    "minecraft:block.furnace.fire_crackle",
                    "minecraft:block.blastfurnace.fire_crackle",
                    "minecraft:block.smoker.smoke"),
                    () -> "minecraft:", entry -> entry instanceof String);

    static {
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    /** Whether {@code sound} is listed in {@link #EXTRA_SOUNDS}, as itself or through its mod id. */
    public static boolean isExtraMuted(ResourceLocation sound) {
        for (String entry : EXTRA_SOUNDS.get()) {
            if (entry.contains(":") ? entry.equals(sound.toString()) : entry.equals(sound.getNamespace())) {
                return true;
            }
        }
        return false;
    }
}
