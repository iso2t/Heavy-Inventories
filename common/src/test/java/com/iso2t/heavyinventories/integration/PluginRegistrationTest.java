package com.iso2t.heavyinventories.integration;

import com.iso2t.heavyinventories.api.ServerWeights;
import com.iso2t.heavyinventories.api.client.*;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.iso2t.heavyinventories.integration.client.ClientPlugins;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PluginRegistrationTest {

	private static final ServerWeights SERVER = unusedService(ServerWeights.class);
	private static final ClientWeights CLIENT = unusedService(ClientWeights.class);

	@Test
	void registrationsAreOrderedImmutableAndClosed () {
		var calls = new ArrayList<String>();
		var retained = new AtomicReference<PluginRegistration>();
		var registry = new CommonPlugins(SERVER);
		registry.initialize(List.of(plugin("z", r -> calls.add("z")), plugin("a", r -> {
			calls.add("a");
			assertSame(SERVER, r.weights());
			retained.set(r);
			r.capacity(id("z"), p -> 1);
			r.capacity(id("a"), p -> 2);
		})));
		assertEquals(List.of("a", "z"), calls);
		assertEquals(List.of(id("a"), id("z")), List.copyOf(registry.registrations().capacities().keySet()));
		assertThrows(UnsupportedOperationException.class, () -> registry.registrations().capacities().clear());
		assertThrows(IllegalStateException.class, () -> retained.get().capacity(id("late"), p -> 1));
		assertThrows(IllegalStateException.class, () -> retained.get().weights());
		assertThrows(IllegalStateException.class, () -> registry.initialize(List.of()));
	}

	@Test
	void commonFailureRollsBackEveryPlugin () {
		var registry = new CommonPlugins(SERVER);
		var failure = assertThrows(IllegalStateException.class, () -> registry.initialize(List.of(plugin("a", r -> r.capacity(id("a"), p -> 1)), plugin("b", r -> {
			r.capacity(id("b"), p -> 1);
			throw new IllegalArgumentException("broken plugin");
		}))));
		assertTrue(failure.getMessage().contains("fixture:b"));
		assertTrue(registry.registrations().capacities().isEmpty());
	}

	@Test
	void catchingInvalidRegistrationDoesNotCommitPartialState () {
		var registry = new CommonPlugins(SERVER);
		assertThrows(IllegalStateException.class, () -> registry.initialize(List.of(plugin("a", r -> {
			r.capacity(id("a"), p -> 1);
			assertThrows(IllegalArgumentException.class, () -> r.capacity(id("a"), p -> 2));
		}))));
		assertTrue(registry.registrations().capacities().isEmpty());
	}

	@Test
	void commonDuplicateIdsAndForeignNamespacesAreFatal () {
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(plugin("same", r -> {
		}), plugin("same", r -> {
		}))));
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(plugin("a", r -> r.capacity(Identifier.fromNamespaceAndPath("other", "bonus"), p -> 1)))));
		var wrongOwner = new PluginCandidate<>(Set.of("other"), "fixture.Foreign", () -> plugin("a", r -> {
		}).factory().get());
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(wrongOwner)));
	}

	@Test
	void registrationsCannotClaimTheSameIdOrContainerItem () {
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(plugin("a", r -> r.capacity(id("same"), p -> 1)), plugin("b", r -> r.capacity(id("same"), p -> 2)))));
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(plugin("a", r -> r.container(id("a"), Set.of(id("bag")), (level, stack, sink) -> true)), plugin("b", r -> r.container(id("b"), Set.of(id("bag")), (level, stack, sink) -> true)))));
	}

	@Test
	void workerThreadCannotRegister () {
		var registry = new CommonPlugins(SERVER);
		assertThrows(IllegalStateException.class, () -> registry.initialize(List.of(plugin("a", r -> {
			var failure = new AtomicReference<Throwable>();
			var worker = new Thread(() -> {
				try {
					r.capacity(id("async"), p -> 1);
				} catch (Throwable e) {
					failure.set(e);
				}
			});
			worker.start();
			try {
				worker.join();
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			assertInstanceOf(IllegalStateException.class, failure.get());
		}))));
		assertTrue(registry.registrations().capacities().isEmpty());
	}

	@Test
	void clientFailureIsIsolatedAndRetainedHudRegistrationExpires () {
		var registry = new ClientPlugins(CLIENT);
		var retained = new AtomicReference<HudRegistration>();
		registry.initialize(List.of(client("a", r -> {
			r.onPlayerChanged(id("failed"), p -> {
			});
			throw new IllegalArgumentException("expected test failure");
		}), client("b", r -> {
			assertSame(CLIENT, r.weights());
			retained.set(r.hud());
			r.onPlayerChanged(id("good"), p -> {
			});
		})));
		assertEquals(Set.of(id("good")), registry.registrations().playerChanged().keySet());
		assertThrows(IllegalStateException.class, () -> retained.get().decorate(id("late"), HudElement.RING, HudRegistration.Phase.AFTER, (g, c, d) -> {
		}));
		assertThrows(IllegalStateException.class, () -> registry.initialize(List.of()));
	}

	@Test
	void duplicateClientPluginsAreBothDisabled () {
		var registry = new ClientPlugins(CLIENT);
		registry.initialize(List.of(client("same", r -> fail("Duplicate must not register")), client("same", r -> fail("Duplicate must not register")), client("good", r -> r.onPlayerChanged(id("good"), p -> {
		}))));
		assertEquals(Set.of(id("good")), registry.registrations().playerChanged().keySet());
	}

	@Test
	void badConstructorIsFatalForCommonAndOptionalForClient () {
		assertThrows(IllegalStateException.class, () -> new CommonPlugins(SERVER).initialize(List.of(new PluginCandidate<>(Set.of("fixture"), "Broken", () -> {
			throw new NoClassDefFoundError("optional.Dependency");
		}))));
		var registry = new ClientPlugins(CLIENT);
		registry.initialize(List.of(new PluginCandidate<>(Set.of("fixture"), "Broken", () -> {
			throw new NoClassDefFoundError("optional.Dependency");
		})));
		assertTrue(registry.registrations().owners().isEmpty());
	}

	@Test
	void metadataIsReadWithoutInitializingThePlugin () {
		var metadata = PluginMetadata.read(getClass().getName() + "$UnloadedPlugin", getClass().getClassLoader()).orElseThrow();
		assertEquals(HIPlugin.Side.CLIENT, metadata.side());
		assertEquals(Set.of("optional_mod"), metadata.requiredMods());
		assertFalse(metadata.dependenciesPresent(mod -> false));
		assertTrue(metadata.dependenciesPresent(mod -> true));
		assertTrue(PluginMetadata.read(getClass().getName(), getClass().getClassLoader()).isEmpty());
	}

	@HIPlugin(value = HIPlugin.Side.CLIENT, requires = "optional_mod")
	static class UnloadedPlugin {

		static {
			if (true) throw new AssertionError("Metadata reading initialized the plugin");
		}
	}

	private static Identifier id (String path) {
		return Identifier.fromNamespaceAndPath("fixture", path);
	}

	private static PluginCandidate<HeavyInventoriesPlugin> plugin (String name, Consumer<PluginRegistration> register) {
		return new PluginCandidate<>(Set.of("fixture"), name, () -> new HeavyInventoriesPlugin() {
			@Override
			public Identifier id () {
				return PluginRegistrationTest.id(name);
			}

			@Override
			public void register (PluginRegistration registration) {
				register.accept(registration);
			}
		});
	}

	private static PluginCandidate<HeavyInventoriesClientPlugin> client (String name, Consumer<ClientPluginRegistration> register) {
		return new PluginCandidate<>(Set.of("fixture"), name, () -> new HeavyInventoriesClientPlugin() {
			@Override
			public Identifier id () {
				return PluginRegistrationTest.id(name);
			}

			@Override
			public void register (ClientPluginRegistration registration) {
				register.accept(registration);
			}
		});
	}

	private static <T> T unusedService (Class<T> type) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
			throw new AssertionError("Registration must not query a world: " + method);
		}));
	}

}
