package com.cyberfarm.multiblock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MultiblockTest {
    /** Ports only where the drawn tower or hall has a flat side: the middle block of a 3- or 5-wide ring, the middle three of a 7- or 9-wide one. */
    @Test
    void portsOnlyInTheMiddleOfTheSides() {
        assertTrue(Multiblock.onFlatSide(1, 0));
        assertFalse(Multiblock.onFlatSide(1, 1));
        assertTrue(Multiblock.onFlatSide(2, 0));
        assertFalse(Multiblock.onFlatSide(2, 1));
        assertTrue(Multiblock.onFlatSide(-3, 1));
        assertFalse(Multiblock.onFlatSide(3, -2));
        assertTrue(Multiblock.onFlatSide(1, 4));
        assertFalse(Multiblock.onFlatSide(4, 2));
        assertFalse(Multiblock.onFlatSide(4, 4));
    }
}
