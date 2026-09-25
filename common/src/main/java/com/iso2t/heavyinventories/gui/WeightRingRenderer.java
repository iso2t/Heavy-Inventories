package com.iso2t.heavyinventories.gui;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.ConfigOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class WeightRingRenderer {
	private static final Identifier FRAME = Identifier.fromNamespaceAndPath(HeavyInventories.MOD_ID, "textures/gui/hud_ring.png");

	private WeightRingRenderer () {
	}

	public static boolean visible (Minecraft client) {
		return ConfigOptions.HUD_MODE.ring() && GraphicsRenderer.visible(client);
	}

	public static int verticalOffset (Minecraft client) {
		return visible(client) ? ConfigOptions.RING_VERTICAL_OFFSET : 0;
	}

	public static void render (GuiGraphicsExtractor graphics, Minecraft client) {
		if (!visible(client)) return;
		var holder = PlayerHolder.getOrCreate(client.player);
		int rows = WeightRingGeometry.filledRows(holder.getWeight(), holder.getMaxWeight());
		int color = WeightRingGeometry.color(holder.isEncumbered(), holder.isOverEncumbered());
		int x = graphics.guiWidth() / 2 - 8;
		int y = graphics.guiHeight() - 39 - ConfigOptions.RING_VERTICAL_OFFSET;
		// One rectangle per interior row preserves the pixel outline without a shader or scissor state.
		for (int row = 1; row <= 14; row++) {
			int left = WeightRingGeometry.left(row);
			graphics.fill(x + left, y + row, x + 16 - left, y + row + 1, row >= 15 - rows ? color : WeightRingGeometry.EMPTY);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, y, 0, 0, 16, 16, 16, 16);
	}

	/**
	 * Wrap the existing vanilla layer so font, outline, and other layer wrappers are preserved.
	 */
	public static void experienceLevel (GuiGraphicsExtractor graphics, Minecraft client, Runnable vanilla) {
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(0, -verticalOffset(client));
			vanilla.run();
		} finally {
			graphics.pose().popMatrix();
		}
	}
}
