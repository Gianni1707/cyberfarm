package com.cyberfarm.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.transfer.transaction.Transaction;

class MachineEnergyHandlerTest {
    private final MachineEnergyHandler energy = new MachineEnergyHandler(1000, 100, 100, () -> {});

    @Test
    void generateStopsAtCapacity() {
        assertEquals(800, energy.generate(800));
        assertEquals(200, energy.generate(500));
        assertEquals(0, energy.generate(1));
        assertTrue(energy.isFull());
        assertEquals(1000, energy.takeGenerated());
        assertEquals(0, energy.takeGenerated());
    }

    @Test
    void consumeIsAllOrNothing() {
        energy.generate(50);
        assertFalse(energy.consume(60));
        assertEquals(50, energy.getEnergy());
        assertTrue(energy.consume(50));
        assertEquals(0, energy.getEnergy());
        assertEquals(50, energy.takeUsed());
    }

    @Test
    void drainTakesWhatIsThere() {
        energy.generate(30);
        assertEquals(30, energy.drain(100));
        assertEquals(0, energy.drain(100));
        assertEquals(30, energy.takeUsed());
    }

    /** Committed transfers by other blocks are counted, the machine's own changes and aborted transfers are not. */
    @Test
    void countsOnlyWhatOthersMoved() {
        energy.generate(500);
        try (Transaction transaction = Transaction.openRoot()) {
            energy.insert(80, transaction);
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            energy.extract(60, transaction);
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            energy.extract(90, transaction);
        }
        energy.set(100);
        assertEquals(80, energy.takeReceived());
        assertEquals(60, energy.takeSent());
        assertEquals(0, energy.takeReceived());
    }

    @Test
    void shrinkingCapacityDropsTheExtra() {
        energy.generate(900);
        energy.setCapacity(400);
        assertEquals(400, energy.getEnergy());
        assertEquals(400, energy.getCapacity());
    }
}
