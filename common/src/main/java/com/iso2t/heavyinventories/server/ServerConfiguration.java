package com.iso2t.heavyinventories.server;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerConfiguration {

	public static boolean canEdit (ServerPlayer player) {
		return Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)).test(player.createCommandSourceStack());
	}

	/**
	 * Called on the server thread. Never trusts client permission, revision, or numeric input.
	 */
	public static void update (ServerPlayer player, ServerConfigUpdatePayload request) {
		if (!canEdit(player)) {
			player.sendSystemMessage(Component.translatableWithFallback("config.heavyinventories.denied", "Only server operators can change server settings."));
			return;
		}
		var state = ServerWeightState.of(player.level().getServer());
		if (request.expectedRevision() != state.revision()) {
			player.sendSystemMessage(Component.translatableWithFallback("config.heavyinventories.stale", "Server settings changed while this screen was open. Reopen it and try again."));
			return;
		}
		try {
			var settings = new ServerSettings(request.startingWeight(), WalkingMode.parse(request.walkingMode()), request.effects());
			ConfigFileManager.writeServerConfig(ConfigFileManager.serverPath(), settings);
			state.replace(settings, state.weights());
			player.sendSystemMessage(Component.translatableWithFallback("config.heavyinventories.saved", "Server settings saved. The change applies to all players."));
		} catch (IOException | IllegalArgumentException e) {
			HeavyInventories.LOGGER.warn("Rejected server configuration edit: {}", e.getMessage());
			player.sendSystemMessage(Component.translatableWithFallback("config.heavyinventories.failed", "Server settings were not applied: %s", e.getMessage()));
		}
	}
}
