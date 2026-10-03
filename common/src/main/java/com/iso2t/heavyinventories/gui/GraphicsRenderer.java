package com.iso2t.heavyinventories.gui;

import com.iso2t.heavyinventories.api.EncumbranceState;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.HudBounds;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.client.ClientWeightData;
import com.iso2t.heavyinventories.client.WeightDisplay;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.player.PlayerHolder;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GraphicsRenderer {

	public static boolean visible (Minecraft client) {
		return ConfigOptions.ENABLE_GUI_OVERLAY && client.player != null && !client.player.isCreative() && !client.player.isSpectator() && !client.gui.hud.isHidden() && client.gui.screen() == null && PlayerHolder.getOrCreate(client.player).hasServerState() && PlayerHolder.getOrCreate(client.player).serverRevision() == ClientWeightData.revision();
	}

	public static void renderGui (GuiGraphicsExtractor graphics, MeasuringSystem measurement, Minecraft client) {
		if (!visible(client)) return;
		if (!ConfigOptions.HUD_MODE.numbers() && PlayerHolder.getOrCreate(client.player).apiSnapshot().carriedWeight().pounds().isPresent()) return;
		WeightHud.render(HudElement.NUMBERS, graphics, client);
	}

	public static HudBounds bounds (Minecraft client, PlayerWeightSnapshot snapshot, MeasuringSystem measure) {
		var main = main(snapshot, measure);
		var status = status(snapshot);
		int width = Math.max(client.font.width(main), status == null ? 0 : client.font.width(status));
		int height = client.font.lineHeight * (status == null ? 1 : 2);
		return new HudBounds(client.getWindow().getGuiScaledWidth() - 4 - width, client.getWindow().getGuiScaledHeight() - 4 - height, width, height);
	}

	public static void drawNumbers (GuiGraphicsExtractor graphics, Minecraft client, int right, int y, PlayerWeightSnapshot snapshot, MeasuringSystem measure) {
		var status = status(snapshot);
		int color = color(snapshot.state() == EncumbranceState.ENCUMBERED, snapshot.state() == EncumbranceState.OVERLOADED);
		if (status != null) {
			draw(graphics, status, right, y, color);
			y += client.font.lineHeight;
		}
		draw(graphics, main(snapshot, measure), right, y, color);
	}

	private static Component main (PlayerWeightSnapshot snapshot, MeasuringSystem measure) {
		return snapshot.carriedWeight().pounds().isEmpty() ? Component.translatable("hud.heavyinventories.calculation_limit") : Component.translatable("hud.heavyinventories.weight", WeightDisplay.number(measure.fromStored(snapshot.carriedWeight().pounds().orElseThrow())), WeightDisplay.weight(snapshot.capacity(), measure), WeightDisplay.number(snapshot.loadRatio().orElseThrow() * 100));
	}

	private static Component status (PlayerWeightSnapshot snapshot) {
		return switch (snapshot.state()) {
			case NORMAL -> null;
			case ENCUMBERED -> Component.translatable("chat.heavyinventories.encumbered");
			case OVERLOADED -> Component.translatable("chat.heavyinventories.over_encumbered");
		};
	}

	public static int color (boolean encumbered, boolean overloaded) {
		return overloaded ? ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR : encumbered ? ConfigOptions.ENCUMBERED_TEXT_COLOR : ConfigOptions.NORMAL_TEXT_COLOR;
	}

	private static void draw (GuiGraphicsExtractor graphics, Component text, int right, int y, int color) {
		graphics.textRenderer().accept(TextAlignment.RIGHT, right, y, text.copy().withStyle(style -> style.withColor(color)));
	}
}
