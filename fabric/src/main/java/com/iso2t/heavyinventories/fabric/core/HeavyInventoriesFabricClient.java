package com.iso2t.heavyinventories.fabric.core;

import com.iso2t.heavyinventories.api.client.HeavyInventoriesClientPlugin;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.fabric.client.FabricClientHooks;
import com.iso2t.heavyinventories.fabric.integration.FabricPluginDiscovery;
import com.iso2t.heavyinventories.integration.client.ClientPlugins;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public final class HeavyInventoriesFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient () {
		ClientPlugins.INSTANCE.initialize(FabricPluginDiscovery.find("heavyinventories_client", HeavyInventoriesClientPlugin.class, HIPlugin.Side.CLIENT));
		FabricClientHooks.register();
		ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> Tooltip.addTooltips(lines, stack));
		ConfigFileManager.loadClientConfig();
	}
}
