package com.cyberfarm.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class CableSwapperItemTest {
    private static Identifier id(String full) {
        return Identifier.parse(full);
    }

    /** CyberFarm's base cable has no tier word: next is advanced, then elite, then nothing. */
    @Test
    void ourCablesClimbTheirTiers() {
        Set<Identifier> blocks = Set.of(id("cyberfarm:energy_conduit"), id("cyberfarm:advanced_energy_conduit"), id("cyberfarm:elite_energy_conduit"));
        assertEquals(id("cyberfarm:advanced_energy_conduit"), CableSwapperItem.nextTier(id("cyberfarm:energy_conduit"), blocks::contains));
        assertEquals(id("cyberfarm:elite_energy_conduit"), CableSwapperItem.nextTier(id("cyberfarm:advanced_energy_conduit"), blocks::contains));
        assertNull(CableSwapperItem.nextTier(id("cyberfarm:elite_energy_conduit"), blocks::contains));
    }

    /** Another mod's tiers, in the same mod only, skipping a tier it does not have. */
    @Test
    void otherModsTiersStayInTheirMod() {
        Set<Identifier> blocks = Set.of(id("mekanism:basic_universal_cable"), id("mekanism:advanced_universal_cable"),
                id("mekanism:ultimate_universal_cable"), id("othermod:elite_universal_cable"));
        assertEquals(id("mekanism:advanced_universal_cable"), CableSwapperItem.nextTier(id("mekanism:basic_universal_cable"), blocks::contains));
        assertEquals(id("mekanism:ultimate_universal_cable"), CableSwapperItem.nextTier(id("mekanism:advanced_universal_cable"), blocks::contains));
    }

    @Test
    void onlyCablesAndPipes() {
        assertTrue(CableSwapperItem.isConduit(id("mekanism:basic_mechanical_pipe")));
        assertTrue(CableSwapperItem.isConduit(id("cyberfarm:gas_pipe")));
        assertTrue(CableSwapperItem.isConduit(id("mekanism:basic_pressurized_tube")));
        assertFalse(CableSwapperItem.isConduit(id("mekanism:basic_energy_cube")));
    }
}
