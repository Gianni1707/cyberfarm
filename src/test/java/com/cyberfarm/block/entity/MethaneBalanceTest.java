package com.cyberfarm.block.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MethaneBalanceTest {
    @Test
    void oneZombieChamberRunsASteamEngineWithSomeToSpare() {
        int burn = SteamEngineBlockEntity.CYLINDERS * SteamEngineBlockEntity.FUEL_PER_CYLINDER;
        assertTrue(MethaneExtractorBlockEntity.ZOMBIE_RATE > burn, "zombie chamber must outpace the engine");
        assertTrue(MethaneExtractorBlockEntity.ZOMBIE_RATE <= burn * 1.25, "but only by a little");
    }
}
