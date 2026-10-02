package com.iso2t.heavyinventories.client;

import com.iso2t.easyconfig.api.metadata.ConfigIntrospector;
import com.iso2t.easyconfig.client.gui.ConfigScreen;
import com.iso2t.easyconfig.client.gui.ConfigScreenTab;
import com.iso2t.heavyinventories.config.ServerConfig;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.player.PlayerHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerConfigScreen {

	public static ConfigScreen create (Consumer<ServerConfigUpdatePayload> send) {
		var title = Component.translatableWithFallback("title.heavyinventories.config.server", "Heavy Inventories Server Config");
		var player = Minecraft.getInstance().player;
		if (player == null || !PlayerHolder.getOrCreate(player).hasServerState()) {
			return CommonConfigScreen.information(title, Component.translatableWithFallback("config.heavyinventories.unavailable", "Join a Heavy Inventories server to view its settings."));
		}
		var holder = PlayerHolder.getOrCreate(player);
		var tab = new ConfigScreenTab<>(title, ConfigIntrospector.inspect(new ServerConfig(holder.serverSettings(), holder.serverRevision())), config -> {
			if (!holder.canEditServerConfig()) return;
			var updated = config.settings();
			if (!updated.equals(holder.serverSettings())) send.accept(new ServerConfigUpdatePayload(updated, config.revision));
		}, () -> ConfigIntrospector.inspect(new ServerConfig(holder.serverSettings(), holder.serverRevision()))).editable(holder::canEditServerConfig).description(config -> holder.canEditServerConfig()
				? Component.translatableWithFallback("config.heavyinventories.edit_help", "Saving sends your changes to the server. Operator permission is checked there; accepted changes apply to all players.")
				: Component.translatableWithFallback("config.heavyinventories.read_only", "These are the server's settings. Only server operators can change them.")).validation(ServerConfigScreen::validate);
		return new ConfigScreen(null, title, List.of(tab));
	}

	private static Optional<Component> validate (ServerConfig config) {
		try {
			config.settings();
			return Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.of(Component.literal(e.getMessage()));
		}
	}
}
