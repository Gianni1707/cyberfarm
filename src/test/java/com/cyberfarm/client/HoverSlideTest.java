package com.cyberfarm.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HoverSlideTest {
    private static final double USUAL = 0.6 * 0.91;

    @Test
    void lettingGoDriftsALittleFurther() {
        double start = 0.2, vanilla = start * USUAL;
        double slid = TrinketClient.slide(start, vanilla);
        assertTrue(slid > vanilla && slid < start, "slows down, but less than on foot: " + slid);
    }

    @Test
    void walkingKeepsItsTopSpeed() {
        double push = 0.1;
        double top = push * USUAL / (1 - USUAL);
        double v = 0;
        for (int i = 0; i < 200; i++) {
            v = TrinketClient.slide(v, (v + push) * USUAL);
        }
        assertEquals(top, v, 1.0E-9);
    }

    @Test
    void aWallStopsIt() {
        assertEquals(0.0, TrinketClient.slide(0.2, 0.0));
    }
}
