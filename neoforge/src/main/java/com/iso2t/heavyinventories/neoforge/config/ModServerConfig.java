package com.iso2t.heavyinventories.neoforge.config;

import com.iso2t.heavyinventories.client.ServerConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;

public final class ModServerConfig {
    private static ConfigBuilder builder;
    public static void init() {
        builder = ServerConfigScreen.create(request -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(request));
        });
    }
    public static ConfigBuilder getBuilder() {
        if (builder == null) init();
        return builder;
    }
}
