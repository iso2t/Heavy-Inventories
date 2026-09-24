package com.iso2t.heavyinventories.fabric.core;

import net.minecraft.world.level.Level;
import com.iso2t.heavyinventories.config.ConfigFileManager;

public class HeavyInventoriesFabricServer extends HeavyInventoriesFabricBase {

    public HeavyInventoriesFabricServer() {
        super();

        ConfigFileManager.loadServerConfig();
    }

    @Override
    public Level getClientLevel() {
        return null;
    }

}
