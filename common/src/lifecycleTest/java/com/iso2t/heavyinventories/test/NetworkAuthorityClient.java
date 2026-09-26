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
	private static int clientStage, ticks, reconnectTicks;
	private static int ringWait, ringStart, xpStart;
	private static volatile boolean                                          captured;
	private static          com.iso2t.heavyinventories.config.ClientSettings originalSettings;
	private static          net.minecraft.client.player.LocalPlayer          initialPlayer;
	private static          net.minecraft.client.multiplayer.ServerData      reconnectServer;

	public static void clientTick (Minecraft client) {
		if (clientStage == 6) return;
		require(++ticks < 1200, "Timed out at multiplayer client stage " + clientStage);
		if (clientStage == 4) {
			require(client.player == null, "Disconnect retained local player");
			if (++reconnectTicks < 15) return;
			require(ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(Items.STONE)) == null, "Disconnect retained server definitions");
			net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), client, net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(reconnectServer.ip), reconnectServer, false, null);
			clientStage = 5;
			return;
		}
		if (client.player == null) {
			if (clientStage > 0 && clientStage != 5) {
				client.stop();
				throw new AssertionError("Disconnected before multiplayer assertions completed");
			}
			return;
		}
		var holder = PlayerHolder.getOrCreate(client.player);
		if (!holder.hasServerState()) return;
		if (clientStage == 0 && holder.getWeight() == 16f && holder.getBaseMaxWeight() == 10.5f) {
			originalSettings = com.iso2t.heavyinventories.config.ClientSettings.current();
			com.iso2t.heavyinventories.config.ClientSettings.DEFAULT.apply();
			RingHudScenario.active = true;
			require(!holder.canEditServerConfig(), "Test client unexpectedly has permission");
			EffectsConfigScenario.checkReadOnly();
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
			client.getConnection().send(new ServerboundCustomPayloadPacket(new ServerConfigUpdatePayload(20.25f, "invalid", holder.serverRevision())));
			var edit = EffectsConfigScenario.editThroughScreen();
			client.getConnection().send(new ServerboundCustomPayloadPacket(new ServerConfigUpdatePayload(edit.startingWeight(), "at_ninety_percent", edit.effects(), edit.expectedRevision())));
			clientStage = 2;
		} else if (clientStage == 2 && holder.getBaseMaxWeight() == 20.25f) {
			require(holder.serverSettings().effects().equals(EffectsConfigScenario.expected()), "Remote effects did not synchronize");
			EffectsConfigScenario.checkReopened();
			HeavyInventories.LOGGER.info("MULTIPLAYER EFFECT CONFIG PASSED: read-only controls, operator controls, validation, remote persistence/synchronization and reopen");
			require(holder.getWeight() == 16f, "Local definitions replaced remote total");
			require(!holder.isOverEncumbered(), "Capacity edit did not refresh remote encumbrance");
			HeavyInventories.LOGGER.info("MULTIPLAYER CLIENT AUTHORITY PASSED: remote totals/definitions/encumbrance, read-only and editable screen construction, operator network edits");
			require(holder.walkingMode() == com.iso2t.heavyinventories.config.WalkingMode.AT_NINETY_PERCENT, "Walking mode did not synchronize");
			MovementScenario.checkImpulse(client.player, 1);
			send(client, 25.5f, holder.serverRevision());
			clientStage = 3;
		} else if (clientStage == 3 && holder.getMaxWeight() == 1200 && holder.getWeight() == 1500) {
			if (!holder.serverSettings().effects().upwardMovement().enabled()) return;
			if (!verifyRing(client, "multiplayer-overloaded")) return;
			require(holder.getStrengthOffset() == 200 && holder.isOverEncumbered(), "Remote Strength/encumbrance mismatch");
			MovementScenario.checkImpulse(client.player, 0.2);
			FluidMovementScenario.checkClient(client.player);
			var fluidNotice = (com.iso2t.heavyinventories.test.mixin.GuiFeedbackTestAccess) client.gui;
			require(fluidNotice.heavyinventories$message() != null && fluidNotice.heavyinventories$message().getString().contains("swim upward"), "Missing fluid denial feedback");
			fluidNotice.heavyinventories$messageTime(20);
			FluidMovementScenario.checkClient(client.player);
			require(fluidNotice.heavyinventories$messageTime() == 20, "Fluid denial feedback was not throttled");
			HeavyInventories.LOGGER.info("MULTIPLAYER MOVEMENT PASSED: live walking-mode edit, Strength capacity, Surefooted, normalized client physics");
			initialPlayer = client.player;
			reconnectServer = client.getCurrentServer();
			require(reconnectServer != null, "Missing dedicated server address");
			clientStage = 4;
			client.disconnectFromWorld(net.minecraft.network.chat.Component.literal("Lifecycle reconnect test"));
		} else if (clientStage == 5 && holder.getBaseMaxWeight() == 512 && holder.getWeight() == 45 && holder.getMaxWeight() == 512) {
			if (!verifyRing(client, "multiplayer-reconnected")) return;
			require(client.player != initialPlayer && holder != PlayerHolder.getOrCreate(initialPlayer), "Reconnect reused local holder");
			require(ClientWeightData.weight(BuiltInRegistries.ITEM.getKey(Items.STONE)) == 3f, "Reconnect kept old definitions");
			require(!holder.isEncumbered() && !holder.isOverEncumbered() && holder.getStrengthOffset() == 0, "Reconnect kept stale bonuses/penalties");
			require(holder.serverSettings().effects().equals(com.iso2t.heavyinventories.config.EffectsSettings.DEFAULT), "Reconnect retained stale effects");
			HeavyInventories.LOGGER.info("MULTIPLAYER RECONNECT CLIENT PASSED: cleared disconnect data, fresh entity, new server definitions/capacity, rebuilt bonuses");
			HeavyInventories.LOGGER.info("MULTIPLAYER RING PASSED: rendered before and after reconnect, fresh synchronized status, vanilla XP offset, no duplicate draws");
			RingHudScenario.active = false;
			originalSettings.apply();
			clientStage = 6;
			client.stop();
		}
	}

	private static boolean verifyRing (Minecraft client, String name) {
		if (ringWait == 0) {
			client.setScreen(null);
			client.gui.getChat().clearMessages(true);
			ringStart = RingHudScenario.ringFrames;
			xpStart = RingHudScenario.xpFrames;
			captured = false;
		}
		if (++ringWait < 12) return false;
		require(com.iso2t.heavyinventories.gui.WeightRingRenderer.verticalOffset(client) == 7, "Remote ring hidden or offset lost");
		require(RingHudScenario.ringFrames > ringStart && RingHudScenario.xpFrames > xpStart, "Remote ring/XP not rendered");
		if (ringWait == 12) net.minecraft.client.Screenshot.grab(Services.PLATFORM.getGameDirectory().toFile(), "ring-" + name + ".png", client.getMainRenderTarget(), 1, message -> captured = true);
		if (!captured) return false;
		ringWait = 0;
		return true;
	}

	private static void send (Minecraft client, float capacity, long revision) {
		client.getConnection().send(new ServerboundCustomPayloadPacket(new ServerConfigUpdatePayload(capacity, "at_ninety_percent", revision)));
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
