package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises the actual Cloth controls and their outgoing request in a disposable client.
 */
public final class EffectsConfigScenario {
	public static EffectsSettings expected () {
		var d = EffectsSettings.DEFAULT;
		return new EffectsSettings(d.water(), d.lava(), new EffectsSettings.Exhaustion(true, 2.25f, 0.01f), new EffectsSettings.FallDamage(true, 80, 125, 3.5f), new EffectsSettings.Swimming(true, 90, 100, 0.35f), d.sinking(), d.upwardMovement(), new EffectsSettings.Knockback(true, 800.25f, 0.4f), new EffectsSettings.Elytra(true, 750, 0.2f, 0.4f));
	}

	public static ServerConfigUpdatePayload editThroughScreen () {
		var sent = new AtomicReference<ServerConfigUpdatePayload>();
		var builder = ServerConfigScreen.create(sent::set);
		Minecraft.getInstance().gui.setScreen(builder.build());
		var general = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general")).getEntries();
		((FloatListEntry) general.getFirst()).setValue("20.25");
		var falls = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fallDamage")).getEntries();
		var start = (FloatListEntry) falls.get(2);
		require(start.isEditable(), "Operator effect control was read-only");
		start.setValue("140");
		require(start.getConfigError().isPresent(), "Screen accepted onset above full threshold");
		start.setValue("80");
		require(start.getConfigError().isEmpty(), "Screen retained a corrected threshold error");
		((FloatListEntry) falls.get(4)).setValue("3.5");
		((FloatListEntry) builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.exhaustion")).getEntries().get(2)).setValue("2.25");
		((FloatListEntry) builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.swimming")).getEntries().get(4)).setValue("0.35");
		((FloatListEntry) builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.knockback")).getEntries().get(2)).setValue("800.25");
		var elytra = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.elytra")).getEntries();
		((FloatListEntry) elytra.get(2)).setValue("750");
		var lift = (FloatListEntry) elytra.get(3);
		lift.setValue("1.1");
		require(lift.getConfigError().isPresent(), "Screen accepted a lift reduction above 100%");
		lift.setValue("0.2");
		require(lift.getConfigError().isEmpty(), "Screen retained a corrected lift error");
		((FloatListEntry) elytra.get(4)).setValue("0.4");
		builder.getSavingRunnable().run();
		require(sent.get() != null && sent.get().effects().equals(expected()), "Settings screen sent wrong effect values");
		Minecraft.getInstance().gui.setScreen(null);
		return sent.get();
	}

	public static void checkReopened () {
		var builder = ServerConfigScreen.create(_ -> {
			throw new AssertionError("Unchanged screen sent an edit");
		});
		var falls = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fallDamage")).getEntries();
		require(((FloatListEntry) falls.get(4)).getValue() == 3.5f, "Reopened screen did not use server effect settings");
		var elytra = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.elytra")).getEntries();
		require(((FloatListEntry) elytra.get(4)).getValue() == .4f, "Reopened screen lost rocket settings");
		builder.getSavingRunnable().run();
	}

	public static void checkReadOnly () {
		var builder = ServerConfigScreen.create(_ -> {
			throw new AssertionError("Read-only screen sent an edit");
		});
		Minecraft.getInstance().gui.setScreen(builder.build());
		var falls = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fallDamage")).getEntries();
		require(!((AbstractConfigListEntry<?>) falls.get(1)).isEditable() && !((FloatListEntry) falls.get(2)).isEditable(), "Non-operator effect controls were editable");
		var elytra = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.elytra")).getEntries();
		require(!((FloatListEntry) elytra.get(3)).isEditable(), "Non-operator flight control was editable");
		builder.getSavingRunnable().run();
		Minecraft.getInstance().gui.setScreen(null);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
