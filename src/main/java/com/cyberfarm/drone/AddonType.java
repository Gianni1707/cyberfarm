package com.cyberfarm.drone;

/**
 * Drone addons. Any number of each fits within the frame's addon limit, except for the ones with
 * their own maximum. Each addon also makes the drone use more power: a percentage on the flight
 * cost, or a percentage on the energy of every job (harvest, mined block).
 */
public enum AddonType {
    /** +10% flight speed each. */
    SPEED(Integer.MAX_VALUE, 10, 0),
    /** +1 stack (64 items) carried per Transport trip each. */
    STACK(Integer.MAX_VALUE, 5, 0),
    /** +25% of the frame's battery capacity each. */
    BATTERY(Integer.MAX_VALUE, 5, 0),
    /** Fortune level for Quarry and Harvester drops. */
    FORTUNE(3, 0, 25),
    /** Silk Touch for Quarry and Harvester drops; cannot be combined with Fortune. */
    SILK_TOUCH(1, 0, 50),
    /** Its dock keeps the chunks the drone works in loaded, paying FE per chunk (see the config). */
    ANCHOR(1, 0, 0);

    private final int maxPerDrone;
    private final int flightCostPercent;
    private final int jobCostPercent;

    AddonType(int maxPerDrone, int flightCostPercent, int jobCostPercent) {
        this.maxPerDrone = maxPerDrone;
        this.flightCostPercent = flightCostPercent;
        this.jobCostPercent = jobCostPercent;
    }

    public int getMaxPerDrone() {
        return maxPerDrone;
    }

    /** Extra flight cost, in percent, for each addon of this type. */
    public int getFlightCostPercent() {
        return flightCostPercent;
    }

    /** Extra energy per job (harvest, mined block), in percent, for each addon of this type. */
    public int getJobCostPercent() {
        return jobCostPercent;
    }

    /** Fortune and Silk Touch exclude each other, like the vanilla enchantments. */
    public boolean conflictsWith(AddonType other) {
        return (this == FORTUNE && other == SILK_TOUCH) || (this == SILK_TOUCH && other == FORTUNE);
    }
}
