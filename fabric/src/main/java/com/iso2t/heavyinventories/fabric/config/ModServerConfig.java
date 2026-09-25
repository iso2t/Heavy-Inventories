package com.iso2t.heavyinventories.fabric.config;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

public final class ModServerConfig {
    private static ConfigBuilder builder;
    public static void init() {
        builder = ServerConfigScreen.create(net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking::send);
    }
    public static ConfigBuilder getBuilder() {
        if (builder == null) init();
        return builder;
    }
}
