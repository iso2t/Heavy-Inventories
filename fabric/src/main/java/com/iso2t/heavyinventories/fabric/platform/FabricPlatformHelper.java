package com.iso2t.heavyinventories.fabric.platform;

import com.iso2t.heavyinventories.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;

public final class FabricPlatformHelper implements IPlatformHelper {
	@Override
	public void sendToPlayer (ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@Override
	public String getPlatformName () {
		return "Fabric";
	}

	@Override
	public List<String> getModIds () {
		return FabricLoader.getInstance().getAllMods().stream().map(mod -> mod.getMetadata().getId()).toList();
	}

	@Override
	public Path getGameDirectory () {
		return FabricLoader.getInstance().getGameDir();
	}
}
