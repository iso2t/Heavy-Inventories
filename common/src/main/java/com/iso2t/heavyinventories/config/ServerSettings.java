package com.iso2t.heavyinventories.config;

import com.google.gson.JsonObject;

/**
 * Validated gameplay settings, owned by a running server rather than a physical side.
 */
public record ServerSettings(float startingWeight, WalkingMode walkingMode, EffectsSettings effects) {

	public static final float          MAX_VALUE = 1_000_000_000f;
	public static final ServerSettings DEFAULT   = new ServerSettings(1000f);

	public ServerSettings (float startingWeight) {
		this(startingWeight, WalkingMode.PROGRESSIVE);
	}

	public ServerSettings (float startingWeight, WalkingMode walkingMode) {
		this(startingWeight, walkingMode, EffectsSettings.DEFAULT);
	}

	public ServerSettings {
		if (effects == null) throw new IllegalArgumentException("effects must be specified");
		if (walkingMode == null) throw new IllegalArgumentException("walkingMode must be specified");
		if (!Float.isFinite(startingWeight) || startingWeight <= 0 || startingWeight > MAX_VALUE) throw new IllegalArgumentException("startingWeight must be finite, greater than 0, and at most " + MAX_VALUE);
	}

	public static ServerSettings parse (JsonObject json) {
		if (json == null) throw new IllegalArgumentException("Server config must be a JSON object");
		float startingWeight = DEFAULT.startingWeight();
		if (json.has("startingWeight")) {
			var value = json.get("startingWeight");
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("startingWeight must be a JSON number");
			startingWeight = value.getAsFloat();
		}
		var mode = DEFAULT.walkingMode();
		if (json.has("walkingMode")) {
			var value = json.get("walkingMode");
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("walkingMode must be a JSON string");
			mode = WalkingMode.parse(value.getAsString());
		}
		return new ServerSettings(startingWeight, mode, EffectsSettings.parse(EffectsSettings.object(json, "effects")));
	}

	public JsonObject toJson () {
		var root = new JsonObject();
		root.addProperty("startingWeight", startingWeight);
		root.addProperty("walkingMode", walkingMode.id());
		root.add("effects", effects.toJson());
		return root;
	}

	public static float validateItemWeight (float value) {
		if (!Float.isFinite(value) || value < 0 || value > MAX_VALUE) throw new IllegalArgumentException("Item weight must be finite and between 0 and " + MAX_VALUE);
		return value;
	}
}
