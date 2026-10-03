package com.iso2t.heavyinventories.api.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Client initialization only. Ownership resolves by user choice, then descending priority, then ID.
 */
public interface HudRegistration {
	/**
	 * One winner per element; losing integrations do not modify layout or draw a replacement.
	 */
	void owner (Identifier id, HudElement element, int priority, HudIntegration integration);

	/**
	 * Decorations run in stable ID order around the resolved element, only while it is visible.
	 */
	void decorate (Identifier id, HudElement element, Phase phase, Decoration decoration);

	enum Phase {
		BEFORE,
		AFTER
	}

	@FunctionalInterface
	interface Decoration {
		/**
		 * Restore any graphics state you change. Do not retain the graphics object.
		 */
		void render (GuiGraphicsExtractor graphics, HudContext context, HudLayout layout);
	}
}
