package com.cyberfarm.trinket;

/**
 * A program the Neural Chip runs, installed at the Trinket Table. {@code costPerTick}: FE drawn from the Power Belt
 * while it runs; Taxi Call and Auto-Feeder pay per use instead (see TrinketTicker).
 */
public enum ChipProgram {
    ZOOM("zoom", 0),
    NIGHT_VISION("night_vision", 2),
    MACHINE_LINK("machine_link", 1),
    GEIGER_HUD("geiger_hud", 1),
    TAXI_CALL("taxi_call", 0),
    AUTO_FEEDER("auto_feeder", 0);

    /** Programs one chip holds: all of them (the user's choice). */
    public static final int SLOTS = values().length;

    private final String name;
    private final int costPerTick;

    ChipProgram(String name, int costPerTick) {
        this.name = name;
        this.costPerTick = costPerTick;
    }

    public String getName() {
        return name;
    }

    public int costPerTick() {
        return costPerTick;
    }

    /** Zoom is only optics: it works with an empty sphere or none. The others run on FE, by the tick or per use. */
    public boolean needsPower() {
        return this != ZOOM;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String translationKey() {
        return "item.cyberfarm." + name + "_program";
    }
}
