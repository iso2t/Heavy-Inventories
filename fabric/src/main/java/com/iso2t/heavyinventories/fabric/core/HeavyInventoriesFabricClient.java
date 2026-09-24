package com.iso2t.heavyinventories.fabric.core;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import com.iso2t.heavyinventories.fabric.client.FabricClientHooks;

public class HeavyInventoriesFabricClient extends HeavyInventoriesFabricBase {

    public HeavyInventoriesFabricClient() {
        super();

        FabricClientHooks.register();
        registerTooltipCallback();

        ConfigFileManager.loadClientConfig();
    }

    private void registerTooltipCallback() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> Tooltip.addTooltips(lines, stack));
    }

    @Override
    public Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

}
