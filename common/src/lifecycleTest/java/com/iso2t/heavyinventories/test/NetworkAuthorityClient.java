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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NetworkAuthorityClient {
    private static int clientStage, ticks;
    public static void clientTick(Minecraft client) {
        if (client.player == null || clientStage == 3) return;
        require(++ticks < 1200, "Timed out at multiplayer client stage " + clientStage);
        var holder = PlayerHolder.getOrCreate(client.player);
        if (!holder.hasServerState()) return;
        if (clientStage == 0 && holder.getWeight() == 16f && holder.getBaseMaxWeight() == 10.5f) {
            require(!holder.canEditServerConfig(), "Test client unexpectedly has permission");
            require(holder.isOverEncumbered(), "Remote encumbrance missing");
            require(ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(Items.STONE)) == 2f, "Remote item definitions missing");
            WeightCache.put(Items.STONE, 9999f);
            ConfigScreens.openServerConfig();
            require(client.screen != null, "Read-only server config screen did not build");
            client.setScreen(null);
            send(client, 20.25f, holder.serverRevision());
            clientStage = 1;
        } else if (clientStage == 1 && holder.canEditServerConfig()) {
            ConfigScreens.openServerConfig();
            require(client.screen != null, "Operator server config screen did not build");
            client.setScreen(null);
            send(client, Float.NaN, holder.serverRevision());
            send(client, 20.25f, holder.serverRevision() - 1);
            send(client, 20.25f, holder.serverRevision());
            clientStage = 2;
        } else if (clientStage == 2 && holder.getBaseMaxWeight() == 20.25f) {
            require(holder.getWeight() == 16f, "Local definitions replaced remote total");
            require(!holder.isOverEncumbered(), "Capacity edit did not refresh remote encumbrance");
            HeavyInventories.LOGGER.info("MULTIPLAYER CLIENT AUTHORITY PASSED: remote totals/definitions/encumbrance, read-only and editable screen construction, operator network edits");
            clientStage = 3;
            client.stop();
        }
    }

    private static void send(Minecraft client, float capacity, long revision) {
        client.getConnection().send(new ServerboundCustomPayloadPacket(new ServerConfigUpdatePayload(capacity, revision)));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
