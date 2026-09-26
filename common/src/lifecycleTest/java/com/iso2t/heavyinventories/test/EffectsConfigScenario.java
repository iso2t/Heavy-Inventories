package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import net.minecraft.network.chat.Component;

/** Exercises the actual Cloth controls and their outgoing request in a disposable client. */
public final class EffectsConfigScenario {
	public static EffectsSettings expected () {
		var d = EffectsSettings.DEFAULT;
		return new EffectsSettings(d.water(), d.lava(), new EffectsSettings.Exhaustion(true, 2.25f, 0.01f),
				new EffectsSettings.FallDamage(true, 80, 125, 3.5f), new EffectsSettings.Swimming(true, 90, 100, 0.35f),
				d.sinking(), d.upwardMovement(), new EffectsSettings.Knockback(true, 800.25f, 0.4f));
	}

	public static ServerConfigUpdatePayload editThroughScreen () {
		var sent = new java.util.concurrent.atomic.AtomicReference<ServerConfigUpdatePayload>();
		var builder = ServerConfigScreen.create(sent::set);
		net.minecraft.client.Minecraft.getInstance().setScreen(builder.build());
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
		builder.getSavingRunnable().run();
		require(sent.get() != null && sent.get().effects().equals(expected()), "Settings screen sent wrong effect values");
		net.minecraft.client.Minecraft.getInstance().setScreen(null);
		return sent.get();
	}

	public static void checkReopened () {
		var builder = ServerConfigScreen.create(_ -> { throw new AssertionError("Unchanged screen sent an edit"); });
		var falls = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fallDamage")).getEntries();
		require(((FloatListEntry) falls.get(4)).getValue() == 3.5f, "Reopened screen did not use server effect settings");
		builder.getSavingRunnable().run();
	}

	public static void checkReadOnly () {
		var builder = ServerConfigScreen.create(_ -> { throw new AssertionError("Read-only screen sent an edit"); });
		net.minecraft.client.Minecraft.getInstance().setScreen(builder.build());
		var falls = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fallDamage")).getEntries();
		require(!((me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?>) falls.get(1)).isEditable() && !((FloatListEntry) falls.get(2)).isEditable(), "Non-operator effect controls were editable");
		builder.getSavingRunnable().run();
		net.minecraft.client.Minecraft.getInstance().setScreen(null);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
