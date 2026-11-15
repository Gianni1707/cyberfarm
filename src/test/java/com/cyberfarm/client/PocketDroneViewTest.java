package com.cyberfarm.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cyberfarm.trinket.DroneFetch;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

class PocketDroneViewTest {
    private static final Vec3 HOME = new Vec3(0, 2, 0), DROP = new Vec3(4, 0, 0);

    /** Out from home to just over the drop, back the other way, arcing up in between: no jump at either end. */
    @Test
    void theDroneFliesAContinuousArc() {
        DroneFetch out = new DroneFetch(DroneFetch.OUT, DROP, 7, 100, 10, ItemStack.EMPTY);
        assertEquals(HOME, PocketDroneView.position(out, HOME, 100));
        Vec3 over = PocketDroneView.position(out, HOME, 110);
        assertEquals(4.0, over.x, 1.0E-6);
        assertEquals(0.45, over.y, 1.0E-6);
        Vec3 middle = PocketDroneView.position(out, HOME, 105);
        assertTrue(middle.y > (HOME.y + over.y) / 2, "arcs above the straight line");

        DroneFetch back = new DroneFetch(DroneFetch.BACK, DROP, -1, 110, 10, ItemStack.EMPTY);
        assertEquals(over, PocketDroneView.position(back, HOME, 110));
        assertEquals(HOME.x, PocketDroneView.position(back, HOME, 125).x, 1.0E-6);
    }

    @Test
    void openingEasesFromRestToDone() {
        assertEquals(0.0F, PocketDroneView.smooth(-1.0F));
        assertEquals(0.5F, PocketDroneView.smooth(0.5F), 1.0E-6F);
        assertEquals(1.0F, PocketDroneView.smooth(2.0F));
    }

    @Test
    void idleItWaitsAtHome() {
        assertEquals(HOME, PocketDroneView.position(DroneFetch.NONE, HOME, 50));
    }
}
