package com.iso2t.heavyinventories.neoforge.platform;

import com.iso2t.heavyinventories.platform.services.IPlatformHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.List;

public final class NeoForgePlatformHelper implements IPlatformHelper {

	@Override
	public void sendToPlayer (ServerPlayer player, CustomPacketPayload payload) {
		player.connection.send(payload);
	}

	@Override
	public String getPlatformName () {
		return "NeoForge";
	}

	@Override
	public List<String> getModIds () {
		return ModList.get().getMods().stream().map(IModInfo::getModId).toList();
	}

	@Override
	public Path getGameDirectory () {
		return FMLPaths.GAMEDIR.get();
	}

}
