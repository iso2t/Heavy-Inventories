package iso2t.hi.client;

import iso2t.hi.core.HeavyInventories;
import iso2t.hi.weight.PlayerWeight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = HeavyInventories.MODID, value = Dist.CLIENT)
public final class ClientEvents {

	private ClientEvents () {
	}

	@SubscribeEvent
	public static void onClientTick (ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null) {
			return;
		}

		float currentWeight = PlayerWeight.getCurrentWeight(player);
		float maxWeight = PlayerWeight.getMaxWeight(player);
		int encumbranceLevel = PlayerWeight.getEncumbranceLevel(player);
	}
}
