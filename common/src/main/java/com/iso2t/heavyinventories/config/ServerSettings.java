package com.iso2t.heavyinventories.config;

import com.google.gson.JsonObject;

/** Validated gameplay settings, owned by a running server rather than a physical side. */
public record ServerSettings(float startingWeight) {
    public static final float MAX_VALUE = 1_000_000_000f;
    public static final ServerSettings DEFAULT = new ServerSettings(1000f);

    public ServerSettings {
        if (!Float.isFinite(startingWeight) || startingWeight <= 0 || startingWeight > MAX_VALUE)
            throw new IllegalArgumentException("startingWeight must be finite, greater than 0, and at most " + MAX_VALUE);
    }

    public static ServerSettings parse(JsonObject json) {
        if (json == null) throw new IllegalArgumentException("Server config must be a JSON object");
        if (!json.has("startingWeight")) return DEFAULT;
        var value = json.get("startingWeight");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("startingWeight must be a JSON number");
        return new ServerSettings(value.getAsFloat());
    }

    public static float validateItemWeight(float value) {
        if (!Float.isFinite(value) || value < 0 || value > MAX_VALUE)
            throw new IllegalArgumentException("Item weight must be finite and between 0 and " + MAX_VALUE);
        return value;
    }
}
