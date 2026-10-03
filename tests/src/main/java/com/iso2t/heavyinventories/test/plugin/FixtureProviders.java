package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.WeightResult;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.iso2t.heavyinventories.api.provider.ContainerContentsProvider;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class FixtureProviders {
	public static int target = -1, count = 4;
	public static ItemStack                              extra         = ItemStack.EMPTY;
	public static String                                 inventoryMode = "normal";
	public static double                                 bonus;
	public static InventoryProvider.SlotSink             retainedSlots;
	public static ContainerContentsProvider.ContentsSink retainedContents;
	public static Identifier                             dimension;

	private static Identifier id (String name) {
		return Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", name);
	}

	public static ItemStack container (String mode) {
		var stack = new ItemStack(Items.CLOCK);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(mode));
		return stack;
	}

	public static ItemStack capacityEquipment () {
		var stack = new ItemStack(Items.BLAZE_ROD);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("hi_capacity"));
		return stack;
	}

	public static void register (PluginRegistration registration) {
		registration.inventory(id("extra"), (player, sink) -> {
			if (player.getId() != target) return;
			retainedSlots = sink;
			switch (inventoryMode) {
				case "throw" -> throw new IllegalStateException("Expected inventory provider failure");
				case "vanilla" -> sink.accept(id("extra/0"), player.getInventory().getItem(0));
				case "reserved" -> sink.accept(Identifier.withDefaultNamespace("inventory/0"), extra);
				case "duplicate" -> {
					sink.accept(id("extra/0"), extra);
					sink.accept(id("extra/0"), extra.copy());
				}
				case "wide" -> {
					for (int i = 0; i < 5000; i++) if (!sink.accept(id("extra/" + i), ItemStack.EMPTY)) break;
				}
				default -> sink.accept(id("extra/0"), extra);
			}
		});
		registration.capacity(id("capacity"), player -> {
			var offhand = player.getOffhandItem();
			if (offhand.is(Items.BLAZE_ROD) && offhand.getHoverName().getString().equals("hi_capacity")) return 125;
			return player.getId() == target ? bonus : 0;
		});
		registration.container(id("clock"), Set.of(BuiltInRegistries.ITEM.getKey(Items.CLOCK)), (level, stack, sink) -> {
			retainedContents = sink;
			dimension = level.dimension().identifier();
			String mode = stack.getHoverName().getString();
			return switch (mode) {
				case "normal" -> sink.accept(new ItemStack(Items.STONE, 4));
				case "external" -> sink.accept(new ItemStack(Items.STONE, count));
				case "nested" -> sink.accept(container("normal"));
				case "cycle" -> sink.accept(stack);
				case "copyCycle" -> sink.accept(stack.copy());
				case "unavailable" -> false;
				case "serverOnly" -> !level.isClientSide() && sink.accept(new ItemStack(Items.STONE, 4));
				case "throw" -> throw new IllegalStateException("Expected container provider failure");
				case "recursive" -> {
					FixturePlugin.weights.stack(level.getServer(), stack);
					yield true;
				}
				case "wide" -> {
					for (int i = 0; i < 5000; i++) if (!sink.accept(ItemStack.EMPTY)) break;
					yield true;
				}
				default -> true;
			};
		});
	}

	public static void server (MinecraftServer server, Consumer<ServerPlayer> update) {
		var weights = FixturePlugin.weights;
		var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "ProviderTest"), ClientInformation.createDefault());
		target = player.getId();
		double stone = weights.item(server, BuiltInRegistries.ITEM.getKey(Items.STONE)).pounds().orElseThrow();
		double clock = weights.item(server, BuiltInRegistries.ITEM.getKey(Items.CLOCK)).pounds().orElseThrow();
		try {
			player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
			extra = new ItemStack(Items.STONE, 3);
			update.accept(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), 5 * stone);
			extra.setCount(5);
			update.accept(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), 7 * stone);
			for (String mode : List.of("duplicate", "reserved", "vanilla", "wide", "throw")) {
				inventoryMode = mode;
				update.accept(player);
				require(weights.player(player).orElseThrow().carriedWeight().status() == WeightResult.Status.INCOMPLETE, "Inventory failure understated weight: " + mode);
			}
			inventoryMode = "normal";
			extra = ItemStack.EMPTY;
			update.accept(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), 2 * stone);
			double base = weights.player(player).orElseThrow().capacity();
			bonus = 250;
			update.accept(player);
			near(weights.player(player).orElseThrow().capacity(), base + 250);
			for (double invalid : new double[] { -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE }) {
				bonus = invalid;
				update.accept(player);
				near(weights.player(player).orElseThrow().capacity(), base);
			}
			bonus = 0;
			player.setItemSlot(EquipmentSlot.OFFHAND, capacityEquipment());
			update.accept(player);
			near(weights.player(player).orElseThrow().capacity(), base + 125);
			player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			update.accept(player);
			near(weights.player(player).orElseThrow().capacity(), base);
			var replacement = new ServerPlayer(server, server.overworld(), player.getGameProfile(), ClientInformation.createDefault());
			update.accept(replacement);
			near(weights.player(replacement).orElseThrow().carriedWeight().pounds().orElseThrow(), 0);
			near(weights.player(replacement).orElseThrow().capacity(), base);

			var normal = container("normal");
			normal.setCount(2);
			normal.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 64))));
			near(weights.stack(server, normal).pounds().orElseThrow(), 2 * (clock + 4 * stone));
			near(weights.stack(server, container("nested")).pounds().orElseThrow(), 2 * clock + 4 * stone);
			var box = new ItemStack(Items.SHULKER_BOX);
			box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(container("normal"))));
			near(weights.stack(server, box).pounds().orElseThrow(), weights.item(server, BuiltInRegistries.ITEM.getKey(Items.SHULKER_BOX)).pounds().orElseThrow() + clock + 4 * stone);
			for (String mode : List.of("cycle", "copyCycle", "wide", "unavailable", "throw", "recursive"))
				require(weights.stack(server, container(mode)).status() == WeightResult.Status.INCOMPLETE, "Container failure understated weight: " + mode);
			weights.stack(server.getLevel(net.minecraft.world.level.Level.NETHER), container("normal"));
			require(dimension.equals(Identifier.withDefaultNamespace("the_nether")), "Container used the wrong level");
			player.getInventory().clearContent();
			player.getInventory().setItem(0, container("external"));
			count = 4;
			update.accept(player);
			count = 8;
			weights.invalidate(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), clock + 4 * stone);
			update.accept(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), clock + 8 * stone);
			count = 12;
			player.tickCount += 20;
			update.accept(player);
			near(weights.player(player).orElseThrow().carriedWeight().pounds().orElseThrow(), clock + 12 * stone);
			try {
				retainedSlots.accept(id("late"), ItemStack.EMPTY);
				throw new AssertionError("Inventory sink remained open");
			} catch (IllegalStateException expected) {
			}
			try {
				retainedContents.accept(ItemStack.EMPTY);
				throw new AssertionError("Contents sink remained open");
			} catch (IllegalStateException expected) {
			}
			LogUtils.getLogger().info("API PROVIDERS SERVER PASSED: extra slots, mutation, duplicate/vanilla ownership, limits/failures, capacity/equipment, entity replacement, nested custom contents, level context, invalidation and fallback refresh");
		} finally {
			reset();
		}
	}

	public static void reset () {
		target = -1;
		count = 4;
		extra = ItemStack.EMPTY;
		bonus = 0;
		inventoryMode = "normal";
		retainedSlots = null;
		retainedContents = null;
	}

	private static void near (double actual, double expected) {
		require(Math.abs(actual - expected) < 0.001, actual + " != " + expected);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
