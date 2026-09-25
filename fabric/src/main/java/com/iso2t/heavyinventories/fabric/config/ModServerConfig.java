package com.iso2t.heavyinventories.fabric.config;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ModServerConfig {
	private static ConfigBuilder builder;

	public static void init () {
		builder = ServerConfigScreen.create(ClientPlayNetworking::send);
	}

	public static ConfigBuilder getBuilder () {
		if (builder == null) init();
		return builder;
	}
}
