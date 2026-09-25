package com.iso2t.heavyinventories.test;

import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import com.iso2t.heavyinventories.client.ClientConfigScreen;
import com.iso2t.heavyinventories.config.ClientSettings;
import com.iso2t.heavyinventories.config.ConfigFileManager;
import com.iso2t.heavyinventories.platform.Services;
import com.iso2t.heavyinventories.test.mixin.GuiFeedbackTestAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FeedbackScenario {
    public static int hudFrames;
    private static ClientSettings original;
    private static byte[] originalFile;
    private static Path file;
    private static boolean originalHideGui;
    private static int originalScale, visibilityPhase, phaseTicks, previousFrames;
    public static void checkJump(Minecraft client) {
        var gui = (GuiFeedbackTestAccess) client.gui;
        require(gui.heavyinventories$message() != null && gui.heavyinventories$message().getString().contains("cannot jump"), "No local jump feedback");
        gui.heavyinventories$messageTime(20);
        client.player.jumpFromGround();
        require(gui.heavyinventories$messageTime() == 20, "Repeated jump reset the action bar throttle");
    }
    public static void prepare(Minecraft client) {
        original = ClientSettings.current();
        originalHideGui = client.options.hideGui;
        originalScale = client.options.guiScale().get();
        file = Services.PLATFORM.getGameDirectory().resolve("config/heavyinventories-client.json");
        try {
            originalFile = Files.exists(file) ? Files.readAllBytes(file) : null;
            // Only this disposable test config is replaced; restore original bytes at the end.
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{}");
            var builder = ClientConfigScreen.create();
            var entries = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general")).getEntries();
            int[] colors = {0x123456, 0xABCDEF, 0x010203};
            for (int i = 0; i < 3; i++) {
                var entry = (me.shedaniel.clothconfig2.gui.entries.ColorEntry) entries.get(i + 2);
                entry.setValue(colors[i]);
                entry.save();
            }
            builder.getSavingRunnable().run();
            require(ClientSettings.current().normal() == colors[0] && ClientSettings.current().encumbered() == colors[1]
                    && ClientSettings.current().overloaded() == colors[2], "Color controls changed the wrong preference");
            ConfigFileManager.loadClientConfig();
            require(ClientSettings.current().encumbered() == colors[1], "Color did not persist");
            var fresh = ClientConfigScreen.create().getOrCreateCategory(Component.translatable("category.heavyinventories.general")).getEntries();
            require(((me.shedaniel.clothconfig2.gui.entries.ColorEntry) fresh.get(3)).getValue() == colors[1], "Reopened screen retained stale state");
            ConfigFileManager.saveClientConfig(new ClientSettings(MeasuringSystem.KGS, true, 0xFFFFFF, 0xFFFF55, 0xFF5555));
            client.options.hideGui = false;
            client.setScreen(null);
            hudFrames = 0;
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
    }
    public static boolean verifyVisibility(Minecraft client) {
        if (visibilityPhase == 6) return true;
        if (visibilityPhase != 0 && ++phaseTicks < 5) return false;
        switch (visibilityPhase) {
            case 0 -> { previousFrames = hudFrames; client.options.hideGui = true; }
            case 1 -> {
                require(hudFrames == previousFrames, "F1 did not hide the weight HUD");
                client.options.hideGui = false;
                com.iso2t.heavyinventories.config.ConfigOptions.ENABLE_GUI_OVERLAY = false;
            }
            case 2 -> {
                require(hudFrames == previousFrames, "Overlay toggle did not hide the weight HUD");
                com.iso2t.heavyinventories.config.ConfigOptions.ENABLE_GUI_OVERLAY = true;
                client.setScreen(ClientConfigScreen.create().build());
            }
            case 3 -> {
                require(hudFrames == previousFrames, "HUD rendered over a config screen");
                client.setScreen(null);
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
                com.iso2t.heavyinventories.HeavyInventories.LOGGER.info("GUI COMPATIBILITY PASSED: F1, overlay toggle, screen suppression, GUI scales 1 and 2");
            }
        }
        phaseTicks = 0;
        visibilityPhase++;
        return visibilityPhase == 6;
    }
    public static void restore() {
        try { if (originalFile == null) Files.deleteIfExists(file); else Files.write(file, originalFile); }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
        original.apply();
        var client = Minecraft.getInstance();
        client.options.hideGui = originalHideGui;
        client.options.guiScale().set(originalScale);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
