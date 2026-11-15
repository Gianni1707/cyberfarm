package com.cyberfarm.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

// FluidStack needs bound registry components, which only a running server provides
@ExtendWith(EphemeralTestServerProvider.class)
class GasTankTest {
    private final GasTank tank = new GasTank(1000, resource -> true, () -> {});

    @BeforeAll
    static void startServer(MinecraftServer server) {
        // asking for the server is what makes the provider start it
    }

    @Test
    void fillStopsAtCapacityAndRefusesAnotherFluid() {
        assertEquals(1000, tank.fill(Fluids.WATER, 1500));
        assertEquals(1000, tank.amount());
        assertEquals(0, tank.fill(Fluids.LAVA, 10));
    }

    @Test
    void drainingToZeroEmptiesTheTank() {
        tank.fill(Fluids.WATER, 300);
        assertEquals(300, tank.drain(500));
        assertEquals(Fluids.EMPTY, tank.fluid());
        assertEquals(0, tank.fluidCode());
        // an empty tank takes any fluid again
        assertEquals(10, tank.fill(Fluids.LAVA, 10));
    }

    @Test
    void fluidCodeMatchesTheGui() {
        tank.fill(Fluids.WATER, 1);
        assertEquals(5, tank.fluidCode());
    }
}
