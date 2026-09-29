package com.iso2t.heavyinventories.test;

import com.iso2t.easyconfig.client.gui.ConfigScreen;
import com.iso2t.heavyinventories.client.ServerConfigScreen;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.ServerConfig;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises the actual EasyConfig controls and their outgoing request in a disposable client.
 */
public final class EffectsConfigScenario {
	public static EffectsSettings expected () {
		var d = EffectsSettings.DEFAULT;
		return new EffectsSettings(d.water(), d.lava(), new EffectsSettings.Exhaustion(true, 2.25f, 0.01f), new EffectsSettings.FallDamage(true, 80, 125, 3.5f), new EffectsSettings.Swimming(true, 90, 100, 0.35f), d.sinking(), d.upwardMovement(), new EffectsSettings.Knockback(true, 800.25f, 0.4f), new EffectsSettings.Elytra(true, 750, 0.2f, 0.4f));
	}

	public static ServerConfigUpdatePayload editThroughScreen () {
		var sent = new AtomicReference<ServerConfigUpdatePayload>();
		var screen = ServerConfigScreen.create(sent::set);
		Minecraft.getInstance().gui.setScreen(screen);
		checkDraft(screen);
		ConfigScreenScenario.field(screen, "option.heavyinventories.starting_max_weight").setValue("20.25");
		var start = ConfigScreenScenario.field(screen, "option.heavyinventories.effects.fallDamage.startPercent");
		require(start.active && ConfigScreenScenario.entry(screen, "effects.fallDamage.startPercent").editable(), "Operator effect control was read-only");
		start.setValue("140");
		require(screen.validationError().isPresent(), "Screen accepted onset above full threshold");
		screen.saveSelected();
		require(sent.get() == null, "Invalid thresholds were sent");
		start.setValue("80");
		require(screen.validationError().isEmpty(), "Screen retained a corrected threshold error");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.fallDamage.maxMultiplier").setValue("3.5");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.exhaustion.maxMultiplier").setValue("2.25");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.swimming.minMultiplier").setValue("0.35");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.knockback.referenceWeight").setValue("800.25");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.elytra.referenceWeight").setValue("750");
		var lift = ConfigScreenScenario.field(screen, "option.heavyinventories.effects.elytra.maxLiftReduction");
		lift.setValue("1.1");
		require(screen.validationError().isPresent(), "Screen accepted a lift reduction above 100%");
		lift.setValue("not a number");
		screen.saveSelected();
		require(screen.validationError().isPresent() && sent.get() == null, "Invalid text allowed saving the previous numeric value");
		lift.setValue("0.2");
		require(screen.validationError().isEmpty(), "Screen retained a corrected lift error");
		ConfigScreenScenario.field(screen, "option.heavyinventories.effects.elytra.maxRocketReduction").setValue("0.4");
		screen.saveSelected();
		require(sent.get() != null && sent.get().effects().equals(expected()), "Settings screen sent wrong effect values");
		Minecraft.getInstance().gui.setScreen(null);
		return sent.get();
	}

	public static void checkReopened () {
		var screen = ServerConfigScreen.create(_ -> {
			throw new AssertionError("Unchanged screen sent an edit");
		});
		require((float) ConfigScreenScenario.entry(screen, "effects.fallDamage.maxMultiplier").value() == 3.5f, "Reopened screen did not use server effect settings");
		require((float) ConfigScreenScenario.entry(screen, "effects.elytra.maxRocketReduction").value() == .4f, "Reopened screen lost rocket settings");
		screen.saveSelected();
	}

	public static void checkReadOnly () {
		var screen = ServerConfigScreen.create(_ -> {
			throw new AssertionError("Read-only screen sent an edit");
		});
		Minecraft.getInstance().gui.setScreen(screen);
		require(!screen.selectedTab().editable(), "Non-operator tab was editable");
		require(ConfigScreenScenario.descendants(screen).noneMatch(EditBox.class::isInstance), "Read-only screen exposed input fields");
		ConfigScreenScenario.entry(screen, "effects.elytra.maxLiftReduction").setValue(.9f);
		screen.saveSelected();
		Minecraft.getInstance().gui.setScreen(null);
	}

	private static void checkDraft (ConfigScreen screen) {
		var schema = screen.selectedTab().schema();
		require(schema.find("revision").isEmpty(), "Server revision became a config control");
		require(schema.entries().stream().allMatch(entry -> entry.field() != null), "Control did not originate from a config field");
		var defaults = new ServerConfig();
		require(defaults.settings().equals(ServerSettings.DEFAULT), "Annotated defaults differ from runtime defaults");
		var capacity = ConfigScreenScenario.entry(screen, "startingWeight");
		var initial = capacity.value();
		capacity.setValue(1234f);
		require(!defaults.startingWeight.get().equals(capacity.value()), "Drafts share mutable fields");
		screen.reloadSelected();
		require(ConfigScreenScenario.entry(screen, "startingWeight").value().equals(initial), "Reload kept unsaved values");
		var mode = ConfigScreenScenario.entry(screen, "walkingMode");
		require(screen.selectedTab().valueLabel(mode, WalkingMode.AT_NINETY_PERCENT).equals(Component.translatable("option.heavyinventories.walking_mode.at_ninety_percent")), "Enum translation was lost");
		var onset = ConfigScreenScenario.entry(screen, "effects.fallDamage.startPercent");
		onset.setValue(200f);
		require(screen.validationError().isPresent(), "Reload detached the validator from its draft");
		onset.resetValue();
		require(screen.validationError().isEmpty(), "Reset did not restore a valid threshold");
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
