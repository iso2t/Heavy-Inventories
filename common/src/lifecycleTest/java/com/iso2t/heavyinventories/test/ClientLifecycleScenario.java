package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.server.ServerWeightState;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.weight.WeightCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;
import com.iso2t.heavyinventories.server.ServerConfiguration;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.platform.Services;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.players.NameAndId;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs only in disposable singleplayer smoke worlds; mutations run on the server thread. */
public final class ClientLifecycleScenario {
    private int stage;
    private int ticks;
    private LocalPlayer oldClient;
    private PlayerHolder oldServerHolder;
    private ServerPlayer serverPlayer;
    private CompletableFuture<ServerPlayer> operation;
    private CompletableFuture<Boolean> weightCheck;
    private boolean pauseOnLostFocus;
    private Path configPath;
    private byte[] originalConfig;
    private boolean originallyOp;

    public void tick(Minecraft client) {
        if (Boolean.getBoolean("heavyinventories.test.multiplayer")) {
            NetworkAuthorityClient.clientTick(client);
            return;
        }
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null || stage == 10) return;
        require(++ticks < 1200, "Timed out at client lifecycle stage " + stage);
        switch (stage) {
            case 0 -> {
                pauseOnLostFocus = client.options.pauseOnLostFocus;
                client.options.pauseOnLostFocus = false;
                oldClient = client.player;
                var uuid = oldClient.getUUID();
                operation = server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(uuid);
                    var state = ServerWeightState.of(server);
                    var values = new java.util.HashMap<>(state.weights());
                    values.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 2f);
                    state.replace(new ServerSettings(10.5f), values);
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(Items.STONE, 8));
                    PlayerHolder.getOrCreate(player);
                    return player;
                });
                stage++;
            }
            case 1 -> {
                if (!operation.isDone()) return;
                serverPlayer = operation.join();
                oldServerHolder = PlayerHolder.getOrCreate(serverPlayer);
                require(oldServerHolder != PlayerHolder.getOrCreate(client.player), "Integrated sides shared a holder");
                require(PlayerHolder.getOrCreate(client.player).getPlayer() == client.player, "Client holder owner mismatch");
                stage++;
            }
            case 2 -> {
                if (!weightsMatch(client, 16f)) return;
                // A client-only value must not overwrite the server's state, even for the same UUID.
                require(PlayerHolder.getOrCreate(client.player).getBaseMaxWeight() == 10.5f, "Fractional capacity was not synchronized");
                require(PlayerHolder.getOrCreate(client.player).isOverEncumbered(), "Server encumbrance was not synchronized");
                WeightCache.put(Items.STONE, 9999f);
                client.player.getInventory().getItem(0).setCount(63);
                PlayerEvents.onPlayerTick(client.player);
                require(PlayerHolder.getOrCreate(client.player).getWeight() == 16f, "Client inventory/cache replaced server total");
                require(ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(Items.STONE)) == 2f, "Tooltip used client weight definitions");
                operation = server.submit(() -> {
                    require(PlayerHolder.getOrCreate(serverPlayer).getWeight() == 16f, "Client mutation leaked to server");
                    serverPlayer.getInventory().getItem(0).setCount(3);
                    return serverPlayer;
                });
                stage++;
            }
            case 3 -> {
                if (!operation.isDone()) return;
                operation.join();
                if (!weightsMatch(client, 6f)) return;
                oldClient = client.player;
                operation = server.submit(() -> {
                    var replacement = server.getPlayerList().respawn(serverPlayer, true, Entity.RemovalReason.KILLED);
                    PlayerHolder.getOrCreate(replacement);
                    return replacement;
                });
                stage++;
            }
            case 4 -> {
                if (!operation.isDone()) return;
                serverPlayer = operation.join();
                if (client.player == oldClient || !weightsMatch(client, 6f)) return;
                require(PlayerHolder.getOrCreate(serverPlayer) != oldServerHolder, "Respawn retained old server holder");
                require(PlayerHolder.getOrCreate(client.player) != PlayerHolder.getOrCreate(oldClient), "Respawn retained old client holder");
                oldServerHolder = PlayerHolder.getOrCreate(serverPlayer);
                oldClient = client.player;
                operation = server.submit(() -> {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                            "execute as " + serverPlayer.getScoreboardName() + " in minecraft:the_nether run tp @s 0 100 0");
                    return serverPlayer;
                });
                stage++;
            }
            case 5 -> {
                if (!operation.isDone()) return;
                operation.join();
                if (client.player.level().dimension() != Level.NETHER || !weightsMatch(client, 6f)) return;
                require(PlayerHolder.getOrCreate(serverPlayer) == oldServerHolder, "Dimension change lost same-entity state");
                require(PlayerHolder.getOrCreate(client.player).getPlayer() == client.player, "Dimension change reused stale client entity");
                operation = server.submit(() -> {
                    var replacement = server.getPlayerList().respawn(serverPlayer, false, Entity.RemovalReason.KILLED);
                    PlayerHolder.getOrCreate(replacement);
                    return replacement;
                });
                stage++;
            }
            case 6 -> {
                if (!operation.isDone()) return;
                serverPlayer = operation.join();
                if (!weightsMatch(client, 0f)) return;
                require(PlayerHolder.getOrCreate(serverPlayer) != oldServerHolder, "Empty respawn retained old holder");
                operation = server.submit(() -> {
                    configPath = Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-server.json");
                    try { originalConfig = Files.exists(configPath) ? Files.readAllBytes(configPath) : null; }
                    catch (java.io.IOException e) { throw new RuntimeException(e); }
                    var profile = new NameAndId(serverPlayer.getGameProfile());
                    originallyOp = server.getPlayerList().isOp(profile);
                    server.getPlayerList().deop(profile);
                    require(!ServerConfiguration.canEdit(serverPlayer), "Test world must have cheats disabled for denial check");
                    var state = ServerWeightState.of(server);
                    long revision = state.revision();
                    ServerConfiguration.update(serverPlayer, new ServerConfigUpdatePayload(20.25f, revision));
                    require(state.revision() == revision, "Non-operator changed settings");
                    server.getPlayerList().op(profile);
                    require(ServerConfiguration.canEdit(serverPlayer), "Operator cannot edit settings");
                    ServerConfiguration.update(serverPlayer, new ServerConfigUpdatePayload(Float.NaN, revision));
                    ServerConfiguration.update(serverPlayer, new ServerConfigUpdatePayload(20.25f, revision - 1));
                    require(state.revision() == revision, "Invalid or stale edit changed settings");
                    PlayerHolder.getOrCreate(serverPlayer).applyBracing(1, 0.1f, 1f);
                    return serverPlayer;
                });
                stage++;
            }
            case 7 -> {
                if (!operation.isDone()) return;
                operation.join();
                var holder = PlayerHolder.getOrCreate(client.player);
                if (!holder.canEditServerConfig()) return;
                client.getConnection().send(new ServerboundCustomPayloadPacket(
                        new ServerConfigUpdatePayload(20.25f, holder.serverRevision())));
                stage++;
            }
            case 8 -> {
                var holder = PlayerHolder.getOrCreate(client.player);
                if (holder.getBaseMaxWeight() != 20.25f) return;
                require(Math.abs(holder.getBracingOffset() - 2.025f) < 0.001f, "Live edit did not rebase/synchronize Bracing");
                operation = server.submit(() -> {
                    try {
                        require(ConfigFileManager.readServerConfig(configPath).startingWeight() == 20.25f, "Network edit was not persisted");
                        // A malformed reload must preserve the running snapshot and revision.
                        var state = ServerWeightState.of(server);
                        long revision = state.revision();
                        Files.writeString(configPath, "{\"startingWeight\":0}");
                        try { state.reload(server); throw new AssertionError("Invalid reload was accepted"); }
                        catch (IllegalArgumentException expected) { }
                        require(state.revision() == revision && state.settings().startingWeight() == 20.25f, "Failed reload changed active settings");
                    } catch (java.io.IOException e) { throw new RuntimeException(e); }
                    finally {
                        try {
                            if (originalConfig == null) Files.deleteIfExists(configPath);
                            else Files.write(configPath, originalConfig);
                        } catch (java.io.IOException e) { throw new RuntimeException(e); }
                        if (!originallyOp) server.getPlayerList().deop(new NameAndId(serverPlayer.getGameProfile()));
                    }
                    return serverPlayer;
                });
                stage++;
            }
            case 9 -> {
                if (!operation.isDone()) return;
                operation.join();
                HeavyInventories.LOGGER.info("CLIENT LIFECYCLE SMOKE PASSED: server authority, inventory sync, respawn, dimension travel, operator network edit, permission/invalid/stale rejection, persistence, transactional reload, live bonus rebase");
                client.options.pauseOnLostFocus = pauseOnLostFocus;
                stage++;
                client.stop();
            }
        }
    }

    private boolean weightsMatch(Minecraft client, float expected) {
        if (weightCheck == null) {
            weightCheck = client.getSingleplayerServer().submit(() -> PlayerHolder.getOrCreate(serverPlayer).getWeight() == expected);
            return false;
        }
        if (!weightCheck.isDone()) return false;
        boolean serverMatches = weightCheck.join();
        weightCheck = null;
        return serverMatches && PlayerHolder.getOrCreate(client.player).hasServerState()
                && PlayerHolder.getOrCreate(client.player).getWeight() == expected;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
