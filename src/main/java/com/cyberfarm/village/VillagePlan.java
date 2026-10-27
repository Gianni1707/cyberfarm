package com.cyberfarm.village;

import java.util.List;
import java.util.Optional;

import com.cyberfarm.CyberFarm;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The tech village's buildings and what each village level unlocks. Every building has three tiers,
 * each a schematic ({@code schematic/village/<building>_<tier>}, written by tools/villagegen.py)
 * holding every block of the tiers before it: projected over a built tier, it asks only for what is
 * new. The Outpost's first tier is the Mechanist's cabin, found in the world.
 */
public final class VillagePlan {
    public static final String OUTPOST = "outpost";
    /** Where the Supply Terminal stands in the Outpost's schematics: the cabin's back wall. */
    public static final BlockPos OUTPOST_TERMINAL = new BlockPos(6, 1, 9);
    private static final String FOLDER = "schematic/village/";

    /** One building at one tier: the blueprint on a disc. */
    public record Unlock(String building, int tier) {
        public Identifier schematic() {
            return CyberFarm.id(FOLDER + building + "_" + tier);
        }

        public Component displayName() {
            return Component.translatable("schematic.cyberfarm.village." + building + "_" + tier);
        }
    }

    /** What each village level unlocks, level 1 first: two blueprints a level. */
    public static final List<List<Unlock>> LEVELS = List.of(
            List.of(new Unlock("drone_workshop", 1), new Unlock("residence", 1)),
            List.of(new Unlock("power_station", 1), new Unlock("hydroponic_farm", 1)),
            List.of(new Unlock(OUTPOST, 2), new Unlock("market_hall", 1)),
            List.of(new Unlock("residence", 2), new Unlock("watch_tower", 1)),
            List.of(new Unlock("drone_workshop", 2), new Unlock("hydroponic_farm", 2)),
            List.of(new Unlock("power_station", 2), new Unlock("market_hall", 2)),
            List.of(new Unlock(OUTPOST, 3), new Unlock("watch_tower", 2)),
            List.of(new Unlock("residence", 3), new Unlock("hydroponic_farm", 3)),
            List.of(new Unlock("power_station", 3), new Unlock("market_hall", 3)),
            List.of(new Unlock("drone_workshop", 3), new Unlock("watch_tower", 3)));

    /** Buildings whose first tier gets a line of its own from the Mechanist when it is finished. */
    public static final List<String> SPECIAL_FIRSTS = List.of("power_station", "drone_workshop", "market_hall");

    private VillagePlan() {}

    public static boolean isVillage(Identifier id) {
        return id.getNamespace().equals(CyberFarm.MODID) && id.getPath().startsWith(FOLDER);
    }

    /** The building and tier a village schematic is for. */
    public static Optional<Unlock> unlock(Identifier id) {
        if (!isVillage(id)) {
            return Optional.empty();
        }
        String name = id.getPath().substring(FOLDER.length());
        int split = name.lastIndexOf('_');
        if (split <= 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Unlock(name.substring(0, split), Integer.parseInt(name.substring(split + 1))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
