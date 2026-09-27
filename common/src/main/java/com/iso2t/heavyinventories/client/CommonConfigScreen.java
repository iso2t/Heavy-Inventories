package com.iso2t.heavyinventories.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.network.chat.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonConfigScreen {

	public static ConfigBuilder create () {
		var builder = ConfigBuilder.create().setParentScreen(null).setTitle(Component.translatable("title.heavyinventories.config.common"));
		var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
		category.addEntry(builder.entryBuilder().startTextDescription(Component.translatable("option.heavyinventories.common_config_info")).build());
		return builder;
	}
}
