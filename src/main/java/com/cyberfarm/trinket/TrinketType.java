package com.cyberfarm.trinket;

import com.cyberfarm.CyberFarm;

import net.minecraft.resources.Identifier;

/**
 * The trinket slots, top to bottom as they sit on the Trinkets screen. They are slots of their own: never armor, never a
 * cosmetic slot, and with Curios each is a Curios slot of its own too (data/cyberfarm/curios/slots).
 */
public enum TrinketType {
    NECK("neck"),
    BELT("belt"),
    FEET("feet");

    public static final int COUNT = values().length;

    private final String name;

    TrinketType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** The Curios slot identifier, from the file name under data/cyberfarm/curios/slots. */
    public String curiosSlot() {
        return "cyber_" + name;
    }

    /** The outline drawn in the empty slot. */
    public Identifier emptyIcon() {
        return CyberFarm.id("container/slot/trinket_" + name);
    }

    public String translationKey() {
        return "trinket.cyberfarm.slot." + name;
    }
}
