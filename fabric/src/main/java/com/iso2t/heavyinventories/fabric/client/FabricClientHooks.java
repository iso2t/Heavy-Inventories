package com.iso2t.heavyinventories.fabric.client;

import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.movement.ModifyPlayerMove;
import com.iso2t.heavyinventories.fabric.callbacks.PlayerInputCallback;
import com.iso2t.heavyinventories.fabric.config.ModClientConfig;
import com.iso2t.heavyinventories.fabric.config.ModCommonConfig;
import com.iso2t.heavyinventories.fabric.config.ModServerConfig;
import com.iso2t.heavyinventories.fabric.platform.FabricConfigScreenHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client-only callbacks; loaded exclusively by the physical client bootstrap. */
public final class FabricClientHooks {
    private FabricClientHooks() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) PlayerEvents.onPlayerTick(client.player);
        });
        PlayerInputCallback.EVENT.register(ModifyPlayerMove::hook);
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
