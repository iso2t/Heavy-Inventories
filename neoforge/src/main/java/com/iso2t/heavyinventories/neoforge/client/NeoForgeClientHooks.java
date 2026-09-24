package com.iso2t.heavyinventories.neoforge.client;

import com.iso2t.heavyinventories.api.movement.ModifyPlayerMove;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.gui.GraphicsRenderer;
import com.iso2t.heavyinventories.neoforge.config.ModClientConfig;
import com.iso2t.heavyinventories.neoforge.config.ModCommonConfig;
import com.iso2t.heavyinventories.neoforge.config.ModServerConfig;
import com.iso2t.heavyinventories.tooltips.Tooltip;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Keeps client event parameter types out of server-loaded event handlers. */
public final class NeoForgeClientHooks {
    private NeoForgeClientHooks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookTooltip);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookPlayerMove);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::hookGui);
    }

    private static void hookTooltip(ItemTooltipEvent event) {
        Tooltip.addTooltips(event.getToolTip(), event.getItemStack());
    }

    private static void hookPlayerMove(MovementInputUpdateEvent event) {
        ModifyPlayerMove.hook(event.getEntity(), event.getInput().keyPresses);
    }

    private static void hookGui(RenderGuiEvent.Post event) {
        GraphicsRenderer.renderGui(event.getGuiGraphics(), ConfigOptions.WEIGHT_MEASURE, Minecraft.getInstance());
    }

    public static void openConfig(String type) {
        switch (type) {
            case "client" -> {
                ModClientConfig.init();
                Minecraft.getInstance().setScreen(ModClientConfig.getBuilder().build());
            }
            case "server" -> {
                ModServerConfig.init();
                Minecraft.getInstance().setScreen(ModServerConfig.getBuilder().build());
            }
            case "common" -> {
                ModCommonConfig.init();
                Minecraft.getInstance().setScreen(ModCommonConfig.getBuilder().build());
            }
        }
    }
}
