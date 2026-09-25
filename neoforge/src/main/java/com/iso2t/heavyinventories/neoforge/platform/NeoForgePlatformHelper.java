package com.iso2t.heavyinventories.neoforge.platform;

import com.iso2t.heavyinventories.platform.services.IPlatformHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
	@Override
	public void sendToPlayer (ServerPlayer player, CustomPacketPayload payload) {
		player.connection.send(payload);
	}

	@Override
	public String getPlatformName () {
		return "NeoForge";
	}

	@Override
	public boolean isModLoaded (String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment () {
		return !FMLEnvironment.isProduction();
	}

	@Override
	public Path getGameDirectory () {
		return FMLPaths.GAMEDIR.get();
	}
}
