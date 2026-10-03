package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.config.HudMode;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.player.PlayerEvents;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.test.plugin.FixtureHud;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.joml.Matrix3x2f;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class HudApiScenario {
	private static final List<String> CASES = List.of("auto", "selected", "builtin", "missing", "move", "hide", "replace", "helpers", "numbers", "ring", "off", "screen", "f1", "scale1", "scale2", "xp0", "layoutFailure", "renderFailure", "doubleDefault", "popParent", "decorationFailure", "restored", "limitedReplacement");
	public static        boolean      active;
	public static        int          nativeRings, nativeNumbers, xpCalls, ringX, ringY;
	private static int index, ticks, frames, startPrimary, startSecondary, startNumbers, startBefore, startAfter, startRenders;
	private static int scale, level;
	private static          CompletableFuture<Void> inventoryChange;
	private static          int                     waiting;
	private static          boolean                 captureRequested;
	private static volatile boolean                 captured;
	private static          Matrix3x2f              pose;
	private static          GuiGraphicsExtractor    frameGraphics;
	private static          ClientSettings          settings;

	public static boolean tick (Minecraft client) {
		if (index == CASES.size()) {
			active = FixtureHud.active = false;
			if (inventoryChange == null) inventoryChange = client.getSingleplayerServer().submit(() -> {
				var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
				player.getInventory().clearContent();
				PlayerEvents.onPlayerTick(player);
			});
			if (!inventoryChange.isDone()) return false;
			inventoryChange.join();
			settings.apply();
			client.options.guiScale().set(scale);
			client.player.experienceLevel = level;
			LogUtils.getLogger().info("API HUD PASSED: external owner/decorations, shared frame layout, XP exactly once, move/hide/replace/helpers, preferences, visibility, scale, scoped graphics, isolated failures");
			return true;
		}
		if (CASES.get(index).equals("limitedReplacement") && ticks == 0) {
			if (inventoryChange == null) inventoryChange = client.getSingleplayerServer().submit(() -> {
				var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
				var nested = new ItemStack(Items.STONE);
				for (int depth = 0; depth < 18; depth++) {
					var box = new ItemStack(Items.SHULKER_BOX);
					box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(nested)));
					nested = box;
				}
				player.getInventory().setItem(0, nested);
				PlayerEvents.onPlayerTick(player);
			});
			if (++waiting > 200) throw new AssertionError("Incomplete weight did not synchronize");
			if (!inventoryChange.isDone()) return false;
			inventoryChange.join();
			var snapshot = PlayerHolder.getOrCreate(client.player).apiSnapshot();
			if (snapshot == null || snapshot.carriedWeight().pounds().isPresent()) return false;
		}
		if (ticks == 0) {
			if (settings == null) {
				settings = ClientSettings.current();
				scale = client.options.guiScale().get();
				level = client.player.experienceLevel;
			}
			prepare(client);
		}
		if (++ticks < 12 || frames < 2) return false;
		var name = CASES.get(index);
		if (List.of("move", "replace", "limitedReplacement").contains(name)) {
			if (!captureRequested) {
				captureRequested = true;
				Screenshot.grab(Services.PLATFORM.getGameDirectory().toFile(), "api-hud-" + name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> captured = true);
			}
			if (!captured) return false;
		}
		boolean failure = List.of("layoutFailure", "renderFailure", "doubleDefault", "popParent", "decorationFailure").contains(name);
		if (failure) require(FixtureHud.failures == 1, "Failing hook was not disabled: " + name + " calls=" + FixtureHud.failures);
		if (FixtureHud.retainedDrawing != null) {
			try {
				FixtureHud.retainedDrawing.drawDefault();
				throw new AssertionError("Retained drawing helper remained usable");
			} catch (IllegalStateException expected) {
			}
		}
		LogUtils.getLogger().info("API HUD checkpoint {} ({} frames)", name, frames);
		active = FixtureHud.active = false;
		ticks = 0;
		inventoryChange = null;
		index++;
		return false;
	}

	private static void prepare (Minecraft client) {
		FixtureHud.reset();
		FixtureHud.mode = FixtureHud.Mode.DEFAULT;
		ConfigOptions.ENABLE_GUI_OVERLAY = true;
		ConfigOptions.HUD_MODE = HudMode.BOTH;
		ConfigOptions.RING_VERTICAL_OFFSET = 7;
		ConfigOptions.RING_HUD_OWNER = "";
		ConfigOptions.NUMBERS_HUD_OWNER = "";
		client.gui.setScreen(null);
		if (client.gui.hud.isHidden()) client.gui.hud.toggle();
		client.player.experienceLevel = 30;
		switch (CASES.get(index)) {
			case "selected" -> ConfigOptions.RING_HUD_OWNER = FixtureHud.id("other_ring").toString();
			case "builtin" -> ConfigOptions.RING_HUD_OWNER = "heavyinventories:default";
			case "missing" -> ConfigOptions.RING_HUD_OWNER = "missing:renderer";
			case "move" -> FixtureHud.mode = FixtureHud.Mode.MOVE;
			case "hide" -> FixtureHud.mode = FixtureHud.Mode.HIDE;
			case "replace" -> FixtureHud.mode = FixtureHud.Mode.REPLACE;
			case "limitedReplacement" -> {
				FixtureHud.mode = FixtureHud.Mode.REPLACE;
				ConfigOptions.HUD_MODE = HudMode.RING;
			}
			case "helpers" -> FixtureHud.mode = FixtureHud.Mode.HELPERS;
			case "numbers" -> ConfigOptions.HUD_MODE = HudMode.NUMBERS;
			case "ring" -> ConfigOptions.HUD_MODE = HudMode.RING;
			case "off" -> ConfigOptions.ENABLE_GUI_OVERLAY = false;
			case "screen" -> client.gui.setScreen(new PauseScreen(false));
			case "f1" -> client.gui.hud.toggle();
			case "scale1" -> client.options.guiScale().set(1);
			case "scale2" -> client.options.guiScale().set(2);
			case "xp0" -> client.player.experienceLevel = 0;
			case "layoutFailure" -> {
				FixtureHud.mode = FixtureHud.Mode.FAIL_LAYOUT;
				ConfigOptions.RING_HUD_OWNER = FixtureHud.id("layout_ring").toString();
				ConfigOptions.NUMBERS_HUD_OWNER = "heavyinventories:default";
			}
			case "renderFailure" -> {
				FixtureHud.mode = FixtureHud.Mode.FAIL_RENDER;
				ConfigOptions.RING_HUD_OWNER = FixtureHud.id("failure_ring").toString();
			}
			case "doubleDefault" -> {
				FixtureHud.mode = FixtureHud.Mode.DOUBLE_DEFAULT;
				ConfigOptions.RING_HUD_OWNER = FixtureHud.id("double_ring").toString();
			}
			case "popParent" -> {
				FixtureHud.mode = FixtureHud.Mode.POP_PARENT;
				ConfigOptions.RING_HUD_OWNER = FixtureHud.id("pop_ring").toString();
			}
			case "decorationFailure" -> FixtureHud.mode = FixtureHud.Mode.FAIL_DECORATION;
		}
		captureRequested = captured = false;
		frames = 0;
		active = FixtureHud.active = true;
	}

	public static void begin (GuiGraphicsExtractor graphics) {
		if (!active) return;
		frameGraphics = graphics;
		nativeRings = nativeNumbers = xpCalls = 0;
		startPrimary = FixtureHud.primaryLayouts;
		startSecondary = FixtureHud.secondaryLayouts;
		startNumbers = FixtureHud.numberLayouts;
		startBefore = FixtureHud.before;
		startAfter = FixtureHud.after;
		startRenders = FixtureHud.renders;
		pose = new Matrix3x2f(graphics.pose());
		graphics.enableScissor(0, 0, graphics.guiWidth(), graphics.guiHeight());
	}

	public static void end () {
		if (!active) return;
		var graphics = frameGraphics;
		frameGraphics = null;
		require(graphics.pose().equals(pose), "Hook corrupted parent pose");
		require(!graphics.containsPointInScissor(-1, -1), "Hook removed parent scissor");
		graphics.disableScissor();
		String name = CASES.get(index);
		boolean hidden = List.of("off", "screen", "f1").contains(name);
		boolean ring = !hidden && !name.equals("numbers") && !name.equals("hide");
		int expectedXpCalls = name.equals("f1") || name.equals("xp0") ? 0 : 1;
		require(xpCalls == expectedXpCalls, "Vanilla XP invocation count " + xpCalls + " in " + name);
		int expectedRings = ring && !name.equals("replace") && !name.equals("limitedReplacement") ? 1 : 0;
		if (frames == 0 && (name.equals("renderFailure") || name.equals("popParent"))) expectedRings = 0;
		require(nativeRings == expectedRings, "Native ring draws " + nativeRings + " expected " + expectedRings + " in " + name);
		int expectedNumbers = hidden || name.equals("ring") ? 0 : name.equals("helpers") ? 2 : 1;
		require(nativeNumbers == expectedNumbers, "Native number draws " + nativeNumbers + " in " + name);
		require(FixtureHud.primaryLayouts - startPrimary + FixtureHud.secondaryLayouts - startSecondary <= 1, "Ring layout resolved more than once per frame");
		require(FixtureHud.numberLayouts - startNumbers <= 1, "Number layout resolved more than once per frame");
		if (hidden) require(FixtureHud.before == startBefore && FixtureHud.renders == startRenders && FixtureHud.after == startAfter, "Hidden HUD dispatched callbacks");
		if (name.equals("selected")) require(FixtureHud.secondaryLayouts == startSecondary + 1 && FixtureHud.primaryLayouts == startPrimary, "Explicit owner preference ignored");
		if (name.equals("auto")) require(FixtureHud.primaryLayouts == startPrimary + 1 && FixtureHud.secondaryLayouts == startSecondary, "Priority owner ignored");
		if (name.equals("move")) {
			require(ringX == 18 && ringY == 18, "Native ring did not move");
			require(FixtureHud.numbers.bounds().x() == 12 && FixtureHud.numbers.bounds().y() == 12, "Numeric layout did not move independently");
		}
		if (name.equals("auto") || name.equals("move") || name.equals("replace")) require(FixtureHud.order.equals(List.of("a", "b", "owner", "after")), "Decoration order differs: " + FixtureHud.order);
		frames++;
	}

	public static void xp (GuiGraphicsExtractor graphics) {
		if (!active) return;
		xpCalls++;
		int expected = List.of("off", "screen", "f1", "move", "hide", "numbers").contains(CASES.get(index)) ? 0 : 7;
		require(graphics.pose().m21() == -expected, "XP used a different layout: " + graphics.pose().m21());
	}

	private static void require (boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
