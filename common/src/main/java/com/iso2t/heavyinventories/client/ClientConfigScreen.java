package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.HudMode;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConfigScreen {

	public static ConfigBuilder create () {
		var current = ClientSettings.current();
		var defaults = ClientSettings.DEFAULT;
		var builder = ConfigBuilder.create().setParentScreen(null).setTitle(Component.translatable("title.heavyinventories.config.client"));
		var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
		var entries = builder.entryBuilder();
		var measure = entries.startEnumSelector(Component.translatable("option.heavyinventories.weight_measure"), MeasuringSystem.class, current.measure()).setDefaultValue(defaults.measure()).setTooltip(Component.translatable("option.heavyinventories.weight_measure.tooltip")).build();
		var overlay = entries.startBooleanToggle(Component.translatable("option.heavyinventories.enable_gui_overlay"), current.overlay()).setDefaultValue(defaults.overlay()).build();
		var normal = entries.startColorField(Component.translatable("option.heavyinventories.normal_text_color"), current.normal()).setDefaultValue(defaults.normal()).build();
		var encumbered = entries.startColorField(Component.translatable("option.heavyinventories.encumbered_text_color"), current.encumbered()).setDefaultValue(defaults.encumbered()).build();
		var overloaded = entries.startColorField(Component.translatable("option.heavyinventories.over_encumbered_text_color"), current.overloaded()).setDefaultValue(defaults.overloaded()).build();
		var mode = entries.startEnumSelector(Component.translatable("option.heavyinventories.hud_mode"), HudMode.class, current.hudMode()).setEnumNameProvider(value -> Component.translatable("option.heavyinventories.hud_mode." + value.name().toLowerCase(Locale.ROOT))).setDefaultValue(defaults.hudMode()).build();
		var offset = entries.startIntField(Component.translatable("option.heavyinventories.ring_vertical_offset"), current.ringVerticalOffset()).setMin(0).setMax(64).setDefaultValue(defaults.ringVerticalOffset()).setTooltip(Component.translatable("option.heavyinventories.ring_vertical_offset.tooltip")).build();

		category.addEntry(measure);
		category.addEntry(overlay);
		category.addEntry(normal);
		category.addEntry(encumbered);
		category.addEntry(overloaded);
		category.addEntry(mode);
		category.addEntry(offset);
		builder.setSavingRunnable(() -> {
			var settings = new ClientSettings(measure.getValue(), overlay.getValue(), normal.getValue(), encumbered.getValue(), overloaded.getValue(), mode.getValue(), offset.getValue());
			if (settings.equals(current)) return;
			try {
				ConfigFileManager.saveClientConfig(settings);
			} catch (IOException | IllegalArgumentException e) {
				HeavyInventories.LOGGER.error("Client preferences were not saved", e);
				Minecraft.getInstance().gui.hud.setOverlayMessage(Component.translatable("config.heavyinventories.client_failed", e.getMessage()), false);
			}
		});
		return builder;
	}
}
