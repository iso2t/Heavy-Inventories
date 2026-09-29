package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigScreens {

	public static void open (SettingsType type) {
		var screen = switch (type) {
			case CLIENT -> ClientConfigScreen.create();
			case SERVER -> ServerConfigScreen.create(ConfigScreens::sendServerSettings);
			case COMMON -> CommonConfigScreen.create();
		};
		Minecraft.getInstance().gui.setScreen(screen);
	}

	private static void sendServerSettings (ServerConfigUpdatePayload request) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection != null) connection.send(new ServerboundCustomPayloadPacket(request));
	}

	public static Optional<SettingsType> fromString (String type) {
		return switch (type) {
			case "client" -> Optional.of(SettingsType.CLIENT);
			case "server" -> Optional.of(SettingsType.SERVER);
			case "common" -> Optional.of(SettingsType.COMMON);
			case null, default -> Optional.empty();
		};
	}

	public enum SettingsType {
		CLIENT,
		SERVER,
		COMMON;
	}

}
