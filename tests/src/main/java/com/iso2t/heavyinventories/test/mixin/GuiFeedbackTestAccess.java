package com.iso2t.heavyinventories.test.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface GuiFeedbackTestAccess {

	@Accessor("contextualInfoBar")
	com.mojang.datafixers.util.Pair<?, ?> heavyinventories$contextualBar ();

	@Accessor("overlayMessageString")
	Component heavyinventories$message ();

	@Accessor("overlayMessageTime")
	int heavyinventories$messageTime ();

	@Accessor("overlayMessageTime")
	void heavyinventories$messageTime (int value);

}
