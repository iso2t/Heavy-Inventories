package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.integration.Notifications;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.lang.ref.WeakReference;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientNotifications {

	private static final ClientNotifications        INSTANCE      = new ClientNotifications();
	private final        ClientWeightAccess         weights       = new ClientWeightAccess();
	private final        Notifications              notifications = new Notifications();
	private              PlayerWeightSnapshot       previous;
	private              WeakReference<LocalPlayer> player        = new WeakReference<>(null);

	public static void tick () {
		var client = Minecraft.getInstance();
		INSTANCE.publish(client.player, INSTANCE.weights.player());
	}

	public static void disconnected () {
		if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Client notifications require the client thread");
		INSTANCE.publish(null, Optional.empty());
		INSTANCE.notifications.clear();
	}

	private void publish (LocalPlayer currentPlayer, Optional<PlayerWeightSnapshot> current) {
		var snapshot = current.orElse(null);
		if ((snapshot == null || currentPlayer == player.get()) && Notifications.sameValues(previous, snapshot)) return;
		previous = snapshot;
		player = new WeakReference<>(snapshot == null ? null : currentPlayer);
		notifications.dispatch("client player", ClientPlugins.INSTANCE.registrations().playerChanged(), listener -> listener.accept(current));
	}

}
