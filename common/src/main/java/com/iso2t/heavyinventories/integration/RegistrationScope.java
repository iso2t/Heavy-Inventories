package com.iso2t.heavyinventories.integration;

import net.minecraft.resources.Identifier;

import java.util.*;

public final class RegistrationScope implements AutoCloseable {

	private final    Identifier       plugin;
	private final    Thread           thread = Thread.currentThread();
	private volatile boolean          open   = true;
	private volatile RuntimeException failure;

	public RegistrationScope (Identifier plugin) {
		this.plugin = plugin;
	}

	public void check () {
		if (!open) throw new IllegalStateException("Registration is closed for " + plugin);
		if (Thread.currentThread() != thread) {
			failure = new IllegalStateException("Registration must run on the initialization thread for " + plugin);
			throw failure;
		}
	}

	public void run (Runnable action) {
		check();
		try {
			action.run();
		} catch (RuntimeException e) {
			failure = e;
			throw e;
		}
	}

	public <T> void add (Map<Identifier, T> entries, Identifier id, T value) {
		run(() -> {
			Objects.requireNonNull(id, "Registration ID");
			Objects.requireNonNull(value, "Registration value");
			if (!id.getNamespace().equals(plugin.getNamespace())) throw new IllegalArgumentException("Registration " + id + " must use plugin namespace " + plugin.getNamespace());
			if (entries.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate registration " + id + " in " + plugin);
		});
	}

	public void finish () {
		check();
		open = false;
		if (failure != null) throw new IllegalStateException("Rejected registration for " + plugin, failure);
	}

	@Override
	public void close () {
		open = false;
	}

	public static <T> Map<Identifier, T> freeze (Map<Identifier, T> entries) {
		var sorted = new TreeMap<Identifier, T>(Comparator.comparing(Identifier::toString));
		sorted.putAll(entries);
		return Collections.unmodifiableMap(sorted);
	}

	public static void compatible (Map<Identifier, ?> existing, Map<Identifier, ?> additions) {
		for (var id : additions.keySet()) {
			if (existing.containsKey(id)) throw new IllegalArgumentException("Registration already owned by another plugin: " + id);
		}
	}

	public static <T> Map<Identifier, T> combine (Map<Identifier, T> current, Map<Identifier, T> added) {
		compatible(current, added);
		var merged = new HashMap<>(current);
		merged.putAll(added);
		return merged;
	}

}
