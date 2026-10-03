package com.iso2t.heavyinventories.gui;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.EncumbranceState;
import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.config.ConfigOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightRingRenderer {
	private static final Identifier FRAME = HeavyInventories.get("textures/gui/hud_ring.png");

	public static boolean visible (Minecraft client) {
		return ConfigOptions.HUD_MODE.ring() && GraphicsRenderer.visible(client);
	}

	public static int verticalOffset (Minecraft client) {
		return WeightHud.xpOffset(client);
	}

	public static void render (GuiGraphicsExtractor graphics, Minecraft client) {
		if (!visible(client)) return;
		WeightHud.render(HudElement.RING, graphics, client);
	}

	public static void draw (GuiGraphicsExtractor graphics, int x, int y, PlayerWeightSnapshot snapshot) {
		int rows = snapshot.carriedWeight().pounds().isEmpty() ? 14 : WeightRingGeometry.filledRows(snapshot.carriedWeight().pounds().orElseThrow(), snapshot.capacity());
		int color = WeightRingGeometry.color(snapshot.state() == EncumbranceState.ENCUMBERED, snapshot.state() == EncumbranceState.OVERLOADED);
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
			graphics.pose().translate(0, -(float) verticalOffset(client));
			vanilla.run();
		} finally {
			graphics.pose().popMatrix();
		}
	}
}
