package com.cyberfarm.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

class DishNetworksTest {
    private static DishNetworks.Network read(String extra) {
        JsonObject json = JsonParser.parseString("""
                {"id": [1, 2, 3, 4], "name": "Net", "colour": 0, "owner": [5, 6, 7, 8], "owner_name": "Steve",
                 "transmitter": {"dimension": "minecraft:overworld", "pos": [0, 64, 0]}, "receivers": []%s}
                """.formatted(extra)).getAsJsonObject();
        return DishNetworks.Network.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    /** Saved before the Team mode: a private network was the owner's team's, a public one stays public. */
    @Test
    void oldSavesKeepWhoCouldUseThem() {
        assertEquals(DishNetworks.Network.TEAM, read(", \"public\": false").access);
        assertEquals(DishNetworks.Network.PUBLIC, read(", \"public\": true").access);
        assertEquals(DishNetworks.Network.PRIVATE, read(", \"public\": false, \"access\": 0").access);
    }
}
