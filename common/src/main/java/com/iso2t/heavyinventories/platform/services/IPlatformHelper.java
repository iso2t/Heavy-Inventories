package com.iso2t.heavyinventories.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {
	void sendToPlayer (ServerPlayer player, CustomPacketPayload payload);

	String getPlatformName ();

	boolean isModLoaded (String modId);

	boolean isDevelopmentEnvironment ();

	Path getGameDirectory ();

	default String getEnvironmentName () {

		return isDevelopmentEnvironment() ? "development" : "production";
	}
}
