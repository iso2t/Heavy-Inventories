package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Fresh entries on every open; both loaders share the exact same value bindings. */
public final class ClientConfigScreen {
    private ClientConfigScreen() {}
    public static ConfigBuilder create() {
        var current = ClientSettings.current();
        var defaults = ClientSettings.DEFAULT;
        var measure = new MeasuringSystem[]{current.measure()};
        var overlay = new boolean[]{current.overlay()};
        var colors = new int[]{current.normal(), current.encumbered(), current.overloaded()};
        var builder = ConfigBuilder.create().setParentScreen(null).setTitle(Component.translatable("title.heavyinventories.config.client"));
        var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
        var entries = builder.entryBuilder();
        category.addEntry(entries.startEnumSelector(Component.translatable("option.heavyinventories.weight_measure"), MeasuringSystem.class, current.measure())
                .setDefaultValue(defaults.measure()).setTooltip(Component.translatable("option.heavyinventories.weight_measure.tooltip"))
                .setSaveConsumer(value -> measure[0] = value).build());
        category.addEntry(entries.startBooleanToggle(Component.translatable("option.heavyinventories.enable_gui_overlay"), current.overlay())
                .setDefaultValue(defaults.overlay()).setSaveConsumer(value -> overlay[0] = value).build());
        String[] names = {"normal_text_color", "encumbered_text_color", "over_encumbered_text_color"};
        int[] defaultColors = {defaults.normal(), defaults.encumbered(), defaults.overloaded()};
        for (int i = 0; i < names.length; i++) {
            int index = i;
            category.addEntry(entries.startColorField(Component.translatable("option.heavyinventories." + names[i]), colors[i])
                    .setDefaultValue(defaultColors[i]).setSaveConsumer(value -> colors[index] = value).build());
        }
        builder.setSavingRunnable(() -> {
            var settings = new ClientSettings(measure[0], overlay[0], colors[0], colors[1], colors[2]);
            if (settings.equals(current)) return;
            try { ConfigFileManager.saveClientConfig(settings); }
            catch (java.io.IOException | IllegalArgumentException e) {
                HeavyInventories.LOGGER.error("Client preferences were not saved", e);
                Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("config.heavyinventories.client_failed", e.getMessage()), false);
            }
        });
        return builder;
    }
}
