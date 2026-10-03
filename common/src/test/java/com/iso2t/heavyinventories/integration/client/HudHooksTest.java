package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.api.client.*;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HudHooksTest {
	private static final HudLayout      RING    = new HudLayout(new HudBounds(100, 200, 16, 16), true, 7);
	private static final HudLayout      NUMBERS = new HudLayout(new HudBounds(180, 210, 50, 18), true, 0);
	private static final HudContext     CONTEXT = new HudContext(Optional.empty(), 320, 240, true, true, true, RING, NUMBERS);
	private static final HudIntegration DEFAULT = new HudIntegration() {
	};

	@Test
	void priorityThenIdSelectOneOwner () {
		var registrations = owners(Map.of(id("z"), owner(10, DEFAULT), id("b"), owner(20, DEFAULT), id("a"), owner(20, DEFAULT)));
		assertEquals(id("a"), new HudHooks().resolve(registrations, CONTEXT, HudElement.RING, "").id());
		assertEquals(id("z"), new HudHooks().resolve(registrations, CONTEXT, HudElement.RING, "fixture:z").id());
	}

	@Test
	void builtinMissingAndHiddenSelectionsDoNotCallPlugins () {
		var broken = new HudIntegration() {
			@Override
			public HudLayout layout (HudContext c, HudElement e, HudLayout o) {
				fail("Owner must not run");
				return o;
			}
		};
		var registrations = owners(Map.of(id("a"), owner(10, broken)));
		var hooks = new HudHooks();
		assertNull(hooks.resolve(registrations, CONTEXT, HudElement.RING, "heavyinventories:default").id());
		assertNull(hooks.resolve(registrations, CONTEXT, HudElement.RING, "fixture:missing").id());
		var hidden = new HudContext(Optional.empty(), 320, 240, false, true, true, new HudLayout(RING.bounds(), false, 0), new HudLayout(NUMBERS.bounds(), false, 0));
		assertFalse(hooks.resolve(registrations, hidden, HudElement.RING, "").layout().visible());
	}

	@Test
	void hiddenRingCannotDisplaceXpAndNumbersCannotOwnXp () {
		var hidden = new HudIntegration() {
			@Override
			public HudLayout layout (HudContext c, HudElement e, HudLayout o) {
				return new HudLayout(o.bounds(), false, 0);
			}
		};
		assertEquals(0, new HudHooks().resolve(owners(Map.of(id("a"), owner(0, hidden))), CONTEXT, HudElement.RING, "").layout().xpOffset());
		var invalid = new HudIntegration() {
			@Override
			public HudLayout layout (HudContext c, HudElement e, HudLayout o) {
				return new HudLayout(o.bounds(), true, 1);
			}
		};
		var registrations = owners(Map.of(id("a"), new ClientRegistrations.Owner(HudElement.NUMBERS, 0, invalid)));
		assertEquals(NUMBERS, new HudHooks().resolve(registrations, CONTEXT, HudElement.NUMBERS, "").layout());
	}

	@Test
	void failedOwnerFallsBackWithoutPromotingAnotherOwner () {
		var calls = new AtomicInteger();
		var broken = new HudIntegration() {
			@Override
			public HudLayout layout (HudContext c, HudElement e, HudLayout o) {
				calls.incrementAndGet();
				throw new IllegalStateException("Expected fixture failure");
			}
		};
		var registrations = owners(Map.of(id("a"), owner(10, broken), id("b"), owner(0, DEFAULT)));
		var hooks = new HudHooks();
		for (int i = 0; i < 3; i++) assertNull(hooks.resolve(registrations, CONTEXT, HudElement.RING, "").id());
		assertEquals(1, calls.get());
		assertEquals(id("b"), hooks.resolve(registrations, CONTEXT, HudElement.RING, "fixture:b").id());
	}

	@Test
	void invalidBoundsAndNullLayoutsDisableTheOwner () {
		for (HudLayout invalid : new HudLayout[] { null, new HudLayout(new HudBounds(Integer.MAX_VALUE, 0, 16, 16), true, 0) }) {
			var plugin = new HudIntegration() {
				@Override
				public HudLayout layout (HudContext c, HudElement e, HudLayout o) {
					return invalid;
				}
			};
			assertEquals(RING, new HudHooks().resolve(owners(Map.of(id("a"), owner(0, plugin))), CONTEXT, HudElement.RING, "").layout());
		}
	}

	private static Identifier id (String name) {
		return Identifier.fromNamespaceAndPath("fixture", name);
	}

	private static ClientRegistrations.Owner owner (int priority, HudIntegration integration) {
		return new ClientRegistrations.Owner(HudElement.RING, priority, integration);
	}

	private static ClientRegistrations owners (Map<Identifier, ClientRegistrations.Owner> owners) {
		return new ClientRegistrations(owners, Map.of(), Map.of());
	}
}
