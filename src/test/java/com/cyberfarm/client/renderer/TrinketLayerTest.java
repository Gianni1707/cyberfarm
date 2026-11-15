package com.cyberfarm.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrinketLayerTest {
    /** The sphere's glass is a ball of little cubes: full in the middle, cut at the corners, the same from every side. */
    @Test
    void theOrbIsRoundAndSymmetric() {
        assertTrue(TrinketLayer.inOrb(2, 2, 2));
        assertTrue(TrinketLayer.inOrb(0, 2, 3), "the middle of a face");
        assertFalse(TrinketLayer.inOrb(0, 0, 0), "a corner");
        assertFalse(TrinketLayer.inOrb(-1, 2, 2), "outside the grid");
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 6; y++) {
                for (int z = 0; z < 6; z++) {
                    assertEquals(TrinketLayer.inOrb(x, y, z), TrinketLayer.inOrb(5 - x, y, z));
                    assertEquals(TrinketLayer.inOrb(x, y, z), TrinketLayer.inOrb(y, z, x));
                }
            }
        }
    }
}
