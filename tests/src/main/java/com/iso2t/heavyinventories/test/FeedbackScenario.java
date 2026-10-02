package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.client.ClientConfigScreen;
import com.iso2t.heavyinventories.client.ConfigScreens;
import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.test.mixin.GuiFeedbackTestAccess;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class FeedbackScenario {
	public static  int            hudFrames;
	private static ClientSettings original;
	private static byte[]         originalFile;
	private static Path           file;
	private static boolean        originalHideGui;
	private static int            originalScale, visibilityPhase, phaseTicks, previousFrames;

	public static void checkJump (Minecraft client) {
		var gui = (GuiFeedbackTestAccess) client.gui.hud;
		require(gui.heavyinventories$message() != null && gui.heavyinventories$message().getString().contains("cannot jump"), "No local jump feedback");
		gui.heavyinventories$messageTime(20);
		client.player.jumpFromGround();
		require(gui.heavyinventories$messageTime() == 20, "Repeated jump reset the action bar throttle");
	}

	public static void prepare (Minecraft client) {
		for (var type : ConfigScreens.SettingsType.values()) {
			ConfigScreens.open(type);
			var first = client.gui.screen();
			require(first != null && first.getTitle().getContents() instanceof TranslatableContents title && title.getKey().equals("title.heavyinventories.config." + type.name().toLowerCase(Locale.ROOT)) && title.getFallback() != null, "Incorrect config screen: " + type);
			ConfigScreens.open(type);
			require(client.gui.screen() != first, "Config screen was reused: " + type);
		}
		var currentScreen = client.gui.screen();
		ConfigScreens.fromString("unknown").ifPresent(ConfigScreens::open);
		require(client.gui.screen() == currentScreen, "Unknown config type changed the screen");
		client.gui.setScreen(null);
		original = ClientSettings.current();
		originalHideGui = client.gui.hud.isHidden();
		originalScale = client.options.guiScale().get();
		file = Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-client.toml");
		require(Files.exists(file), "Client startup did not generate its TOML config");
		try {
			originalFile = Files.exists(file) ? Files.readAllBytes(file) : null;
			// Only this disposable test config is replaced; restore original bytes at the end.
			Files.createDirectories(file.getParent());
			Files.writeString(file, "");
			var screen = ClientConfigScreen.create();
			client.gui.setScreen(screen);
			int[] colors = { 0x123456, 0xABCDEF, 0x010203 };
			String[] paths = { "normal", "encumbered", "overloaded" };
			for (int i = 0; i < 3; i++) ConfigScreenScenario.entry(screen, paths[i]).setValue(colors[i]);
			var offset = ConfigScreenScenario.field(screen, "option.heavyinventories.ring_vertical_offset");
			offset.setValue("65");
			require(screen.validationError().isPresent(), "Offset above limit was accepted");
			screen.saveSelected();
			require(ClientSettings.current().equals(original), "Invalid client draft was saved");
			offset.setValue("12");
			screen.saveSelected();
			require(ClientSettings.current().normal() == colors[0] && ClientSettings.current().encumbered() == colors[1] && ClientSettings.current().overloaded() == colors[2], "Color controls changed the wrong preference");
			ConfigFileManager.loadClientConfig();
			require(ClientSettings.current().encumbered() == colors[1], "Color did not persist");
			require(ClientSettings.current().ringVerticalOffset() == 12, "Ring offset did not persist");
			var fresh = ClientConfigScreen.create();
			require(((int) ConfigScreenScenario.entry(fresh, "encumbered").value() & 0xFFFFFF) == colors[1], "Reopened screen retained stale state");
			require((int) ConfigScreenScenario.entry(fresh, "ringVerticalOffset").value() == 12, "Reopened offset is stale");
			ConfigFileManager.saveClientConfig(new ClientSettings(MeasuringSystem.KGS, true, 0xFFFFFF, 0xFFFF55, 0xFF5555));
			if (client.gui.hud.isHidden()) client.gui.hud.toggle();
			client.gui.setScreen(null);
			hudFrames = 0;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean verifyVisibility (Minecraft client) {
		if (visibilityPhase == 6) return true;
		if (visibilityPhase != 0 && ++phaseTicks < 5) return false;
		switch (visibilityPhase) {
			case 0 -> {
				previousFrames = hudFrames;
				if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
			}
			case 1 -> {
				require(hudFrames == previousFrames, "F1 did not hide the weight HUD");
				if (client.gui.hud.isHidden()) client.gui.hud.toggle();
				ConfigOptions.ENABLE_GUI_OVERLAY = false;
			}
			case 2 -> {
				require(hudFrames == previousFrames, "Overlay toggle did not hide the weight HUD");
				ConfigOptions.ENABLE_GUI_OVERLAY = true;
				client.gui.setScreen(ClientConfigScreen.create());
			}
			case 3 -> {
				require(hudFrames == previousFrames, "HUD rendered over a config screen");
				client.gui.setScreen(null);
				client.options.guiScale().set(1);
			}
			case 4 -> {
				require(hudFrames > previousFrames, "HUD missing at GUI scale 1");
				previousFrames = hudFrames;
				client.options.guiScale().set(2);
			}
			case 5 -> {
				require(hudFrames > previousFrames, "HUD missing at GUI scale 2");
				client.options.guiScale().set(originalScale);
				HeavyInventories.LOGGER.info("GUI COMPATIBILITY PASSED: F1, overlay toggle, screen suppression, GUI scales 1 and 2");
			}
		}
		phaseTicks = 0;
		visibilityPhase++;
		return visibilityPhase == 6;
	}

	public static void restore () {
		try {
			if (originalFile == null) Files.deleteIfExists(file);
			else Files.write(file, originalFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		original.apply();
		var client = Minecraft.getInstance();
		if (client.gui.hud.isHidden() != originalHideGui) client.gui.hud.toggle();
		client.options.guiScale().set(originalScale);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
