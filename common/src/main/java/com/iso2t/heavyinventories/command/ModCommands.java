package com.iso2t.heavyinventories.command;

import com.iso2t.heavyinventories.helper.RegistryHelper;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.player.PlayerWeightCache;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.server.weight.LegacyWeightConverter;
import com.iso2t.heavyinventories.server.weight.WeightReportExporter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommands {

	public static void registerCommands (CommandDispatcher<CommandSourceStack> dispatcher) {
		var operatorPermission = Commands.<CommandSourceStack>hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER));
		var reload = Commands.literal("reload").requires(operatorPermission).executes(ModCommands::executeReloadCommand).then(Commands.literal("weight").executes(ModCommands::executeReloadCommand)).then(Commands.literal("players").executes(ModCommands::executeReloadPlayersCommand));
		var convert = Commands.literal("convert").requires(operatorPermission).then(Commands.literal("legacy").then(Commands.argument("pack_name", StringArgumentType.word()).executes(ModCommands::executeConvertCommand)));
		var dump = Commands.literal("dump").requires(operatorPermission).then(Commands.argument("modid", StringArgumentType.string()).suggests(ModCommands::suggestModIds).executes(ModCommands::executeDumpCommand));
		var config = Commands.literal("config").then(Commands.argument("config", StringArgumentType.string()).suggests(ModCommands::suggestConfigTypes).executes(context -> executeOpenConfig(context, StringArgumentType.getString(context, "config"))));
		dispatcher.register(Commands.literal("heavyinventories").then(reload).then(convert).then(dump).then(config));
	}

	private static int executeReloadPlayersCommand (CommandContext<CommandSourceStack> context) {
		PlayerWeightCache.clearAll(context.getSource().getServer());
		context.getSource().sendSuccess(() -> Component.translatableWithFallback("command.heavyinventories.players_reloaded", "Player weights will refresh on the next tick."), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int executeOpenConfig (CommandContext<CommandSourceStack> context, String type) {
		var source = context.getSource();
		if (!Set.of("client", "server", "common").contains(type)) {
			source.sendFailure(Component.translatableWithFallback("command.heavyinventories.config.invalid", "Unknown config: %s. Use client, server, or common.", type));
			return 0;
		}
		var player = source.getPlayer();

		if (player == null) {
			source.sendFailure(Component.translatableWithFallback("command.heavyinventories.config.no_player", "Open a config screen from a player."));
			return 0;
		}

		Services.CONFIG_SCREEN.sendOpenConfigPacket(player, type);

		source.sendSuccess(() -> Component.translatableWithFallback("command.heavyinventories.config.success", "Requested the %s config screen.", type), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int executeReloadCommand (CommandContext<CommandSourceStack> context) {
		try {
			ServerWeightState.of(context.getSource().getServer()).reload(context.getSource().getServer());
			context.getSource().sendSuccess(() -> Component.translatableWithFallback("config.heavyinventories.reloaded", "Reloaded server settings and rebuilt weights from loaded datapacks and recipes. Use /reload to load edited datapack files."), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | IllegalArgumentException | IllegalStateException e) {
			context.getSource().sendFailure(Component.translatableWithFallback("config.heavyinventories.failed", "Server settings were not applied: %s", e.getMessage()));
			return 0;
		}
	}

	private static int executeConvertCommand (CommandContext<CommandSourceStack> context) {
		var game = Services.PLATFORM.getGameDirectory();
		var version = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
		try {
			var result = LegacyWeightConverter.convert(game.resolve("weights"), game.resolve("weight-packs"), StringArgumentType.getString(context, "pack_name"), version.major(), version.minor());
			context.getSource().sendSuccess(() -> Component.translatableWithFallback("command.heavyinventories.converted", "Converted %s weights (%s entries without weights skipped) to %s. Review the ZIP, install it in the intended world's datapacks folder, and enable it. Source files and gameplay were unchanged.", result.converted(), result.skipped(), game.toAbsolutePath().normalize().relativize(result.file()).toString()), false);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | IllegalArgumentException e) {
			context.getSource().sendFailure(Component.translatableWithFallback("command.heavyinventories.conversion_failed", "Weight conversion failed: %s", e.getMessage()));
			return 0;
		}
	}

	private static int executeDumpCommand (CommandContext<CommandSourceStack> context) {
		var modid = StringArgumentType.getString(context, "modid");

		var items = RegistryHelper.getItemsFor(modid);
		var blocks = RegistryHelper.getBlocksFor(modid);
		if (!modid.matches("[a-z0-9_.-]+") || (items.isEmpty() && blocks.isEmpty())) {
			context.getSource().sendFailure(Component.literal(modid + " is invalid or not loaded!"));
			return 0;
		}

		context.getSource().sendSystemMessage(Component.literal("Dumping " + modid + "..."));
		context.getSource().sendSystemMessage(Component.literal("Found " + items.size() + " items and " + blocks.size() + " blocks."));

		var level = context.getSource().getLevel();
		Path export;
		try {
			export = WeightReportExporter.export(modid, level);
		} catch (IOException | IllegalArgumentException e) {
			context.getSource().sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.translatableWithFallback("command.heavyinventories.exported", "Exported weight report to %s, including sources. This is not an installable datapack and does not change gameplay.", Services.PLATFORM.getGameDirectory().relativize(export).toString()), false);
		return Command.SINGLE_SUCCESS;
	}

	private static CompletableFuture<Suggestions> suggestModIds (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		String platform = Services.PLATFORM.getPlatformName().toLowerCase(Locale.ROOT);
		for (var modId : Services.PLATFORM.getModIds()) {
			if (modId.equals(platform)) continue;
			if (RegistryHelper.getItemsFor(modId).isEmpty() && RegistryHelper.getBlocksFor(modId).isEmpty()) continue;
			builder.suggest(modId);
		}
		return builder.buildFuture();
	}

	private static CompletableFuture<Suggestions> suggestConfigTypes (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		builder.suggest("client");
		builder.suggest("server");
		builder.suggest("common");
		return builder.buildFuture();
	}
}
