package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.player.PlayerFeedback;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientFeedback {

	public static void register () {
		PlayerFeedback.registerFluidNotice(holder -> {
			if (!holder.getPlayer().level().isClientSide()) return;
			var client = Minecraft.getInstance();
			if (holder.getPlayer() != client.player || !holder.allowJumpNotice()) return;
			client.gui.hud.setOverlayMessage(Component.translatable("chat.heavyinventories.no_swim_up"), false);
		});
		PlayerFeedback.registerJumpNotice(holder -> {
			// The integrated server shares this callback but must never touch the client GUI.
			if (!holder.getPlayer().level().isClientSide()) return;
			var client = Minecraft.getInstance();
			if (holder.getPlayer() != client.player || !holder.allowJumpNotice()) return;
			client.gui.hud.setOverlayMessage(Component.translatable("chat.heavyinventories.no_jump", Component.translatable(holder.isOverEncumbered() ? "chat.heavyinventories.over_encumbered" : "chat.heavyinventories.encumbered")), false);
		});
	}
}
