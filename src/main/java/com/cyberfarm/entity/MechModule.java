package com.cyberfarm.entity;

import java.util.function.Supplier;

import com.cyberfarm.registry.ModItems;

import net.minecraft.world.item.Item;

/**
 * The modules a mech can carry, one per mount: the jet on its back, the drill on its right arm,
 * the blade on its left arm, the armour plating on its trunk. Installed in a Mech Hangar.
 */
public enum MechModule {
    JET("jet", () -> ModItems.MECH_JET_MODULE.get()),
    DRILL("drill", () -> ModItems.MECH_DRILL_MODULE.get()),
    BLADE("blade", () -> ModItems.MECH_BLADE_MODULE.get()),
    ARMOR("armor", () -> ModItems.MECH_ARMOR_MODULE.get());

    private final String id;
    private final Supplier<Item> item;

    MechModule(String id, Supplier<Item> item) {
        this.id = id;
        this.item = item;
    }

    public String id() {
        return id;
    }

    public Item item() {
        return item.get();
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** Translation key of its short name on the cockpit display and in the hangar. */
    public String hudKey() {
        return "hud.cyberfarm.mech.module." + id;
    }
}
