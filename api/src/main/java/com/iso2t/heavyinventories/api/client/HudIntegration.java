package com.iso2t.heavyinventories.api.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Exclusive layout/render owner for one element. Defaults preserve HI's drawing.
 */
public interface HudIntegration {
	/**
	 * Cannot override the player's global HUD visibility rules. Only called once per element per frame.
	 */
	default HudLayout layout (HudContext context, HudElement element, HudLayout original) {
		return original;
	}

	/**
	 * Override to replace drawing; omit drawDefault() to suppress the built-in element.
	 */
	default void render (GuiGraphicsExtractor graphics, HudContext context, HudLayout layout, HudDrawing drawing) {
		drawing.drawDefault();
	}
}
