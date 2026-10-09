package com.iso2t.heavyinventories.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Validated, immutable settings for the encumbrance effects.
 */
public record EffectsSettings(boolean water, boolean lava, Exhaustion exhaustion, FallDamage fallDamage, Swimming swimming, Sinking sinking, UpwardMovement upwardMovement, Knockback knockback, Elytra elytra) {

	public static final  float                                         MAX_PERCENT    = 10_000;
	public static final  float                                         MAX_MULTIPLIER = 100;
	public static final  EffectsSettings                               DEFAULT        = new EffectsSettings(true, true, new Exhaustion(true, 1.5f, 0.01f), new FallDamage(true, 90, 125, 2), new Swimming(true, 90, 100, 0.5f), new Sinking(true, 90, 100, 2), new UpwardMovement(false, 100), new Knockback(true, 1000, 0.4f), new Elytra(true, 1000, 0.15f, 0.25f));
	private static final Gson                                          JSON           = new Gson();
	// Reuse file validation on the wire; bounded text also limits malformed client requests.
	public static final  StreamCodec<FriendlyByteBuf, EffectsSettings> CODEC          = StreamCodec.of((buf, settings) -> buf.writeUtf(settings.toJson().toString(), 8192), buf -> parse(JsonParser.parseString(buf.readUtf(8192)).getAsJsonObject()));

	public EffectsSettings {
		if (exhaustion == null || fallDamage == null || swimming == null || sinking == null || upwardMovement == null || knockback == null || elytra == null)
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

	public record Elytra(boolean enabled, float referenceWeight, float maxLiftReduction, float maxRocketReduction) {

		public Elytra {
			range("elytra.referenceWeight", referenceWeight, Float.MIN_VALUE, ServerSettings.MAX_VALUE);
			range("elytra.maxLiftReduction", maxLiftReduction, 0, 1);
			range("elytra.maxRocketReduction", maxRocketReduction, 0, 1);
		}

	}

	private static void thresholds (String group, float start, float full) {
		range(group + ".startPercent", start, 0, MAX_PERCENT);
		range(group + ".fullPercent", full, 0, MAX_PERCENT);
		if (full <= start) throw new IllegalArgumentException(group + ".fullPercent must be greater than startPercent");
	}

	private static void range (String name, float value, float min, float max) {
		if (!Float.isFinite(value) || value < min || value > max) throw new IllegalArgumentException(name + " must be finite and between " + min + " and " + max);
	}

	public JsonObject toJson () {
		return JSON.toJsonTree(this).getAsJsonObject();
	}

	public static EffectsSettings parse (JsonObject json) {
		if (json == null) throw new IllegalArgumentException("effects must be a JSON object");
		var exhaustion = object(json, "exhaustion");
		var fallDamage = object(json, "fallDamage");
		var swimming = object(json, "swimming");
		var sinking = object(json, "sinking");
		var upwardMovement = object(json, "upwardMovement");
		var knockback = object(json, "knockback");
		var elytra = object(json, "elytra");
		return new EffectsSettings(bool(json, "water", DEFAULT.water), bool(json, "lava", DEFAULT.lava), new Exhaustion(bool(exhaustion, "enabled", DEFAULT.exhaustion.enabled), number(exhaustion, "maxMultiplier", DEFAULT.exhaustion.maxMultiplier), number(exhaustion, "walkingCostPerBlock", DEFAULT.exhaustion.walkingCostPerBlock)), new FallDamage(bool(fallDamage, "enabled", DEFAULT.fallDamage.enabled), number(fallDamage, "startPercent", DEFAULT.fallDamage.startPercent), number(fallDamage, "fullPercent", DEFAULT.fallDamage.fullPercent), number(fallDamage, "maxMultiplier", DEFAULT.fallDamage.maxMultiplier)), new Swimming(bool(swimming, "enabled", DEFAULT.swimming.enabled), number(swimming, "startPercent", DEFAULT.swimming.startPercent), number(swimming, "fullPercent", DEFAULT.swimming.fullPercent), number(swimming, "minMultiplier", DEFAULT.swimming.minMultiplier)), new Sinking(bool(sinking, "enabled", DEFAULT.sinking.enabled), number(sinking, "startPercent", DEFAULT.sinking.startPercent), number(sinking, "fullPercent", DEFAULT.sinking.fullPercent), number(sinking, "maxMultiplier", DEFAULT.sinking.maxMultiplier)), new UpwardMovement(bool(upwardMovement, "enabled", DEFAULT.upwardMovement.enabled), number(upwardMovement, "thresholdPercent", DEFAULT.upwardMovement.thresholdPercent)), new Knockback(bool(knockback, "enabled", DEFAULT.knockback.enabled), number(knockback, "referenceWeight", DEFAULT.knockback.referenceWeight), number(knockback, "maxResistance", DEFAULT.knockback.maxResistance)), new Elytra(bool(elytra, "enabled", DEFAULT.elytra.enabled), number(elytra, "referenceWeight", DEFAULT.elytra.referenceWeight), number(elytra, "maxLiftReduction", DEFAULT.elytra.maxLiftReduction), number(elytra, "maxRocketReduction", DEFAULT.elytra.maxRocketReduction)));
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
