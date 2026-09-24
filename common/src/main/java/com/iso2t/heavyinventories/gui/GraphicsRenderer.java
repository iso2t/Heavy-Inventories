package com.iso2t.heavyinventories.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import com.iso2t.heavyinventories.config.ConfigOptions;

public class GraphicsRenderer {

    /**
     * Renders the GUI overlay, displaying the player's weight and encumbrance status.
     * The method checks various conditions (e.g., whether the GUI is enabled, whether the
     * player is in creative mode, or whether other screens are open) before rendering.
     * It calculates the positions of the status and weight lines dynamically based on
     * the screen dimensions and font size.
     *
     * @param graphics the {@code GuiGraphicsExtractor} object responsible for rendering text and other elements on the GUI.
     * @param measurement the {@code MeasuringSystem} used to display weight units and formatting.
     * @param instance the {@code Minecraft} instance providing access to the player's state,
     *                 screen information, and game settings.
     */
    public static void renderGui (GuiGraphicsExtractor graphics, MeasuringSystem measurement, Minecraft instance) {
        if (!ConfigOptions.ENABLE_GUI_OVERLAY) return;
        if (instance.player == null || instance.player.isCreative() || instance.options.hideGui || instance.screen != null) return;

        int width = instance.getWindow().getGuiScaledWidth();
        int height = instance.getWindow().getGuiScaledHeight();
        int margin = 4;
        int lineH = instance.font.lineHeight;

        var holder = PlayerHolder.getOrCreate(instance.player);

        // Bottom line (numbers)
        String main = String.format("%.1f/%.1f %s (%.1f%%)", holder.getWeight(), holder.getMaxWeight(), measurement.getSub(), holder.getEncumberedPercentage());

        // Status line
        Component statusComp = getStatusComponent(holder);
        int statusColor = getStatusColor(holder);

        int mainW = instance.font.width(main);
        int statusW = statusComp == null ? 0 : instance.font.width(statusComp);

        int totalLines = statusComp == null ? 1 : 2;
        int blockH = totalLines * lineH;
        int yTop = height - blockH - margin;

        // Draw status
        if (statusComp != null) {
            int xStatus = width - statusW - margin;
            graphics.textRenderer().accept(TextAlignment.LEFT, xStatus, yTop, statusComp.copy().withStyle(style -> style.withColor(statusColor)));
            yTop += lineH;
        }

        // Draw main
        int mainColor = getTextColor(holder);
        int xMain = width - mainW - margin;
        graphics.textRenderer().accept(TextAlignment.LEFT, xMain, yTop, Component.literal(main).withStyle(style -> style.withColor(mainColor)));
    }

    /**
     * Determines the appropriate status message to display based on the player's encumbrance state.
     *
     * @param holder the PlayerHolder instance that provides the player's current encumbrance status
     * @return a Component representing the status message:
     *         "chat.heavyinventories.over_encumbered" if the player is over encumbered,
     *         "chat.heavyinventories.encumbered" if the player is encumbered,
     *         or null if the player is neither encumbered nor over encumbered
     */
    private static Component getStatusComponent(PlayerHolder holder) {
        if (holder.isOverEncumbered()) {
            return Component.translatable("chat.heavyinventories.over_encumbered");
        } else if (holder.isEncumbered()) {
            return Component.translatable("chat.heavyinventories.encumbered");
        }
        return null;
    }

    /**
     * Determines the appropriate text color based on the player's encumbrance status.
     *
     * @param holder the PlayerHolder instance that provides the player's encumbrance state
     * @return an integer representing the color code corresponding to the player's status:
     *         ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR if the player is over encumbered,
     *         ConfigOptions.ENCUMBERED_TEXT_COLOR if the player is encumbered, or
     *         ConfigOptions.NORMAL_TEXT_COLOR if the player is neither encumbered nor over encumbered
     */
    private static int getStatusColor(PlayerHolder holder) {
        if (holder.isOverEncumbered()) {
            return ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR;
        } else if (holder.isEncumbered()) {
            return ConfigOptions.ENCUMBERED_TEXT_COLOR;
        }
        return ConfigOptions.NORMAL_TEXT_COLOR;
    }

    /**
     * Determines the appropriate text color based on the player's encumbrance state.
     *
     * @param holder the PlayerHolder instance that provides the player's encumbrance state
     * @return an integer representing the color code corresponding to the player's encumbrance status:
     *         ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR if the player is over encumbered,
     *         ConfigOptions.ENCUMBERED_TEXT_COLOR if the player is moderately encumbered,
     *         or ConfigOptions.NORMAL_TEXT_COLOR if the player is neither encumbered nor over encumbered
     */
    private static int getTextColor(PlayerHolder holder) {
        return holder.getEncumberedPercentage() >= 100 ? ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR : holder.getEncumberedPercentage() >= 90 ? ConfigOptions.ENCUMBERED_TEXT_COLOR : holder.getEncumberedPercentage() >= 75 ? ConfigOptions.ENCUMBERED_TEXT_COLOR : ConfigOptions.NORMAL_TEXT_COLOR;
    }

}
