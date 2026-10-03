package com.iso2t.heavyinventories.neoforge.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.client.ClientFeedback;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.gui.GraphicsRenderer;
import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.integration.client.ClientNotifications;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Keeps client event parameter types out of server-loaded event handlers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NeoForgeClientHooks {

	public static void register (IEventBus modBus) {
		modBus.addListener(NeoForgeClientHooks::registerLayers);
		ClientFeedback.register();
		NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::logout);
		NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookTooltip);
	}

	private static void registerLayers (RegisterGuiLayersEvent event) {
		event.registerAboveAll(HeavyInventories.get("weight"), (graphics, _) -> GraphicsRenderer.renderGui(graphics, ConfigOptions.WEIGHT_MEASURE, Minecraft.getInstance()));
		var level = VanillaGuiLayers.EXPERIENCE_LEVEL;
		event.registerBelow(level, HeavyInventories.get("weight_ring"), (graphics, _) -> WeightRingRenderer.render(graphics, Minecraft.getInstance()));
		event.wrapLayer(level, original -> (graphics, delta) -> WeightRingRenderer.experienceLevel(graphics, Minecraft.getInstance(), () -> original.render(graphics, delta)));
	}

	private static void logout (ClientPlayerNetworkEvent.LoggingOut event) {
		Minecraft.getInstance().execute(() -> {
			ClientWeightData.clear();
			ClientNotifications.disconnected();
		});
	}

	public static void receiveWeight (PlayerWeightPayload packet) {
		var player = Minecraft.getInstance().player;
		if (player != null) PlayerHolder.getOrCreate(player).accept(packet);
	}

	private static void hookTooltip (ItemTooltipEvent event) {
		Tooltip.addTooltips(event.getToolTip(), event.getItemStack());
	}

}
