package com.iso2t.heavyinventories.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Validated, immutable settings for the encumbrance effects. Thresholds are capacity percentages. */
public record EffectsSettings(boolean water, boolean lava, Exhaustion exhaustion, FallDamage fallDamage,
                              Swimming swimming, Sinking sinking, UpwardMovement upwardMovement, Knockback knockback) {

	public static final float MAX_PERCENT = 10_000;
	public static final float MAX_MULTIPLIER = 100;
	public static final EffectsSettings DEFAULT = new EffectsSettings(true, true,
			new Exhaustion(true, 1.5f, 0.01f), new FallDamage(true, 90, 125, 2),
			new Swimming(true, 90, 100, 0.5f), new Sinking(true, 90, 100, 2),
			new UpwardMovement(false, 100), new Knockback(true, 1000, 0.4f));
	private static final Gson JSON = new Gson();
	// Reuse file validation on the wire; bounded text also limits malformed client requests.
	public static final StreamCodec<FriendlyByteBuf, EffectsSettings> CODEC = StreamCodec.of(
			(buf, settings) -> buf.writeUtf(settings.toJson().toString(), 8192),
			buf -> parse(JsonParser.parseString(buf.readUtf(8192)).getAsJsonObject()));

	public EffectsSettings {
		if (exhaustion == null || fallDamage == null || swimming == null || sinking == null || upwardMovement == null || knockback == null)
			throw new IllegalArgumentException("Every encumbrance settings group must be specified");
	}

	public record Exhaustion(boolean enabled, float maxMultiplier, float walkingCostPerBlock) {
		public Exhaustion {
			range("exhaustion.maxMultiplier", maxMultiplier, 1, MAX_MULTIPLIER);
			range("exhaustion.walkingCostPerBlock", walkingCostPerBlock, 0, 100);
		}
	}

	public record FallDamage(boolean enabled, float startPercent, float fullPercent, float maxMultiplier) {
		public FallDamage {
			thresholds("fallDamage", startPercent, fullPercent);
			range("fallDamage.maxMultiplier", maxMultiplier, 1, MAX_MULTIPLIER);
		}
	}

	public record Swimming(boolean enabled, float startPercent, float fullPercent, float minMultiplier) {
		public Swimming {
			thresholds("swimming", startPercent, fullPercent);
			range("swimming.minMultiplier", minMultiplier, 0, 1);
		}
	}

	public record Sinking(boolean enabled, float startPercent, float fullPercent, float maxMultiplier) {
		public Sinking {
			thresholds("sinking", startPercent, fullPercent);
			range("sinking.maxMultiplier", maxMultiplier, 1, MAX_MULTIPLIER);
		}
	}

	public record UpwardMovement(boolean enabled, float thresholdPercent) {
		public UpwardMovement {
			range("upwardMovement.thresholdPercent", thresholdPercent, 0, MAX_PERCENT);
		}
	}

	public record Knockback(boolean enabled, float referenceWeight, float maxResistance) {
		public Knockback {
			range("knockback.referenceWeight", referenceWeight, Float.MIN_VALUE, ServerSettings.MAX_VALUE);
			range("knockback.maxResistance", maxResistance, 0, 1);
		}
	}

	private static void thresholds (String group, float start, float full) {
		range(group + ".startPercent", start, 0, MAX_PERCENT);
		range(group + ".fullPercent", full, 0, MAX_PERCENT);
		if (full <= start) throw new IllegalArgumentException(group + ".fullPercent must be greater than startPercent");
	}

	private static void range (String name, float value, float min, float max) {
		if (!Float.isFinite(value) || value < min || value > max)
			throw new IllegalArgumentException(name + " must be finite and between " + min + " and " + max);
	}

	public JsonObject toJson () {
		return JSON.toJsonTree(this).getAsJsonObject();
	}

	public static EffectsSettings parse (JsonObject json) {
		if (json == null) throw new IllegalArgumentException("effects must be a JSON object");
		var e = object(json, "exhaustion");
		var f = object(json, "fallDamage");
		var w = object(json, "swimming");
		var s = object(json, "sinking");
		var u = object(json, "upwardMovement");
		var k = object(json, "knockback");
		return new EffectsSettings(bool(json, "water", DEFAULT.water), bool(json, "lava", DEFAULT.lava),
				new Exhaustion(bool(e, "enabled", DEFAULT.exhaustion.enabled), number(e, "maxMultiplier", DEFAULT.exhaustion.maxMultiplier), number(e, "walkingCostPerBlock", DEFAULT.exhaustion.walkingCostPerBlock)),
				new FallDamage(bool(f, "enabled", DEFAULT.fallDamage.enabled), number(f, "startPercent", DEFAULT.fallDamage.startPercent), number(f, "fullPercent", DEFAULT.fallDamage.fullPercent), number(f, "maxMultiplier", DEFAULT.fallDamage.maxMultiplier)),
				new Swimming(bool(w, "enabled", DEFAULT.swimming.enabled), number(w, "startPercent", DEFAULT.swimming.startPercent), number(w, "fullPercent", DEFAULT.swimming.fullPercent), number(w, "minMultiplier", DEFAULT.swimming.minMultiplier)),
				new Sinking(bool(s, "enabled", DEFAULT.sinking.enabled), number(s, "startPercent", DEFAULT.sinking.startPercent), number(s, "fullPercent", DEFAULT.sinking.fullPercent), number(s, "maxMultiplier", DEFAULT.sinking.maxMultiplier)),
				new UpwardMovement(bool(u, "enabled", DEFAULT.upwardMovement.enabled), number(u, "thresholdPercent", DEFAULT.upwardMovement.thresholdPercent)),
				new Knockback(bool(k, "enabled", DEFAULT.knockback.enabled), number(k, "referenceWeight", DEFAULT.knockback.referenceWeight), number(k, "maxResistance", DEFAULT.knockback.maxResistance)));
	}

	static JsonObject object (JsonObject root, String key) {
		if (!root.has(key)) return new JsonObject();
		if (!root.get(key).isJsonObject()) throw new IllegalArgumentException(key + " must be a JSON object");
		return root.getAsJsonObject(key);
	}

	private static boolean bool (JsonObject root, String key, boolean fallback) {
		if (!root.has(key)) return fallback;
		var value = root.get(key);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException(key + " must be a JSON boolean");
		return value.getAsBoolean();
	}

	private static float number (JsonObject root, String key, float fallback) {
		if (!root.has(key)) return fallback;
		var value = root.get(key);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key + " must be a JSON number");
		return value.getAsFloat();
	}
}
