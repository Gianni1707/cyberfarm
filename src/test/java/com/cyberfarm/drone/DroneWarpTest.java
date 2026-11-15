package com.cyberfarm.drone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

class DroneWarpTest {
    /** A warp comes out short of the target, on the side it came from, at the height asked. */
    @Test
    void itComesOutShortOfTheTarget() {
        Vec3 out = DroneWarp.exit(new Vec3(0, 70, 0), new Vec3(500, 64, 0), 90);
        assertEquals(500 - DroneWarp.ARRIVE_SHORT, out.x, 1.0E-9);
        assertEquals(0.0, out.z, 1.0E-9);
        assertEquals(90.0, out.y, 1.0E-9);

        Vec3 back = DroneWarp.exit(new Vec3(0, 70, 400), new Vec3(0, 64, 0), 80);
        assertEquals(DroneWarp.ARRIVE_SHORT, back.z, 1.0E-9, "from the south it stops south of it");
    }

    /** Flown blocks count in full, warped ones at a third: by the distance, a little less. */
    @Test
    void aLongTripCostsLessThanFlyingIt() {
        assertEquals(100.0, DroneWarp.costDistance(100.0), 1.0E-9);
        assertEquals(DroneWarp.DISTANCE + 300.0, DroneWarp.costDistance(DroneWarp.DISTANCE + 900.0), 1.0E-9);
    }

    @Test
    void rightOverItItStaysThere() {
        Vec3 out = DroneWarp.exit(new Vec3(3, 70, 4), new Vec3(3, 64, 4), 70);
        assertEquals(new Vec3(3, 70, 4), out);
    }
}
