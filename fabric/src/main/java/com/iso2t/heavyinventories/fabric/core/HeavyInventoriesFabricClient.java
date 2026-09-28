package com.iso2t.heavyinventories.fabric.core;

import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.fabric.client.FabricClientHooks;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public final class HeavyInventoriesFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient () {
		FabricClientHooks.register();
		ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> Tooltip.addTooltips(lines, stack));
		ConfigFileManager.loadClientConfig();
	}
}
