package com.iso2t.heavyinventories.test;

import com.iso2t.easyconfig.api.metadata.ConfigEntry;
import com.iso2t.easyconfig.client.gui.ConfigScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.stream.Stream;

final class ConfigScreenScenario {
	static ConfigEntry entry (ConfigScreen screen, String path) {
		return screen.selectedTab().schema().find(path.toLowerCase(Locale.ROOT)).orElseThrow();
	}

	static EditBox field (GuiEventListener screen, String translation) {
		return descendants(screen).filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(box -> box.getMessage().equals(Component.translatable(translation))).findFirst().orElseThrow();
	}

	static Stream<GuiEventListener> descendants (GuiEventListener listener) {
		return Stream.concat(Stream.of(listener), listener instanceof ContainerEventHandler container ? container.children().stream().flatMap(ConfigScreenScenario::descendants) : Stream.empty());
	}
}
