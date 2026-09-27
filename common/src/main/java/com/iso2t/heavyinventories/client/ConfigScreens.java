package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import java.util.Optional;

/**
 * Utility class to handle configuration screen management for client, server, and shared settings.
 * This class provides methods to open different types of configuration screens, process server-side
 * setting updates, and map string representations of setting types to corresponding enums.
 * <p>
 * It is designed as a final class with a private constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigScreens {

	/**
	 * Opens a configuration screen based on the specified {@link SettingsType}.
	 * The corresponding configuration screen is determined by the provided type:
	 * <ul>
	 *   <li>{@code CLIENT}: Opens the client configuration screen.</li>
	 *   <li>{@code SERVER}: Opens the server configuration screen. It also sets up
	 *   server configuration updates to be sent back to the server.</li>
	 *   <li>{@code COMMON}: Opens the common/shared configuration screen.</li>
	 * </ul>
	 * The screen is displayed by updating the current Minecraft GUI.
	 *
	 * @param type the {@link SettingsType} enum value that determines which configuration
	 *             screen to display. Must be one of {@code CLIENT}, {@code SERVER}, or {@code COMMON}.
	 */
	public static void open (SettingsType type) {
		var builder = switch (type) {
			case CLIENT -> ClientConfigScreen.create();
			case SERVER -> ServerConfigScreen.create(ConfigScreens::sendServerSettings);
			case COMMON -> CommonConfigScreen.create();
		};
		Minecraft.getInstance().gui.setScreen(builder.build());
	}

	/**
	 * Sends server settings update payload data from the client to the server. This method is responsible
	 * for serializing and transmitting the configuration updates encapsulated within a
	 * {@link ServerConfigUpdatePayload} instance. If there is no active server connection, the method performs no action.
	 *
	 * @param request the {@link ServerConfigUpdatePayload} containing the server configuration updates to send.
	 */
	private static void sendServerSettings (ServerConfigUpdatePayload request) {
		var connection = Minecraft.getInstance().getConnection();
		if (connection != null) connection.send(new ServerboundCustomPayloadPacket(request));
	}

	/**
	 * Converts a string representation of a settings type to its corresponding {@link SettingsType} enum value.
	 *
	 * @param type the string representation of the settings type. Valid values are "client", "server",
	 *             and "common". If the input is null or does not match any valid type, an empty {@link Optional}
	 *             will be returned.
	 * @return an {@link Optional} containing the corresponding {@link SettingsType} if the input matches
	 *         a valid type, or an empty {@link Optional} if the input is null or invalid.
	 */
	public static Optional<SettingsType> fromString (String type) {
		return switch (type) {
			case "client" -> Optional.of(SettingsType.CLIENT);
			case "server" -> Optional.of(SettingsType.SERVER);
			case "common" -> Optional.of(SettingsType.COMMON);
			case null, default -> Optional.empty();
		};
	}

	/**
	 * Represents the types of settings available in the application.
	 * This enumeration is used to differentiate between client-side, server-side, and common/shared settings.
	 *
	 * <ul>
	 *   <li>{@code CLIENT}: Refers to settings that are specific to the client and are only used locally.</li>
	 *   <li>{@code SERVER}: Refers to settings that are specific to the server and may involve updates sent back to the server.</li>
	 *   <li>{@code COMMON}: Refers to settings that are shared or applicable to both the client and the server.</li>
	 * </ul>
	 */
	public enum SettingsType {
		CLIENT, SERVER, COMMON;
	}

}
