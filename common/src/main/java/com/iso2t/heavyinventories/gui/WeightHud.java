package com.iso2t.heavyinventories.gui;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.client.*;
import com.iso2t.heavyinventories.config.ConfigOptions;
import com.iso2t.heavyinventories.integration.client.ClientPlugins;
import com.iso2t.heavyinventories.integration.client.ClientRegistrations;
import com.iso2t.heavyinventories.integration.client.HudHooks;
import com.iso2t.heavyinventories.player.PlayerHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.EnumSet;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightHud {
	private static final HudHooks HOOKS = new HudHooks();
	private static       Frame    frame;
	private static       boolean  extracting;

	public static void beginFrame () {
		frame = null;
		extracting = true;
	}

	public static void endFrame () {
		frame = null;
		extracting = false;
	}

	private static Frame frame (Minecraft client) {
		if (frame == null) {
			var context = context(client);
			var registrations = ClientPlugins.INSTANCE.registrations();
			frame = new Frame(context, registrations, HOOKS.resolve(registrations, context, HudElement.RING, ConfigOptions.RING_HUD_OWNER), HOOKS.resolve(registrations, context, HudElement.NUMBERS, ConfigOptions.NUMBERS_HUD_OWNER));
		}
		return frame;
	}

	private static HudContext context (Minecraft client) {
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		var snapshot = client.player == null ? Optional.<PlayerWeightSnapshot>empty() : Optional.ofNullable(PlayerHolder.getOrCreate(client.player).apiSnapshot());
		boolean visible = GraphicsRenderer.visible(client) && snapshot.isPresent();
		boolean ring = visible && ConfigOptions.HUD_MODE.ring();
		boolean numbers = visible && (ConfigOptions.HUD_MODE.numbers() || snapshot.orElseThrow().carriedWeight().pounds().isEmpty());
		var ringBounds = new HudBounds(width / 2 - 8, height - 39 - ConfigOptions.RING_VERTICAL_OFFSET, 16, 16);
		var numberBounds = snapshot.map(value -> GraphicsRenderer.bounds(client, value, ConfigOptions.WEIGHT_MEASURE)).orElse(new HudBounds(width - 4, height - 4, 0, 0));
		return new HudContext(snapshot, width, height, visible, ConfigOptions.HUD_MODE.ring(), ConfigOptions.HUD_MODE.numbers(), new HudLayout(ringBounds, ring, ring ? ConfigOptions.RING_VERTICAL_OFFSET : 0), new HudLayout(numberBounds, numbers, 0));
	}

	public static int xpOffset (Minecraft client) {
		if (!extracting) return WeightRingRenderer.visible(client) ? ConfigOptions.RING_VERTICAL_OFFSET : 0;
		return frame(client).ring.layout().xpOffset();
	}

	public static void render (HudElement element, GuiGraphicsExtractor graphics, Minecraft client) {
		if (!extracting) return;
		var current = frame(client);
		if (!current.drawn.add(element)) return;
		var resolved = element == HudElement.RING ? current.ring : current.numbers;
		if (!resolved.layout().visible()) return;
		decorate(current, element, HudRegistration.Phase.BEFORE, graphics, resolved.layout());
		if (resolved.integration() == null) drawDefault(element, graphics, current.context, resolved.layout());
		else {
			try (var drawing = new Drawing(graphics, () -> drawDefault(element, graphics, current.context, resolved.layout()))) {
				((HudGraphicsState) graphics).heavyinventories$scoped(() -> resolved.integration().render(graphics, current.context, resolved.layout(), drawing));
			} catch (RuntimeException | LinkageError e) {
				HOOKS.failOwner(resolved.id(), e);
			}
		}
		decorate(current, element, HudRegistration.Phase.AFTER, graphics, resolved.layout());
	}

	private static void decorate (Frame current, HudElement element, HudRegistration.Phase phase, GuiGraphicsExtractor graphics, HudLayout layout) {
		current.registrations.decorations().forEach((id, decoration) -> {
			if (decoration.element() != element || decoration.phase() != phase || !HOOKS.decorationEnabled(id)) return;
			try {
				((HudGraphicsState) graphics).heavyinventories$scoped(() -> decoration.renderer().render(graphics, current.context, layout));
			} catch (RuntimeException | LinkageError e) {
				HOOKS.failDecoration(id, e);
			}
		});
	}

	private static void drawDefault (HudElement element, GuiGraphicsExtractor graphics, HudContext context, HudLayout layout) {
		var snapshot = context.player().orElseThrow();
		var bounds = layout.bounds();
		if (element == HudElement.RING) WeightRingRenderer.draw(graphics, bounds.x(), bounds.y(), snapshot);
		else GraphicsRenderer.drawNumbers(graphics, Minecraft.getInstance(), bounds.x() + bounds.width(), bounds.y(), snapshot, ConfigOptions.WEIGHT_MEASURE);
	}

	@RequiredArgsConstructor
	private static final class Frame {
		private final HudContext          context;
		private final ClientRegistrations registrations;
		private final HudHooks.Resolved   ring, numbers;
		private final EnumSet<HudElement> drawn = EnumSet.noneOf(HudElement.class);
	}

	private static final class Drawing implements HudDrawing, AutoCloseable {
		private final Thread               thread = Thread.currentThread();
		private       GuiGraphicsExtractor graphics;
		private       Runnable             defaultDraw;
		private       boolean              drawn;

		private Drawing (GuiGraphicsExtractor graphics, Runnable defaultDraw) {
			this.graphics = graphics;
			this.defaultDraw = defaultDraw;
		}

		private void check (GuiGraphicsExtractor target) {
			if (graphics == null || target != graphics || Thread.currentThread() != thread) throw new IllegalStateException("HUD drawing is only valid inside its render callback");
		}

		@Override
		public void drawDefault () {
			check(graphics);
			if (drawn) throw new IllegalStateException("Default HUD element already drawn");
			drawn = true;
			defaultDraw.run();
		}

		@Override
		public void ring (GuiGraphicsExtractor target, int x, int y, PlayerWeightSnapshot player) {
			check(target);
			WeightRingRenderer.draw(target, x, y, player);
		}

		@Override
		public void numbers (GuiGraphicsExtractor target, int x, int y, PlayerWeightSnapshot player) {
			check(target);
			GraphicsRenderer.drawNumbers(target, Minecraft.getInstance(), x, y, player, ConfigOptions.WEIGHT_MEASURE);
		}

		@Override
		public void close () {
			graphics = null;
			defaultDraw = null;
		}
	}
}
