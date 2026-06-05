package iso2t.hi.server;

import iso2t.hi.core.HeavyInventories;
import iso2t.hi.weight.PlayerWeight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeavyInventories.MODID)
public final class ServerEvents {

	private ServerEvents() {}

	@SubscribeEvent
	public static void onPlayerLogin (PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		PlayerWeight.recalculate(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		PlayerWeight.recalculate(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		PlayerWeight.recalculate(player);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		PlayerWeight.recalculate(player);
	}

	@SubscribeEvent
	public static void onItemPickup (ItemEntityPickupEvent.Pre event) {
		if (!(event.getPlayer() instanceof ServerPlayer player)) {
			return;
		}

		HeavyInventories.getInstance().getCurrentServer().execute(() -> PlayerWeight.recalculate(player));
		//player.server.execute(() -> PlayerWeight.recalculate(player));
	}

	@SubscribeEvent
	public static void onPlayerTick (PlayerTickEvent.Post event) {
		Player player = event.getEntity();

		if (player.level().isClientSide()) {
			return;
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		PlayerWeight.recalculate(serverPlayer);
		PlayerWeight.applyEncumbrance(serverPlayer);
	}
}
