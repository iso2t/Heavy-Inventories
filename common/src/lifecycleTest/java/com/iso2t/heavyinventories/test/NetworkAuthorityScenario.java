package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.config.ConfigScreens;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.weight.WeightCache;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in, separate-process localhost test. The server restores its config after the client exits. */
public final class NetworkAuthorityScenario {
    private static int serverStage, ticks, startedAt;
    private static long initialRevision;
    private static Path config;
    private static byte[] previousConfig;
    private static NameAndId profile;
    private static boolean wasOp;

    public static void serverTick(MinecraftServer server) {
        if (!server.isDedicatedServer()) return;
        require(++ticks < 2400, "Timed out waiting for multiplayer authority test, stage " + serverStage);
        var players = server.getPlayerList();
        try {
            var state = ServerWeightState.of(server);
            if (serverStage == 0 && !players.getPlayers().isEmpty()) {
                var player = players.getPlayers().getFirst();
                profile = new NameAndId(player.getGameProfile());
                wasOp = players.isOp(profile);
                players.deop(profile);
                config = Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-server.json");
                previousConfig = Files.exists(config) ? Files.readAllBytes(config) : null;
                var weights = new java.util.HashMap<>(state.weights());
                weights.put(BuiltInRegistries.ITEM.getKey(Items.STONE), 2f);
                state.replace(new ServerSettings(10.5f), weights);
                initialRevision = state.revision();
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.STONE, 8));
                startedAt = ticks;
                serverStage = 1;
            } else if (serverStage == 1 && ticks - startedAt > 100) {
                require(state.revision() == initialRevision, "Non-operator network request changed settings");
                require(java.util.Arrays.equals(previousConfig, Files.exists(config) ? Files.readAllBytes(config) : null),
                        "Non-operator request changed the config file");
                players.op(profile);
                serverStage = 2;
            } else if (serverStage == 2 && state.settings().startingWeight() == 20.25f) {
                require(state.revision() == initialRevision + 1, "Invalid/stale requests were applied");
                require(ConfigFileManager.readServerConfig(config).startingWeight() == 20.25f, "Edit was not persisted on dedicated server");
                serverStage = 3;
            } else if (serverStage == 3 && players.getPlayers().isEmpty()) {
                if (previousConfig == null) Files.deleteIfExists(config);
                else Files.write(config, previousConfig);
                if (!wasOp) players.deop(profile);
                HeavyInventories.LOGGER.info("MULTIPLAYER SERVER AUTHORITY PASSED: denied/invalid/stale requests, valid operator edit, server-side persistence");
                server.halt(false);
                serverStage = 4;
            }
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
