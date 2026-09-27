package com.iso2t.heavyinventories.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
	void sendToPlayer (net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload);

	String getPlatformName ();

	boolean isModLoaded (String modId);

	boolean isDevelopmentEnvironment ();

	Path getGameDirectory ();

	default String getEnvironmentName () {

		return isDevelopmentEnvironment() ? "development" : "production";
	}
}
