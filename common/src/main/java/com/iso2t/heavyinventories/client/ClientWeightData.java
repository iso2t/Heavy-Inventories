package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.network.ItemWeightsPayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Client-thread-only connection data. Server code never reads or writes this store.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientWeightData {

	private static final DefinitionReceiver DEFINITIONS = new DefinitionReceiver(BuiltInRegistries.ITEM::containsKey);

	public static void accept (ItemWeightsPayload payload) {
		DEFINITIONS.accept(payload);
	}

	public static Float weight (Identifier item) {
		return DEFINITIONS.weight(item);
	}

	public static double unitWeight (Identifier item) {
		var weight = weight(item);
		return weight == null ? Double.NaN : weight;
	}

	public static void clear () {
		DEFINITIONS.clear();
	}

	public static long revision () {
		return DEFINITIONS.revision;
	}

	/**
	 * Publishes a complete revision atomically, never a partly received item table.
	 */
	@RequiredArgsConstructor
	public static final class DefinitionReceiver {

		private final Predicate<Identifier>  registeredItem;
		private       Map<Identifier, Float> active    = Map.of();
		private final Map<Identifier, Float> pending   = new HashMap<>();
		private       long                   revision;
		private       long                   pendingRevision;
		private       int                    nextIndex = -1;
		private       int                    chunks;
		private       int                    pendingBytes;

		public void accept (ItemWeightsPayload payload) {
			if (payload.revision() <= revision || payload.revision() < pendingRevision) return;
			if (payload.revision() > pendingRevision) {
				clearPending();
				pendingRevision = payload.revision();
				if (payload.index() != 0) return;
				nextIndex = 0;
				chunks = payload.chunks();
			}
			if (payload.index() != nextIndex || payload.chunks() != chunks || pending.size() + payload.entries().size() > ItemWeightsPayload.MAX_ENTRIES) {
				clearPending();
				return;
			}
			for (var entry : payload.entries()) {
				if (!registeredItem.test(entry.item()) || pending.containsKey(entry.item()) || pendingBytes + entry.sizeBytes() > ItemWeightsPayload.MAX_TABLE_BYTES) {
					clearPending();
					return;
				}
				pending.put(entry.item(), entry.weight());
				pendingBytes += entry.sizeBytes();
			}
			nextIndex++;
			if (nextIndex == chunks) {
				active = Map.copyOf(pending);
				revision = pendingRevision;
				clearPending();
			}
		}

		public Float weight (Identifier item) {
			return active.get(item);
		}

		public void clear () {
			active = Map.of();
			clearPending();
			revision = pendingRevision = 0;
		}

		private void clearPending () {
			pending.clear();
			nextIndex = -1;
			chunks = pendingBytes = 0;
		}

	}

}
