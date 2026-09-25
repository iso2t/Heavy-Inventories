package com.iso2t.heavyinventories.gui;

import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import com.iso2t.heavyinventories.api.weight.StackWeight;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;

public final class GraphicsRenderer {
	private GraphicsRenderer () {
	}

	public static boolean visible (Minecraft client) {
		return ConfigOptions.ENABLE_GUI_OVERLAY && client.player != null && !client.player.isCreative() && !client.player.isSpectator() && !client.options.hideGui && client.screen == null && PlayerHolder.getOrCreate(client.player).hasServerState();
	}

	public static void renderGui (GuiGraphicsExtractor graphics, MeasuringSystem measurement, Minecraft client) {
		if (!visible(client)) return;
		var holder = PlayerHolder.getOrCreate(client.player);
		if (!ConfigOptions.HUD_MODE.numbers() && holder.getWeight() != StackWeight.TOO_COMPLEX) return;
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		int lineH = client.font.lineHeight;
		Component main = holder.getWeight() == StackWeight.TOO_COMPLEX ? Component.translatable("hud.heavyinventories.calculation_limit") : Component.translatable("hud.heavyinventories.weight", WeightDisplay.number(measurement.fromStored(holder.getWeight())), WeightDisplay.weight(holder.getMaxWeight(), measurement), WeightDisplay.number(holder.getEncumberedPercentage()));
		Component status = holder.isOverEncumbered() ? Component.translatable("chat.heavyinventories.over_encumbered") : holder.isEncumbered() ? Component.translatable("chat.heavyinventories.encumbered") : null;
		int color = color(holder.isEncumbered(), holder.isOverEncumbered());
		int y = height - (status == null ? 1 : 2) * lineH - 4;
		if (status != null) {
			draw(graphics, client, status, width, y, color);
			y += lineH;
		}
		draw(graphics, client, main, width, y, color);
	}

	public static int color (boolean encumbered, boolean overloaded) {
		return overloaded ? ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR : encumbered ? ConfigOptions.ENCUMBERED_TEXT_COLOR : ConfigOptions.NORMAL_TEXT_COLOR;
	}

	private static void draw (GuiGraphicsExtractor graphics, Minecraft client, Component text, int width, int y, int color) {
		graphics.textRenderer().accept(TextAlignment.RIGHT, width - 4, y, text.copy().withStyle(style -> style.withColor(color)));
	}
}
