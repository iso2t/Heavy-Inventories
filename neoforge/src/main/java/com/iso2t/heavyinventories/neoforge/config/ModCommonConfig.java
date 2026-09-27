package com.iso2t.heavyinventories.neoforge.config;

import com.iso2t.heavyinventories.client.CommonConfigScreen;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfig {

	private static ConfigBuilder builder;

	public static void init () {
		builder = CommonConfigScreen.create();
	}

	public static ConfigBuilder getBuilder () {
		if (builder == null) init();
		return builder;
	}
}
