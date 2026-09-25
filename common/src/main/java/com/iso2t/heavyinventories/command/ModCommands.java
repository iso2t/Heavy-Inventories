package com.iso2t.heavyinventories.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
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
import net.minecraft.world.item.ItemStack;
import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.config.ServerSettings;
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
                        .requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
                        .then(Commands.literal("set")
                                .then(Commands.literal("weight")
                                        .then(RequiredArgumentBuilder.<CommandSourceStack, Float>argument("weight_argument", FloatArgumentType.floatArg())
                                                .executes(ModCommands::executeSetWeightCommand)
                                        )
                                )
                        )
                        .then(Commands.literal("reload")
                                .executes(ModCommands::executeReloadCommand)
                                .then(Commands.literal("weight")
                                        .executes(ModCommands::executeReloadCommand)
                                )
                                .then(Commands.literal("players")
                                        .executes(context -> {
                                            PlayerWeightCache.clearAll(context.getSource().getServer());
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("dump")
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
        var player = source.getPlayer();
        
        if (player == null) {
            source.sendFailure(Component.translatable("command.heavyinventories.config.no_player"));
            return 0;
        }

        Services.CONFIG_SCREEN.sendOpenConfigPacket(player, type);
        
        source.sendSuccess(() -> Component.translatable("command.heavyinventories.config.success", type), true);
        return Command.SINGLE_SUCCESS;
    }

    protected static int executeSetWeightCommand(CommandContext<CommandSourceStack> context) {
        float number = FloatArgumentType.getFloat(context, "weight_argument");

        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(Component.translatable("command.heavyinventories.command_set.failure", number));
            return 0;
        }
        ItemStack stack = context.getSource().getPlayer().getItemInHand(context.getSource().getPlayer().getUsedItemHand());
        try { ServerSettings.validateItemWeight(number); }
        catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Hold an item to set its weight."));
            return 0;
        }
        WeightOverride.put(stack.getItem(), number);
        if (executeReloadCommand(context) == 0) return 0;
        context.getSource().sendSuccess(() -> Component.translatable("command.heavyinventories.command_set.success", number), true);
        return Command.SINGLE_SUCCESS;
    }

    protected static int executeReloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerWeightState.of(context.getSource().getServer()).reload(context.getSource().getServer());
            WeightCache.clearAll();
            context.getSource().sendSuccess(() -> Component.translatable("config.heavyinventories.reloaded"), true);
            return Command.SINGLE_SUCCESS;
        } catch (java.io.IOException | IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("config.heavyinventories.failed", e.getMessage()));
            return 0;
        }
    }

    protected static int executeDumpCommand(CommandContext<CommandSourceStack> context) {
        var modid = StringArgumentType.getString(context, "modid");

        if (!Services.PLATFORM.isModLoaded(modid)) {
            context.getSource().sendFailure(Component.literal(modid + " is invalid or not loaded!"));
            return 0;
        }

        var items = RegistryHelper.getItemsFor(modid);
        var blocks = RegistryHelper.getBlocksFor(modid);

        context.getSource().sendSystemMessage(Component.literal("Dumping " + modid + "..."));
        context.getSource().sendSystemMessage(Component.literal("Found " + items.size() + " items and " + blocks.size() + " blocks."));

        var level = context.getSource().getLevel();
        try { WeightOverride.putDumpFile(items, blocks, level); }
        catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        if (executeReloadCommand(context) == 0) return 0;

        context.getSource().sendSuccess(() -> Component.literal("Done!"), true);
        return Command.SINGLE_SUCCESS;
    }

    // Suggestion provider for the dump command.
    private static class ModidSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (var modid : HeavyInventories.getInstance().getModIds()) {
                if (modid.equals(Services.PLATFORM.getPlatformName().toLowerCase())) continue;
                if (RegistryHelper.getItemsFor(modid).isEmpty() || RegistryHelper.getBlocksFor(modid).isEmpty()) continue;

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
