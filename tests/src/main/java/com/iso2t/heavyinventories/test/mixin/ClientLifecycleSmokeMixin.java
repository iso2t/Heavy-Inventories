package com.iso2t.heavyinventories.test.mixin;

import com.iso2t.heavyinventories.test.ClientLifecycleScenario;
import com.iso2t.heavyinventories.test.plugin.ApiWeightChecks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientLifecycleSmokeMixin {

	@Unique
	private final ClientLifecycleScenario                 heavyinventories$scenario = new ClientLifecycleScenario();

	@Unique
	private       int                                     heavyinventories$startupTicks;

	@Unique
	private       boolean                                 heavyinventories$joined;

	@Unique
	private       net.minecraft.client.gui.screens.Screen heavyinventories$handledUpgrade;

	@Inject(method = "tick", at = @At("TAIL"))
	private void heavyinventories$testClient (CallbackInfo ci) {
		var client = (Minecraft) (Object) this;
		if (client.player == null || !com.iso2t.heavyinventories.player.PlayerHolder.getOrCreate(client.player).hasServerState()) ApiWeightChecks.unavailableClient();
		if (client.player != null) heavyinventories$joined = true;
		if (!heavyinventories$joined) {
			var upgrade = client.gui.screen();
			if (Boolean.getBoolean("heavyinventories.test.allowWorldUpgrade") && upgrade != heavyinventories$handledUpgrade && upgrade != null) {
				var confirm = upgrade instanceof net.minecraft.client.gui.screens.BackupConfirmScreen ? net.minecraft.client.gui.screens.BackupConfirmScreen.BACKUP_AND_JOIN : upgrade instanceof net.minecraft.client.gui.screens.ConfirmScreen && upgrade.getTitle().equals(net.minecraft.network.chat.Component.translatable("upgradeWorld.done")) ? net.minecraft.network.chat.CommonComponents.GUI_YES : null;
				if (confirm != null) {
					for (var child : upgrade.children()) {
						if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().equals(confirm)) {
							heavyinventories$handledUpgrade = upgrade;
							button.onPress(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN, InputConstants.KEYCODE_RETURN, 0));
							break;
						}
					}
				}
			}
			if (++heavyinventories$startupTicks % 200 == 0) {
				var screen = client.gui.screen();
				com.iso2t.heavyinventories.HeavyInventories.LOGGER.info("CLIENT STARTUP WAIT: {}", screen == null ? "no screen" : screen.getClass().getName() + ": " + screen.getTitle().getString());
			}
			if (heavyinventories$startupTicks > 2400) throw new AssertionError("Test client did not join the disposable world/server within two minutes");
		}
		heavyinventories$scenario.tick(client);
	}

}
