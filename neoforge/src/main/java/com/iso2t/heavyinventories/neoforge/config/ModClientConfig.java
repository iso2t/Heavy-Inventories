package com.iso2t.heavyinventories.neoforge.config;

import com.iso2t.heavyinventories.client.ClientConfigScreen;
import com.iso2t.heavyinventories.config.ConfigScreenOpener;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;

public final class ModClientConfig implements ConfigScreenOpener {
    private static ConfigBuilder builder;
    public static void init() { builder = ClientConfigScreen.create(); }
    public static ConfigBuilder getBuilder() {
        if (builder == null) init();
        return builder;
    }
    @Override public void openConfigScreen() {
        init();
        Minecraft.getInstance().setScreen(builder.build());
    }
}
