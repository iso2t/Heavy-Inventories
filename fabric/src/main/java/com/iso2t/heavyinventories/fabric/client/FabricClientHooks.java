package com.iso2t.heavyinventories.fabric.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.fabric.config.ModClientConfig;
import com.iso2t.heavyinventories.fabric.config.ModCommonConfig;
import com.iso2t.heavyinventories.fabric.config.ModServerConfig;
import com.iso2t.heavyinventories.fabric.platform.FabricConfigScreenHelper;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client-only callbacks; loaded exclusively by the physical client bootstrap.
 */
public final class FabricClientHooks {
	private FabricClientHooks () {
	}

	public static void register () {
		com.iso2t.heavyinventories.client.ClientFeedback.register();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "weight"), (graphics, _) -> com.iso2t.heavyinventories.gui.GraphicsRenderer.renderGui(graphics, com.iso2t.heavyinventories.config.ConfigOptions.WEIGHT_MEASURE, Minecraft.getInstance()));
		// INFO_BAR wraps the background even at XP level zero; EXPERIENCE_LEVEL does not.
		HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "weight_ring"), (graphics, _) -> com.iso2t.heavyinventories.gui.WeightRingRenderer.render(graphics, Minecraft.getInstance()));
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, original -> (graphics, delta) -> com.iso2t.heavyinventories.gui.WeightRingRenderer.experienceLevel(graphics, Minecraft.getInstance(), () -> original.extractRenderState(graphics, delta)));
		ClientPlayConnectionEvents.INIT.register((_, _) -> ClientWeightData.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> ClientWeightData.clear());
		ClientPlayNetworking.registerGlobalReceiver(PlayerWeightPayload.TYPE, (packet, context) -> {
			var player = context.player();
			PlayerHolder.getOrCreate(player).accept(packet);
		});
		ClientPlayNetworking.registerGlobalReceiver(ItemWeightsPayload.TYPE, (packet, _) -> ClientWeightData.accept(packet));
		ClientPlayNetworking.registerGlobalReceiver(FabricConfigScreenHelper.OPEN_CONFIG_PACKET_TYPE, (packet, context) -> context.client().execute(() -> openConfig(packet.configType())));
	}

	public static void openConfig (String type) {
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
