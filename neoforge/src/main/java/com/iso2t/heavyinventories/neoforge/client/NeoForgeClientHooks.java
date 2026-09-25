package com.iso2t.heavyinventories.neoforge.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.client.ClientFeedback;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.gui.GraphicsRenderer;
import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.neoforge.config.ModClientConfig;
import com.iso2t.heavyinventories.neoforge.config.ModCommonConfig;
import com.iso2t.heavyinventories.neoforge.config.ModServerConfig;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Keeps client event parameter types out of server-loaded event handlers.
 */
public final class NeoForgeClientHooks {
	private NeoForgeClientHooks () {
	}

	public static void register (IEventBus modBus) {
		modBus.addListener(NeoForgeClientHooks::registerLayers);
		ClientFeedback.register();
		NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::logout);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookTooltip);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookGui);
	}

	private static void registerLayers (RegisterGuiLayersEvent event) {
		var level = VanillaGuiLayers.EXPERIENCE_LEVEL;
		event.registerBelow(level, Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "weight_ring"), (graphics, _) -> WeightRingRenderer.render(graphics, Minecraft.getInstance()));
		event.wrapLayer(level, original -> (graphics, delta) -> WeightRingRenderer.experienceLevel(graphics, Minecraft.getInstance(), () -> original.render(graphics, delta)));
	}

	private static void logout (ClientPlayerNetworkEvent.LoggingOut event) {
		ClientWeightData.clear();
	}

	public static void receiveWeight (PlayerWeightPayload packet) {
		var player = Minecraft.getInstance().player;
		if (player != null) PlayerHolder.getOrCreate(player).accept(packet);
	}

	private static void hookTooltip (ItemTooltipEvent event) {
		Tooltip.addTooltips(event.getToolTip(), event.getItemStack());
	}


	private static void hookGui (RenderGuiEvent.Post event) {
		GraphicsRenderer.renderGui(event.getGuiGraphics(), ConfigOptions.WEIGHT_MEASURE, Minecraft.getInstance());
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
