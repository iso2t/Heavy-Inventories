package com.iso2t.heavyinventories.test.mixin;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Gui.class)
public interface GuiFeedbackTestAccess {
    @Accessor("overlayMessageString") Component heavyinventories$message();
    @Accessor("overlayMessageTime") int heavyinventories$messageTime();
    @Accessor("overlayMessageTime") void heavyinventories$messageTime(int value);
}
