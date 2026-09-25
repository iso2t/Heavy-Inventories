package com.iso2t.heavyinventories.fabric.client;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.iso2t.heavyinventories.fabric.config.ModClientConfig;
import com.iso2t.heavyinventories.fabric.config.ModCommonConfig;
import com.iso2t.heavyinventories.fabric.config.ModServerConfig;
import com.iso2t.heavyinventories.fabric.platform.FabricConfigScreenHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client-only callbacks; loaded exclusively by the physical client bootstrap. */
public final class FabricClientHooks {
    private FabricClientHooks() {}

    public static void register() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> ClientWeightData.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientWeightData.clear());
        ClientPlayNetworking.registerGlobalReceiver(PlayerWeightPayload.TYPE, (packet, context) -> {
            var player = context.player();
            PlayerHolder.getOrCreate(player).accept(packet);
        });
        ClientPlayNetworking.registerGlobalReceiver(ItemWeightsPayload.TYPE, (packet, context) -> ClientWeightData.accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(FabricConfigScreenHelper.OPEN_CONFIG_PACKET_TYPE,
                (packet, context) -> context.client().execute(() -> openConfig(packet.configType())));
    }

    public static void openConfig(String type) {
        switch (type) {
            case "client" -> {
                ModClientConfig.init();
                Minecraft.getInstance().setScreen(ModClientConfig.getBuilder().build());
            }
            case "server" -> {
                ModServerConfig.init();
                Minecraft.getInstance().setScreen(ModServerConfig.getBuilder().build());
            }
            case "common" -> {
                ModCommonConfig.init();
                Minecraft.getInstance().setScreen(ModCommonConfig.getBuilder().build());
            }
        }
    }
}
