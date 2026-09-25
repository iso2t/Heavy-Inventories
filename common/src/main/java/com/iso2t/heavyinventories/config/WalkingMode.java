package com.iso2t.heavyinventories.config;

/** Stable configuration IDs, independent of display text and the user's locale. */
public enum WalkingMode {
    PROGRESSIVE("progressive"),
    AT_NINETY_PERCENT("at_ninety_percent");

    private final String id;
    WalkingMode(String id) { this.id = id; }
    public String id() { return id; }

    public static WalkingMode parse(String id) {
        for (var mode : values()) if (mode.id.equals(id)) return mode;
        throw new IllegalArgumentException("walkingMode must be progressive or at_ninety_percent");
    }
}
