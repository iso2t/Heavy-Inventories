package com.iso2t.heavyinventories.server;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.platform.Services;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;

public final class ServerConfiguration {

	private ServerConfiguration () {
	}

	public static boolean canEdit (ServerPlayer player) {
		return Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)).test(player.createCommandSourceStack());
	}

	/**
	 * Called on the server thread. Never trusts client permission, revision, or numeric input.
	 */
	public static void update (ServerPlayer player, ServerConfigUpdatePayload request) {
		if (!canEdit(player)) {
			player.sendSystemMessage(Component.translatable("config.heavyinventories.denied"));
			return;
		}
		var state = ServerWeightState.of(player.level().getServer());
		if (request.expectedRevision() != state.revision()) {
			player.sendSystemMessage(Component.translatable("config.heavyinventories.stale"));
			return;
		}
		try {
			var settings = new ServerSettings(request.startingWeight(), WalkingMode.parse(request.walkingMode()), request.effects());
			ConfigFileManager.writeServerConfig(Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-server.json"), settings);
			state.replace(settings, state.weights());
			player.sendSystemMessage(Component.translatable("config.heavyinventories.saved"));
		} catch (IOException | IllegalArgumentException e) {
			HeavyInventories.LOGGER.warn("Rejected server configuration edit: {}", e.getMessage());
			player.sendSystemMessage(Component.translatable("config.heavyinventories.failed", e.getMessage()));
		}
	}
}
