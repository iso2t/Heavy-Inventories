package iso2t.hi.client;

import iso2t.hi.core.HeavyInventories;
import iso2t.hi.weight.PlayerWeight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = HeavyInventories.MODID, value = Dist.CLIENT)
public final class WeightHudOverlay {

	@SubscribeEvent
	public static void onRenderGui (RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.options.hideGui) {
			return;
		}

		float current = PlayerWeight.getCurrentWeight(player);
		float max = PlayerWeight.getMaxWeight(player);
		int level = PlayerWeight.getEncumbranceLevel(player);

		var graphics = event.getGuiGraphics();

		String text = String.format("Weight: %.1f / %.1f", current, max);
		System.out.println(level);

		if (level == 1) {
			text += " - Encumbered";
		} else if (level == 2) {
			text += " - Overencumbered";
		}

		graphics.textRenderer().accept(TextAlignment.LEFT, event.getGuiGraphics().guiWidth() / 2, event.getGuiGraphics().guiHeight() / 2, Component.literal(text));
	}
}
