package com.iso2t.heavyinventories.neoforge.platform;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.ConfigScreens;
import com.iso2t.heavyinventories.neoforge.client.NeoForgeClientHooks;
import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.network.OpenConfigPayload;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.platform.services.IConfigScreenHelper;
import com.iso2t.heavyinventories.server.ServerConfiguration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgeConfigScreenHelper implements IConfigScreenHelper {

	@SuppressWarnings("unused")
	@EventBusSubscriber(modid = HeavyInventories.MOD_ID)
	public static class NetworkHandler {
		@SubscribeEvent
		public static void registerPayload (RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("4");
			registrar.playToClient(PlayerWeightPayload.TYPE, PlayerWeightPayload.CODEC, (packet, context) -> context.enqueueWork(() -> NeoForgeClientHooks.receiveWeight(packet)));
			registrar.playToClient(ItemWeightsPayload.TYPE, ItemWeightsPayload.CODEC, (packet, context) -> context.enqueueWork(() -> ClientWeightData.accept(packet)));
			registrar.playToServer(ServerConfigUpdatePayload.TYPE, ServerConfigUpdatePayload.CODEC, (packet, context) -> context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player) ServerConfiguration.update(player, packet);
			}));

			registrar.playToClient(OpenConfigPayload.TYPE, OpenConfigPayload.CODEC, (packet, context) -> context.enqueueWork(() -> ConfigScreens.fromString(packet.configType()).ifPresent(ConfigScreens::open)));
		}
	}

	@Override
	public void sendOpenConfigPacket (ServerPlayer player, String configType) {
		var packet = new OpenConfigPayload(configType);
		player.connection.send(packet);
	}
}
