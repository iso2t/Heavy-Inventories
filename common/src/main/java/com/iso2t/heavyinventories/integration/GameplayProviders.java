package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.provider.InventoryProvider;
import com.iso2t.heavyinventories.weight.StackWeight;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class GameplayProviders {
	private static final ThreadLocal<Boolean>       CALLBACK    = ThreadLocal.withInitial(() -> false);
	private static final Map<String, Long>          NEXT_REPORT = new ConcurrentHashMap<>();
	private final        CommonRegistrations        registrations;
	private final        Map<Identifier, Container> containers  = new HashMap<>();

	public record Container(Identifier id, CommonRegistrations.Container registration) {
	}

	public record Inventory(List<Identifier> slots, List<ItemStack> stacks, boolean complete) {
	}

	public GameplayProviders (CommonRegistrations registrations) {
		this.registrations = registrations;
		registrations.containers().forEach((id, value) -> value.items().forEach(item -> containers.put(item, new Container(id, value))));
	}

	public Container container (Identifier item) {
		return containers.get(item);
	}

	public static void checkCalculation () {
		if (CALLBACK.get()) throw new IllegalStateException("Providers cannot start another weight calculation");
	}

	public static <T> T callback (Supplier<T> operation) {
		boolean previous = CALLBACK.get();
		CALLBACK.set(true);
		try {
			return operation.get();
		} finally {
			if (previous) CALLBACK.set(true);
			else CALLBACK.remove();
		}
	}

	public static void failure (String kind, Identifier id, Throwable error) {
		String key = kind + ":" + id;
		long now = System.nanoTime();
		NEXT_REPORT.compute(key, (ignored, next) -> {
			if (next != null && now - next < 0) return next;
			HeavyInventories.LOGGER.error("HI {} provider {} failed; {}", kind, id, kind.equals("capacity") ? "no capacity bonus applied" : "weight calculation is incomplete", error);
			return now + 60_000_000_000L;
		});
	}

	public Inventory inventory (ServerPlayer player, List<ItemStack> vanilla) {
		checkCalculation();
		if (registrations.inventories().isEmpty()) return new Inventory(List.of(), vanilla, true);
		var slots = new TreeMap<Identifier, ItemStack>(Comparator.comparing(Identifier::toString));
		Set<ItemStack> identities = Collections.newSetFromMap(new IdentityHashMap<>());
		vanilla.stream().filter(stack -> !stack.isEmpty()).forEach(identities::add);
		for (var entry : registrations.inventories().entrySet()) {
			try (var sink = new Slots(entry.getKey(), slots, identities)) {
				callback(() -> {
					entry.getValue().collect(player, sink);
					return null;
				});
				if (!sink.complete) return new Inventory(List.of(), List.of(), false);
			} catch (RuntimeException | LinkageError e) {
				failure("inventory", entry.getKey(), e);
				return new Inventory(List.of(), List.of(), false);
			}
		}
		var stacks = new ArrayList<>(vanilla);
		stacks.addAll(slots.values());
		return new Inventory(List.copyOf(slots.keySet()), stacks, true);
	}

	public float capacity (ServerPlayer player, float existingCapacity) {
		checkCalculation();
		float total = 0;
		for (var entry : registrations.capacities().entrySet()) {
			try {
				double value = callback(() -> entry.getValue().bonus(player));
				float next = (float) (total + value);
				if (!Double.isFinite(value) || value < 0 || !Float.isFinite(next) || (double) existingCapacity + next > Float.MAX_VALUE)
					throw new IllegalArgumentException("Capacity bonus must be finite, nonnegative, and fit the total capacity");
				total = next;
			} catch (RuntimeException | LinkageError e) {
				failure("capacity", entry.getKey(), e);
			}
		}
		return total;
	}

	private static final class Slots implements InventoryProvider.SlotSink, AutoCloseable {
		private final Identifier                 owner;
		private final Thread                     thread   = Thread.currentThread();
		private       Map<Identifier, ItemStack> slots;
		private       Set<ItemStack>             identities;
		private       boolean                    complete = true;

		private Slots (Identifier owner, Map<Identifier, ItemStack> slots, Set<ItemStack> identities) {
			this.owner = owner;
			this.slots = slots;
			this.identities = identities;
		}

		@Override
		public boolean accept (Identifier slot, ItemStack stack) {
			if (slots == null || thread != Thread.currentThread()) throw new IllegalStateException("Inventory sink is callback-scoped");
			if (!complete) return false;
			if (slots.size() >= StackWeight.MAX_VISITS) return complete = false;
			if (slot == null || !slot.getNamespace().equals(owner.getNamespace()) || slots.containsKey(slot) || stack == null || !stack.isEmpty() && !identities.add(stack)) {
				complete = false;
				throw new IllegalArgumentException("Slot must be uniquely owned, namespaced to its provider, and separate from vanilla slots");
			}
			slots.put(slot, stack.copy());
			return true;
		}

		@Override
		public void close () {
			slots = null;
			identities = null;
		}
	}
}
