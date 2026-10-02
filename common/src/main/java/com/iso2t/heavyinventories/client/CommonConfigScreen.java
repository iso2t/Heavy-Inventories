package com.iso2t.heavyinventories.client;

import com.iso2t.easyconfig.api.metadata.ConfigIntrospector;
import com.iso2t.easyconfig.client.gui.ConfigScreen;
import com.iso2t.easyconfig.client.gui.ConfigScreenTab;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConfigScreen {

	public static ConfigScreen create () {
		return information(Component.translatableWithFallback("title.heavyinventories.config.common", "Heavy Inventories Common Config"), Component.translatableWithFallback("option.heavyinventories.common_config_info", "Use Client Settings for display preferences and Server Settings for gameplay."));
	}

	static ConfigScreen information (Component title, Component message) {
		var schema = ConfigIntrospector.inspect(new EmptyConfig());
		var tab = new ConfigScreenTab<>(title, schema, config -> {
		}, () -> schema).editable(() -> false).description(config -> message);
		return new ConfigScreen(null, title, List.of(tab));
	}

	public static final class EmptyConfig {
	}
}
