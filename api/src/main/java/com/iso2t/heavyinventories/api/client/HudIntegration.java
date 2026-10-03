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
	 * Override to replace drawing; omit drawDefault() to suppress the built-in artwork. This does not reset XP
	 * positioning: return a hidden layout with zero offset from layout() to hide the element completely.
	 * Runs only for a visible layout. Graphics and drawing helpers are valid only within this callback.
	 */
	default void render (GuiGraphicsExtractor graphics, HudContext context, HudLayout layout, HudDrawing drawing) {
		drawing.drawDefault();
	}
}
