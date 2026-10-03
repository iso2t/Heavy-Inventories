package com.iso2t.heavyinventories.api.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing helpers scoped to a render callback; helpers never invoke integration hooks recursively.
 */
public interface HudDrawing {
	/**
	 * Draw the owned element at its resolved layout. At most one call is allowed per owner callback.
	 */
	void drawDefault ();

	/**
	 * Draw HI's native 16x16 ring with its current fill and colors at the supplied top-left position.
	 */
	void ring (GuiGraphicsExtractor graphics, int x, int y, PlayerWeightSnapshot player);

	/**
	 * Draw the weight readout, right-aligned at x, beginning at y, using local display preferences.
	 */
	void numbers (GuiGraphicsExtractor graphics, int x, int y, PlayerWeightSnapshot player);
}
