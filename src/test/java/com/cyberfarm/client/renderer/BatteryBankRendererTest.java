package com.cyberfarm.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class BatteryBankRendererTest {
    @Test
    void flowsFitTheScreenWhateverTheLanguage() {
        assertEquals("0", BatteryBankRenderer.compact(0));
        assertEquals("950", BatteryBankRenderer.compact(950));
        assertEquals("1.2k", BatteryBankRenderer.compact(1_234));
        assertEquals("12k", BatteryBankRenderer.compact(12_400));
        assertEquals("1.5M", BatteryBankRenderer.compact(1_500_000));
        assertEquals("2.5G", BatteryBankRenderer.compact(2_500_000_000L));
        assertEquals("12,400", BatteryBankRenderer.full(12_400));
    }

    /** Columns count towards the facing's clockwise side, whichever way the bank faces. */
    @Test
    void columnsRunTheSameWayRoundTheFront() {
        BlockPos west = new BlockPos(0, 0, 0), east = new BlockPos(1, 0, 0);
        Direction right = Direction.NORTH.getClockWise();
        assertEquals(1, BatteryBankRenderer.along(right, east) - BatteryBankRenderer.along(right, west));
        right = Direction.SOUTH.getClockWise();
        assertEquals(1, BatteryBankRenderer.along(right, west) - BatteryBankRenderer.along(right, east));
    }
}
