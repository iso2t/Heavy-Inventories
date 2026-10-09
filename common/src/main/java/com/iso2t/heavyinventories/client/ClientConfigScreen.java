package com.iso2t.heavyinventories.client;

import com.iso2t.easyconfig.api.metadata.ConfigIntrospector;
import com.iso2t.easyconfig.client.gui.ConfigScreen;
import com.iso2t.easyconfig.client.gui.ConfigScreenTab;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ClientConfig;
import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientConfigScreen {

	public static ConfigScreen create () {
		return create(null);
	}

	public static ConfigScreen create (Screen parent) {
		var title = Component.translatable("title.heavyinventories.config.client");
		var tab = new ConfigScreenTab<>(title, ConfigIntrospector.inspect(new ClientConfig(ClientSettings.current())), ClientConfigScreen::save, () -> {
			ConfigFileManager.loadClientConfig();
			return ConfigIntrospector.inspect(new ClientConfig(ClientSettings.current()));
		});
		return new ConfigScreen(parent, title, List.of(tab));
	}

	private static void save (ClientConfig config) {
		var settings = config.settings();
		if (settings.equals(ClientSettings.current())) return;
		try {
			ConfigFileManager.saveClientConfig(settings);
		} catch (IOException | IllegalArgumentException e) {
			HeavyInventories.LOGGER.error("Client preferences were not saved", e);
			Minecraft.getInstance().gui.hud.setOverlayMessage(Component.translatable("config.heavyinventories.client_failed", e.getMessage()), false);
		}
	}

}
