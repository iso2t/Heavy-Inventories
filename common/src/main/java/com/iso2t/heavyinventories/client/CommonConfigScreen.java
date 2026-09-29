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
		return information(Component.translatable("title.heavyinventories.config.common"), "option.heavyinventories.common_config_info");
	}

	static ConfigScreen information (Component title, String message) {
		var schema = ConfigIntrospector.inspect(new EmptyConfig());
		var tab = new ConfigScreenTab<>(title, schema, config -> {
		}, () -> schema).editable(() -> false).description(config -> Component.translatable(message));
		return new ConfigScreen(null, title, List.of(tab));
	}

	public static final class EmptyConfig {
	}
}
