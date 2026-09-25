package com.iso2t.heavyinventories.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.api.player.PlayerWeightCache;
import com.iso2t.heavyinventories.api.weight.WeightCache;
import com.iso2t.heavyinventories.api.weight.WeightOverride;
import com.iso2t.heavyinventories.helper.RegistryHelper;
import com.iso2t.heavyinventories.platform.Services;

import java.util.concurrent.CompletableFuture;

public class ModCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("heavyinventories")
                        .then(Commands.literal("reload").requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
                                .executes(ModCommands::executeReloadCommand)
                                .then(Commands.literal("weight")
                                        .executes(ModCommands::executeReloadCommand)
                                )
                                .then(Commands.literal("players")
                                        .executes(context -> {
                                            PlayerWeightCache.clearAll(context.getSource().getServer());
                                            context.getSource().sendSuccess(() -> Component.translatable("command.heavyinventories.players_reloaded"), false);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("dump").requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("modid", StringArgumentType.string())
                                        .suggests(new ModidSuggestionProvider())
                                        .executes(ModCommands::executeDumpCommand)
                                )
                        )
                        .then(Commands.literal("config").then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("config", StringArgumentType.string())
                                .suggests(new ConfigSuggestionProvider())
                                .executes(context -> executeOpenConfig(context, StringArgumentType.getString(context, "config")))))
        );
    }

    protected static int executeOpenConfig(CommandContext<CommandSourceStack> context, String type) {
        var source = context.getSource();
        if (!java.util.Set.of("client", "server", "common").contains(type)) {
            source.sendFailure(Component.translatable("command.heavyinventories.config.invalid", type));
            return 0;
        }
        var player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.translatable("command.heavyinventories.config.no_player"));
            return 0;
        }

        Services.CONFIG_SCREEN.sendOpenConfigPacket(player, type);
        
        source.sendSuccess(() -> Component.translatable("command.heavyinventories.config.success", type), false);
        return Command.SINGLE_SUCCESS;
    }

    protected static int executeReloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerWeightState.of(context.getSource().getServer()).reload(context.getSource().getServer());
            WeightCache.clearAll();
            context.getSource().sendSuccess(() -> Component.translatable("config.heavyinventories.reloaded"), true);
            return Command.SINGLE_SUCCESS;
        } catch (java.io.IOException | IllegalArgumentException | IllegalStateException e) {
            context.getSource().sendFailure(Component.translatable("config.heavyinventories.failed", e.getMessage()));
            return 0;
        }
    }

    protected static int executeDumpCommand(CommandContext<CommandSourceStack> context) {
        var modid = StringArgumentType.getString(context, "modid");

        if (!modid.matches("[a-z0-9_.-]+") || (RegistryHelper.getItemsFor(modid).isEmpty() && RegistryHelper.getBlocksFor(modid).isEmpty())) {
            context.getSource().sendFailure(Component.literal(modid + " is invalid or not loaded!"));
            return 0;
        }

        var items = RegistryHelper.getItemsFor(modid);
        var blocks = RegistryHelper.getBlocksFor(modid);

        context.getSource().sendSystemMessage(Component.literal("Dumping " + modid + "..."));
        context.getSource().sendSystemMessage(Component.literal("Found " + items.size() + " items and " + blocks.size() + " blocks."));

        var level = context.getSource().getLevel();
        java.nio.file.Path export;
        try { export = WeightOverride.putDumpFile(modid, items, blocks, level); }
        catch (java.io.IOException | IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.heavyinventories.exported", Services.PLATFORM.getGameDirectory().relativize(export).toString()), false);
        return Command.SINGLE_SUCCESS;
    }

    // Suggestion provider for the dump command.
    private static class ModidSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (var modid : HeavyInventories.getInstance().getModIds()) {
                if (modid.equals(Services.PLATFORM.getPlatformName().toLowerCase())) continue;
                if (RegistryHelper.getItemsFor(modid).isEmpty() && RegistryHelper.getBlocksFor(modid).isEmpty()) continue;

                builder.suggest(modid);
            }

            return builder.buildFuture();
        }

    }

    private static class ConfigSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            builder.suggest("client");
            builder.suggest("server");
            builder.suggest("common");
            return builder.buildFuture();
        }
    }

}
