package com.cyberfarm.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrinketTableRendererTest {
    /** Each key pose must bring the tool's tip where it is asked to be: onto the cradle, the tray, the weld. */
    @Test
    void theArmsReachWhatTheyAreSentTo() {
        float[][] targets = {{18.0F, 19.9F}, {12.5F, 18.0F}, {10.9F, 17.5F}, {12.1F, 21.0F}, {8.5F, 22.5F}};
        for (float[] target : targets) {
            float[][] p = TrinketTableRenderer.points(TrinketTableRenderer.ik(target[0], target[1]), 26.0F);
            assertEquals(target[0], p[3][0], 0.01F, "u of " + target[0] + ", " + target[1]);
            assertEquals(target[1], p[3][1], 0.01F, "y of " + target[0] + ", " + target[1]);
        }
    }
}
