package com.iso2t.heavyinventories.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;

public interface IPlatformHelper {
	void sendToPlayer (ServerPlayer player, CustomPacketPayload payload);

	String getPlatformName ();

	List<String> getModIds ();

	Path getGameDirectory ();
}
