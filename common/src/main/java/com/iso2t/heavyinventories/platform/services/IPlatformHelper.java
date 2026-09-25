package com.iso2t.heavyinventories.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
	void sendToPlayer (net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload);

	/**
	 * Gets the name of the current platform
	 *
	 * @return The name of the current platform.
	 */
	String getPlatformName ();

	/**
	 * Checks if a mod with the given id is loaded.
	 *
	 * @param modId The mod to check if it is loaded.
	 * @return True if the mod is loaded, false otherwise.
	 */
	boolean isModLoaded (String modId);

	/**
	 * Check if the game is currently in a development environment.
	 *
	 * @return True if in a development environment, false otherwise.
	 */
	boolean isDevelopmentEnvironment ();

	/**
	 * Gets the root game directory for the current platform.
	 *
	 * @return The game directory.
	 */
	Path getGameDirectory ();

	/**
	 * Gets the name of the environment type as a string.
	 *
	 * @return The name of the environment type.
	 */
	default String getEnvironmentName () {

		return isDevelopmentEnvironment() ? "development" : "production";
	}
}
