package com.iso2t.heavyinventories.fabric.core;

import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.fabric.client.FabricClientHooks;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class HeavyInventoriesFabricClient extends HeavyInventoriesFabricBase {

	public HeavyInventoriesFabricClient () {
		super();

		FabricClientHooks.register();
		registerTooltipCallback();

		ConfigFileManager.loadClientConfig();
	}

	private void registerTooltipCallback () {
		ItemTooltipCallback.EVENT.register((stack, _, _, lines) -> Tooltip.addTooltips(lines, stack));
	}

	@Override
	public Level getClientLevel () {
		return Minecraft.getInstance().level;
	}

}
