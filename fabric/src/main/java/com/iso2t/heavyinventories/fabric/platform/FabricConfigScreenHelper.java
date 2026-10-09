package com.iso2t.heavyinventories.fabric.platform;

import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import com.iso2t.heavyinventories.network.OpenConfigPayload;
import com.iso2t.heavyinventories.network.PlayerWeightPayload;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.platform.services.IConfigScreenHelper;
import com.iso2t.heavyinventories.server.ServerConfiguration;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class FabricConfigScreenHelper implements IConfigScreenHelper {

	public static void registerPayloads () {
		PayloadTypeRegistry.clientboundPlay().register(OpenConfigPayload.TYPE, OpenConfigPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PlayerWeightPayload.TYPE, PlayerWeightPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ItemWeightsPayload.TYPE, ItemWeightsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ServerConfigUpdatePayload.TYPE, ServerConfigUpdatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ServerConfigUpdatePayload.TYPE, (packet, context) -> ServerConfiguration.update(context.player(), packet));
	}

	@Override
	public void sendOpenConfigPacket (ServerPlayer player, String configType) {
		var packet = new OpenConfigPayload(configType);
		ServerPlayNetworking.send(player, packet);
	}

}
