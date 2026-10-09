package com.iso2t.heavyinventories.config;

import com.iso2t.easyconfig.api.Side;
import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.annotations.Config;
import com.iso2t.easyconfig.api.annotations.Ignore;
import com.iso2t.easyconfig.api.annotations.Translation;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.EnumValue;
import com.iso2t.easyconfig.api.value.wrappers.FloatValue;
import lombok.NoArgsConstructor;

@Config(name = "heavyinventories", side = Side.SERVER)
@NoArgsConstructor
public final class ServerConfig {

	@Comment("Base capacity in stored pounds, before Strength and equipment bonuses.")
	@Translation(value = "option.heavyinventories.starting_max_weight", tooltip = "option.heavyinventories.starting_max_weight.tooltip")
	public final FloatValue startingWeight = FloatValue.of(ServerSettings.DEFAULT.startingWeight(), Float.MIN_VALUE, ServerSettings.MAX_VALUE);

	@Comment("Progressive slows walking as weight increases. The 90% mode keeps full speed until 90%, then decreases to zero at capacity. Surefooted applies in both modes.")
	@Translation(value = "option.heavyinventories.walking_mode", tooltip = "option.heavyinventories.walking_mode.tooltip", valuePrefix = "option.heavyinventories.walking_mode.")
	public final EnumValue<WalkingMode> walkingMode = EnumValue.of(ServerSettings.DEFAULT.walkingMode());

	@Comment("Encumbrance effects")
	@Translation("category.heavyinventories.effects")
	public Effects effects = new Effects();

	@Ignore
	public long revision;

	public ServerConfig (ServerSettings settings, long revision) {
		startingWeight.set(settings.startingWeight());
		walkingMode.set(settings.walkingMode());
		effects = new Effects(settings.effects());
		this.revision = revision;
	}

	public ServerSettings settings () {
		return new ServerSettings(startingWeight.get(), walkingMode.get(), effects.settings());
	}

	@NoArgsConstructor
	public static final class Effects {

		@Comment("Apply swimming, sinking and upward-movement restrictions in water.")
		@Translation(value = "option.heavyinventories.effects.fluids.water", tooltip = "option.heavyinventories.effects.fluids.water.tooltip")
		public final BooleanValue water = BooleanValue.of(EffectsSettings.DEFAULT.water());

		@Comment("Apply swimming, sinking and upward-movement restrictions in lava.")
		@Translation(value = "option.heavyinventories.effects.fluids.lava", tooltip = "option.heavyinventories.effects.fluids.lava.tooltip")
		public final BooleanValue lava = BooleanValue.of(EffectsSettings.DEFAULT.lava());

		@Comment("Exhaustion")
		@Translation("category.heavyinventories.effects.exhaustion")
		public Exhaustion exhaustion = new Exhaustion();

		@Comment("Fall damage")
		@Translation("category.heavyinventories.effects.fallDamage")
		public FallDamage fallDamage = new FallDamage();

		@Comment("Swimming")
		@Translation("category.heavyinventories.effects.swimming")
		public Swimming swimming = new Swimming();

		@Comment("Sinking")
		@Translation("category.heavyinventories.effects.sinking")
		public Sinking sinking = new Sinking();

		@Comment("Upward swimming")
		@Translation("category.heavyinventories.effects.upwardMovement")
		public UpwardMovement upwardMovement = new UpwardMovement();

		@Comment("Knockback resistance")
		@Translation("category.heavyinventories.effects.knockback")
		public Knockback knockback = new Knockback();

		@Comment("Elytra flight")
		@Translation("category.heavyinventories.effects.elytra")
		public Elytra elytra = new Elytra();

		public Effects (EffectsSettings settings) {
			water.set(settings.water());
			lava.set(settings.lava());
			exhaustion = new Exhaustion(settings.exhaustion());
			fallDamage = new FallDamage(settings.fallDamage());
			swimming = new Swimming(settings.swimming());
			sinking = new Sinking(settings.sinking());
			upwardMovement = new UpwardMovement(settings.upwardMovement());
			knockback = new Knockback(settings.knockback());
			elytra = new Elytra(settings.elytra());
		}

		public EffectsSettings settings () {
			return new EffectsSettings(water.get(), lava.get(), exhaustion.settings(), fallDamage.settings(), swimming.settings(), sinking.settings(), upwardMovement.settings(), knockback.settings(), elytra.settings());
		}

	}

	@NoArgsConstructor
	public static final class Exhaustion {

		@Comment("Increase the cost of voluntary walking, sprinting, swimming and jumping. No additional idle drain.")
		@Translation(value = "option.heavyinventories.effects.exhaustion.enabled", tooltip = "option.heavyinventories.effects.exhaustion.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.exhaustion().enabled());

		@Comment("1 to 100. Scales existing qualifying exhaustion with the walking mode, before Surefooted. Reaches maximum at full capacity.")
		@Translation(value = "option.heavyinventories.effects.exhaustion.maxMultiplier", tooltip = "option.heavyinventories.effects.exhaustion.maxMultiplier.tooltip")
		public final FloatValue maxMultiplier = FloatValue.of(EffectsSettings.DEFAULT.exhaustion().maxMultiplier(), 1f, EffectsSettings.MAX_MULTIPLIER);

		@Comment("0 to 100. Extra cost per block of voluntary ordinary walking at full capacity; lower loads scale with walking mode.")
		@Translation(value = "option.heavyinventories.effects.exhaustion.walkingCostPerBlock", tooltip = "option.heavyinventories.effects.exhaustion.walkingCostPerBlock.tooltip")
		public final FloatValue walkingCostPerBlock = FloatValue.of(EffectsSettings.DEFAULT.exhaustion().walkingCostPerBlock(), 0f, 100f);

		public Exhaustion (EffectsSettings.Exhaustion settings) {
			enabled.set(settings.enabled());
			maxMultiplier.set(settings.maxMultiplier());
			walkingCostPerBlock.set(settings.walkingCostPerBlock());
		}

		public EffectsSettings.Exhaustion settings () {
			return new EffectsSettings.Exhaustion(enabled.get(), maxMultiplier.get(), walkingCostPerBlock.get());
		}

	}

	@NoArgsConstructor
	public static final class FallDamage {

		@Comment("Apply a configurable multiplier while preserving normal fall protection.")
		@Translation(value = "option.heavyinventories.effects.fallDamage.enabled", tooltip = "option.heavyinventories.effects.fallDamage.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.fallDamage().enabled());

		@Comment("0 to 10000 percent of effective capacity. Must be below the full-penalty threshold.")
		@Translation(value = "option.heavyinventories.effects.fallDamage.startPercent", tooltip = "option.heavyinventories.effects.fallDamage.startPercent.tooltip")
		public final FloatValue startPercent = FloatValue.of(EffectsSettings.DEFAULT.fallDamage().startPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("0 to 10000 percent of effective capacity. Must be above the onset threshold; the penalty is capped here.")
		@Translation(value = "option.heavyinventories.effects.fallDamage.fullPercent", tooltip = "option.heavyinventories.effects.fallDamage.fullPercent.tooltip")
		public final FloatValue fullPercent = FloatValue.of(EffectsSettings.DEFAULT.fallDamage().fullPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("1 to 100. Caps the added fall penalty; 1 adds no penalty.")
		@Translation(value = "option.heavyinventories.effects.fallDamage.maxMultiplier", tooltip = "option.heavyinventories.effects.fallDamage.maxMultiplier.tooltip")
		public final FloatValue maxMultiplier = FloatValue.of(EffectsSettings.DEFAULT.fallDamage().maxMultiplier(), 1f, EffectsSettings.MAX_MULTIPLIER);

		public FallDamage (EffectsSettings.FallDamage settings) {
			enabled.set(settings.enabled());
			startPercent.set(settings.startPercent());
			fullPercent.set(settings.fullPercent());
			maxMultiplier.set(settings.maxMultiplier());
		}

		public EffectsSettings.FallDamage settings () {
			return new EffectsSettings.FallDamage(enabled.get(), startPercent.get(), fullPercent.get(), maxMultiplier.get());
		}

	}

	@NoArgsConstructor
	public static final class Swimming {

		@Comment("Scale horizontal movement in enabled fluids while preserving other movement modifiers.")
		@Translation(value = "option.heavyinventories.effects.swimming.enabled", tooltip = "option.heavyinventories.effects.swimming.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.swimming().enabled());

		@Comment("0 to 10000 percent of effective capacity. Must be below the full-penalty threshold.")
		@Translation(value = "option.heavyinventories.effects.swimming.startPercent", tooltip = "option.heavyinventories.effects.swimming.startPercent.tooltip")
		public final FloatValue startPercent = FloatValue.of(EffectsSettings.DEFAULT.swimming().startPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("0 to 10000 percent of effective capacity. Must be above the onset threshold; the penalty is capped here.")
		@Translation(value = "option.heavyinventories.effects.swimming.fullPercent", tooltip = "option.heavyinventories.effects.swimming.fullPercent.tooltip")
		public final FloatValue fullPercent = FloatValue.of(EffectsSettings.DEFAULT.swimming().fullPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("0 to 1. At full penalty: 0.5 means half the otherwise applicable horizontal movement.")
		@Translation(value = "option.heavyinventories.effects.swimming.minMultiplier", tooltip = "option.heavyinventories.effects.swimming.minMultiplier.tooltip")
		public final FloatValue minMultiplier = FloatValue.of(EffectsSettings.DEFAULT.swimming().minMultiplier(), 0f, 1f);

		public Swimming (EffectsSettings.Swimming settings) {
			enabled.set(settings.enabled());
			startPercent.set(settings.startPercent());
			fullPercent.set(settings.fullPercent());
			minMultiplier.set(settings.minMultiplier());
		}

		public EffectsSettings.Swimming settings () {
			return new EffectsSettings.Swimming(enabled.get(), startPercent.get(), fullPercent.get(), minMultiplier.get());
		}

	}

	@NoArgsConstructor
	public static final class Sinking {

		@Comment("Scale gravity in enabled fluids; does not cancel currents or bubble-column motion.")
		@Translation(value = "option.heavyinventories.effects.sinking.enabled", tooltip = "option.heavyinventories.effects.sinking.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.sinking().enabled());

		@Comment("0 to 10000 percent of effective capacity. Must be below the full-penalty threshold.")
		@Translation(value = "option.heavyinventories.effects.sinking.startPercent", tooltip = "option.heavyinventories.effects.sinking.startPercent.tooltip")
		public final FloatValue startPercent = FloatValue.of(EffectsSettings.DEFAULT.sinking().startPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("0 to 10000 percent of effective capacity. Must be above the onset threshold; the penalty is capped here.")
		@Translation(value = "option.heavyinventories.effects.sinking.fullPercent", tooltip = "option.heavyinventories.effects.sinking.fullPercent.tooltip")
		public final FloatValue fullPercent = FloatValue.of(EffectsSettings.DEFAULT.sinking().fullPercent(), 0f, EffectsSettings.MAX_PERCENT);

		@Comment("1 to 100. At full penalty: 2 means twice the normal fluid gravity.")
		@Translation(value = "option.heavyinventories.effects.sinking.maxMultiplier", tooltip = "option.heavyinventories.effects.sinking.maxMultiplier.tooltip")
		public final FloatValue maxMultiplier = FloatValue.of(EffectsSettings.DEFAULT.sinking().maxMultiplier(), 1f, EffectsSettings.MAX_MULTIPLIER);

		public Sinking (EffectsSettings.Sinking settings) {
			enabled.set(settings.enabled());
			startPercent.set(settings.startPercent());
			fullPercent.set(settings.fullPercent());
			maxMultiplier.set(settings.maxMultiplier());
		}

		public EffectsSettings.Sinking settings () {
			return new EffectsSettings.Sinking(enabled.get(), startPercent.get(), fullPercent.get(), maxMultiplier.get());
		}

	}

	@NoArgsConstructor
	public static final class UpwardMovement {

		@Comment("Prevent player-driven ascent at the threshold, including jumping out of the fluid. External upward motion is preserved.")
		@Translation(value = "option.heavyinventories.effects.upwardMovement.enabled", tooltip = "option.heavyinventories.effects.upwardMovement.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.upwardMovement().enabled());

		@Comment("0 to 10000 percent of effective capacity, including Strength and equipment bonuses. Default: 100.")
		@Translation(value = "option.heavyinventories.effects.upwardMovement.thresholdPercent", tooltip = "option.heavyinventories.effects.upwardMovement.thresholdPercent.tooltip")
		public final FloatValue thresholdPercent = FloatValue.of(EffectsSettings.DEFAULT.upwardMovement().thresholdPercent(), 0f, EffectsSettings.MAX_PERCENT);

		public UpwardMovement (EffectsSettings.UpwardMovement settings) {
			enabled.set(settings.enabled());
			thresholdPercent.set(settings.thresholdPercent());
		}

		public EffectsSettings.UpwardMovement settings () {
			return new EffectsSettings.UpwardMovement(enabled.get(), thresholdPercent.get());
		}

	}

	@NoArgsConstructor
	public static final class Knockback {

		@Comment("Add temporary resistance based on actual carried pounds, independently of carrying-capacity bonuses.")
		@Translation(value = "option.heavyinventories.effects.knockback.enabled", tooltip = "option.heavyinventories.effects.knockback.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.knockback().enabled());

		@Comment("Greater than zero, at most 1000000000 stored pounds. Resistance scales linearly up to this weight.")
		@Translation(value = "option.heavyinventories.effects.knockback.referenceWeight", tooltip = "option.heavyinventories.effects.knockback.referenceWeight.tooltip")
		public final FloatValue referenceWeight = FloatValue.of(EffectsSettings.DEFAULT.knockback().referenceWeight(), Float.MIN_VALUE, ServerSettings.MAX_VALUE);

		@Comment("0 to 1. HI adds at most this amount without replacing resistance from equipment or other mods.")
		@Translation(value = "option.heavyinventories.effects.knockback.maxResistance", tooltip = "option.heavyinventories.effects.knockback.maxResistance.tooltip")
		public final FloatValue maxResistance = FloatValue.of(EffectsSettings.DEFAULT.knockback().maxResistance(), 0f, 1f);

		public Knockback (EffectsSettings.Knockback settings) {
			enabled.set(settings.enabled());
			referenceWeight.set(settings.referenceWeight());
			maxResistance.set(settings.maxResistance());
		}

		public EffectsSettings.Knockback settings () {
			return new EffectsSettings.Knockback(enabled.get(), referenceWeight.get(), maxResistance.get());
		}

	}

	@NoArgsConstructor
	public static final class Elytra {

		@Comment("Reduce elytra lift and rocket thrust with carried weight. Does not prevent deploying elytra. Creative and spectator players are exempt.")
		@Translation(value = "option.heavyinventories.effects.elytra.enabled", tooltip = "option.heavyinventories.effects.elytra.enabled.tooltip")
		public final BooleanValue enabled = BooleanValue.of(EffectsSettings.DEFAULT.elytra().enabled());

		@Comment("Greater than zero, at most 1000000000 stored pounds. Penalties grow smoothly from zero load to this weight, then stop increasing. Default: 1000.")
		@Translation(value = "option.heavyinventories.effects.elytra.referenceWeight", tooltip = "option.heavyinventories.effects.elytra.referenceWeight.tooltip")
		public final FloatValue referenceWeight = FloatValue.of(EffectsSettings.DEFAULT.elytra().referenceWeight(), Float.MIN_VALUE, ServerSettings.MAX_VALUE);

		@Comment("0 to 1. Default 0.15 reduces lift by 15% at the reference weight. This shortens glides; it is not a fixed percentage of flight distance. Zero disables this penalty.")
		@Translation(value = "option.heavyinventories.effects.elytra.maxLiftReduction", tooltip = "option.heavyinventories.effects.elytra.maxLiftReduction.tooltip")
		public final FloatValue maxLiftReduction = FloatValue.of(EffectsSettings.DEFAULT.elytra().maxLiftReduction(), 0f, 1f);

		@Comment("0 to 1. Default 0.25 reduces rocket thrust by 25% at the reference weight. Rocket duration is unchanged. Zero disables this penalty.")
		@Translation(value = "option.heavyinventories.effects.elytra.maxRocketReduction", tooltip = "option.heavyinventories.effects.elytra.maxRocketReduction.tooltip")
		public final FloatValue maxRocketReduction = FloatValue.of(EffectsSettings.DEFAULT.elytra().maxRocketReduction(), 0f, 1f);

		public Elytra (EffectsSettings.Elytra settings) {
			enabled.set(settings.enabled());
			referenceWeight.set(settings.referenceWeight());
			maxLiftReduction.set(settings.maxLiftReduction());
			maxRocketReduction.set(settings.maxRocketReduction());
		}

		public EffectsSettings.Elytra settings () {
			return new EffectsSettings.Elytra(enabled.get(), referenceWeight.get(), maxLiftReduction.get(), maxRocketReduction.get());
		}

	}

}
