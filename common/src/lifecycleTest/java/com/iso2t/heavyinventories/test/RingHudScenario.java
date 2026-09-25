package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.events.PlayerEvents;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.config.HudMode;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.gui.WeightRingRenderer;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.server.ServerWeightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Opt-in visual checkpoints in disposable worlds, using server-synchronized weights.
 */
public final class RingHudScenario {
	public static int ringFrames, xpFrames, ringsThisFrame;
	public static boolean active;

	private record Case(String name, int percent, int level, int offset, HudMode mode, boolean overlay) {
	}

	private static final List<Case> CASES = cases();
	private static       int        index, phase, frames, beforeRing, beforeXp, originalLevel, waiting;
	private static          CompletableFuture<Void> operation;
	private static volatile boolean                 captured;

	private static List<Case> cases () {
		var cases = new ArrayList<Case>();
		for (int p : new int[] { 0, 25, 50, 89, 90, 99, 100, 125 }) cases.add(new Case("weight-" + p, p, 30, 7, HudMode.RING, true));
		for (int xp : new int[] { 0, 1, 100, 1000 }) cases.add(new Case("xp-" + xp, 90, xp, 7, HudMode.RING, true));
		for (int offset : new int[] { 0, 8, 12, 64 }) cases.add(new Case("offset-" + offset, 100, 1000, offset, HudMode.RING, true));
		cases.add(new Case("numbers", 90, 30, 12, HudMode.NUMBERS, true));
		cases.add(new Case("both", 90, 30, 12, HudMode.BOTH, true));
		cases.add(new Case("off", 90, 30, 12, HudMode.RING, false));
		cases.add(new Case("calculation-limit", 126, 30, 7, HudMode.RING, true));
		return List.copyOf(cases);
	}

	public static boolean tick (Minecraft client, ServerPlayer player) {
		if (index == CASES.size()) {
			if (phase != 4) {
				active = false;
				operation = client.getSingleplayerServer().submit(() -> {
					player.getInventory().clearContent();
					player.setExperienceLevels(originalLevel);
					try {
						ServerWeightState.of(client.getSingleplayerServer()).reload(client.getSingleplayerServer());
					} catch (java.io.IOException e) {
						throw new java.io.UncheckedIOException(e);
					}
					PlayerEvents.onPlayerTick(player);
				});
				phase = 4;
			}
			if (!operation.isDone()) return false;
			operation.join();
			HeavyInventories.LOGGER.info("RING HUD PASSED: 20 synchronized visual checkpoints, thresholds, XP zero/widths, offsets, modes, calculation limit, vanilla XP transform");
			return true;
		}
		var test = CASES.get(index);
		if (phase == 0) {
			HeavyInventories.LOGGER.info("Ring checkpoint {}", test.name());
			client.setScreen(null);
			client.gui.getChat().clearMessages(true);
			client.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
			client.options.guiScale().set(2);
			ConfigOptions.HUD_MODE = test.mode();
			ConfigOptions.RING_VERTICAL_OFFSET = test.offset();
			ConfigOptions.ENABLE_GUI_OVERLAY = test.overlay();
			operation = client.getSingleplayerServer().submit(() -> {
				if (index == 0) originalLevel = player.experienceLevel;
				player.removeAllEffects();
				player.getInventory().clearContent();
				player.getInventory().setItem(0, new ItemStack(Items.STONE));
				if (test.percent() == 126) {
					var nested = new ItemStack(Items.STONE);
					for (int depth = 0; depth < 18; depth++) {
						var box = new ItemStack(Items.SHULKER_BOX);
						box.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(nested)));
						nested = box;
					}
					player.getInventory().setItem(0, nested);
				}
				player.setExperienceLevels(test.level());
				// Send the fixture explicitly; do not depend on vanilla's total-XP dirty check.
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
				var state = ServerWeightState.of(client.getSingleplayerServer());
				state.replace(ServerSettings.DEFAULT, Map.of(Identifier.withDefaultNamespace("stone"), test.percent() * 10f));
				PlayerEvents.onPlayerTick(player);
			});
			active = true;
			phase = 1;
		} else if (phase == 1) {
			if (!operation.isDone()) return false;
			operation.join();
			var holder = PlayerHolder.getOrCreate(client.player);
			float expectedWeight = test.percent() == 126 ? com.iso2t.heavyinventories.api.weight.StackWeight.TOO_COMPLEX : test.percent() * 10f;
			if (holder.getWeight() != expectedWeight || holder.getMaxWeight() != 1000 || client.player.experienceLevel != test.level()) {
				if (++waiting > 100) throw new AssertionError("Ring snapshot mismatch " + test.name() + ": weight=" + holder.getWeight() + ", capacity=" + holder.getMaxWeight() + ", xp=" + client.player.experienceLevel);
				return false;
			}
			waiting = 0;
			if (holder.isEncumbered() != (test.percent() >= 90 && test.percent() < 100) || holder.isOverEncumbered() != (test.percent() >= 100)) throw new AssertionError("Ring threshold fixture differs from gameplay");
			int expected = test.overlay() && test.mode().ring() ? test.offset() : 0;
			if (WeightRingRenderer.verticalOffset(client) != expected) throw new AssertionError("Hidden ring retained XP offset");
			beforeRing = ringFrames;
			beforeXp = xpFrames;
			frames = 0;
			phase = 2;
		} else if (phase == 2) {
			if (++frames < 6) return false;
			boolean ring = test.overlay() && test.mode().ring();
			if ((ringFrames > beforeRing) != ring) throw new AssertionError("Ring registration/visibility mismatch: " + test.name());
			if (test.level() > 0 && xpFrames <= beforeXp) throw new AssertionError("Vanilla XP draw was missing");
			captured = false;
			Screenshot.grab(Services.PLATFORM.getGameDirectory().toFile(), "ring-" + test.name() + ".png", client.getMainRenderTarget(), 1, message -> captured = true);
			phase = 3;
		} else if (phase == 3 && captured) {
			index++;
			phase = 0;
		}
		return false;
	}
}
