package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.api.events.PlayerFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientFeedback {
	private ClientFeedback () {
	}

	public static void register () {
		PlayerFeedback.registerJumpNotice(holder -> {
			// The integrated server shares this callback but must never touch the client GUI.
			if (!holder.getPlayer().level().isClientSide()) return;
			var client = Minecraft.getInstance();
			if (holder.getPlayer() != client.player || !holder.allowJumpNotice()) return;
			client.gui.setOverlayMessage(Component.translatable("chat.heavyinventories.no_jump", Component.translatable(holder.isOverEncumbered() ? "chat.heavyinventories.over_encumbered" : "chat.heavyinventories.encumbered")), false);
		});
	}
}
