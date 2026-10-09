package com.iso2t.heavyinventories.fabric.client;

import com.iso2t.heavyinventories.client.ClientConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public final class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<Screen> getModConfigScreenFactory () {
		return parent -> ClientConfigScreen.create(parent);
	}

}
