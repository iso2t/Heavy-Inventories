package com.iso2t.heavyinventories.neoforge.config;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public final class ModServerConfig {

	private static ConfigBuilder builder;

	public static void init () {
		builder = ServerConfigScreen.create(request -> {
			var connection = Minecraft.getInstance().getConnection();
			if (connection != null) connection.send(new ServerboundCustomPayloadPacket(request));
		});
	}

	public static ConfigBuilder getBuilder () {
		if (builder == null) init();
		return builder;
	}
}
