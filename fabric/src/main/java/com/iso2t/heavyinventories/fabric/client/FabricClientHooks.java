package com.iso2t.heavyinventories.fabric.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.client.ClientFeedback;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.ConfigScreens;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.gui.GraphicsRenderer;
import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.network.OpenConfigPayload;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.player.PlayerHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;

/**
 * Client-only callbacks; loaded exclusively by the physical client bootstrap.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FabricClientHooks {

	public static void register () {
		ClientFeedback.register();
		HudElementRegistry.addLast(HeavyInventories.get("weight"), (graphics, _) -> GraphicsRenderer.renderGui(graphics, ConfigOptions.WEIGHT_MEASURE, Minecraft.getInstance()));
		// INFO_BAR wraps the background even at XP level zero; EXPERIENCE_LEVEL does not.
		HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR, HeavyInventories.get("weight_ring"), (graphics, _) -> WeightRingRenderer.render(graphics, Minecraft.getInstance()));
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, original -> (graphics, delta) -> WeightRingRenderer.experienceLevel(graphics, Minecraft.getInstance(), () -> original.extractRenderState(graphics, delta)));
		ClientPlayConnectionEvents.INIT.register((_, _) -> ClientWeightData.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> ClientWeightData.clear());
		ClientPlayNetworking.registerGlobalReceiver(PlayerWeightPayload.TYPE, (packet, context) -> {
			var player = context.player();
			PlayerHolder.getOrCreate(player).accept(packet);
		});
		ClientPlayNetworking.registerGlobalReceiver(ItemWeightsPayload.TYPE, (packet, _) -> ClientWeightData.accept(packet));
		ClientPlayNetworking.registerGlobalReceiver(OpenConfigPayload.TYPE, (packet, context) -> context.client().execute(() -> ConfigScreens.fromString(packet.configType()).ifPresent(ConfigScreens::open)));
	}

}
