package com.iso2t.heavyinventories.platform.services;

import net.minecraft.server.level.ServerPlayer;

public interface IConfigScreenHelper {
	void sendOpenConfigPacket (ServerPlayer player, String configType);
}
