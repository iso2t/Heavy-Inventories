package com.iso2t.heavyinventories.integration.client;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.api.client.HudContext;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.api.client.HudIntegration;
import com.iso2t.heavyinventories.api.client.HudLayout;
import net.minecraft.resources.Identifier;

import java.util.*;

public final class HudHooks {
	public static final Identifier      BUILTIN           = HeavyInventories.get("default");
	private final       Set<Identifier> failedOwners      = new HashSet<>();
	private final       Set<Identifier> failedDecorations = new HashSet<>();
	private final       Set<HudElement> reportedConflicts = new HashSet<>();

	public record Resolved(HudLayout layout, Identifier id, HudIntegration integration) {
	}

	public Resolved resolve (ClientRegistrations registrations, HudContext context, HudElement element, String preference) {
		var original = element == HudElement.RING ? context.ring() : context.numbers();
		var fallback = new Resolved(original, null, null);
		if (!context.visible() || !original.visible() || preference.equals(BUILTIN.toString())) return fallback;
		var candidates = registrations.owners().entrySet().stream().filter(e -> e.getValue().element() == element).sorted(Comparator.<Map.Entry<Identifier, ClientRegistrations.Owner>>comparingInt(e -> e.getValue().priority()).reversed().thenComparing(e -> e.getKey().toString())).toList();
		if (preference.isEmpty() && candidates.size() > 1 && reportedConflicts.add(element))
			HeavyInventories.LOGGER.info("HI HUD owners for {}: {}; using {} by priority and ID", element, candidates.stream().map(Map.Entry::getKey).toList(), candidates.getFirst().getKey());
		var selected = candidates.stream().filter(e -> preference.isEmpty() || e.getKey().toString().equals(preference)).findFirst();
		if (selected.isEmpty() || failedOwners.contains(selected.get().getKey())) return fallback;
		var owner = selected.get();
		try {
			var layout = Objects.requireNonNull(owner.getValue().integration().layout(context, element, original), "HUD layout");
			if (element == HudElement.NUMBERS && layout.xpOffset() != 0) throw new IllegalArgumentException("Numeric HUD cannot move XP");
			Math.addExact(layout.bounds().x(), layout.bounds().width());
			Math.addExact(layout.bounds().y(), layout.bounds().height());
			return new Resolved(layout, owner.getKey(), owner.getValue().integration());
		} catch (RuntimeException | LinkageError e) {
			failOwner(owner.getKey(), e);
			return fallback;
		}
	}

	public void failOwner (Identifier id, Throwable error) {
		if (failedOwners.add(id)) HeavyInventories.LOGGER.error("Disabled HI HUD owner {} until restart; restoring the default layout", id, error);
	}

	public boolean decorationEnabled (Identifier id) {
		return !failedDecorations.contains(id);
	}

	public void failDecoration (Identifier id, Throwable error) {
		if (failedDecorations.add(id)) HeavyInventories.LOGGER.error("Disabled HI HUD decoration {} until restart", id, error);
	}
}
