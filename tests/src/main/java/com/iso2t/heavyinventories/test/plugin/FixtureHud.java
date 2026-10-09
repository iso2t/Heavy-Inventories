package com.iso2t.heavyinventories.test.plugin;

import com.iso2t.heavyinventories.api.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public final class FixtureHud {

	public enum Mode {
		DEFAULT,
		MOVE,
		HIDE,
		REPLACE,
		HELPERS,
		FAIL_RENDER,
		FAIL_LAYOUT,
		DOUBLE_DEFAULT,
		FAIL_DECORATION,
		POP_PARENT
	}

	public static boolean active;
	public static Mode    mode = Mode.DEFAULT;
	public static int     primaryLayouts, secondaryLayouts, numberLayouts, renders, before, after, failures;
	public static HudContext context;
	public static HudLayout  ring, numbers;
	public static       HudDrawing   retainedDrawing;
	public static final List<String> order = new ArrayList<>();

	public static Identifier id (String path) {
		return Identifier.fromNamespaceAndPath("heavyinventories_lifecycle_test", path);
	}

	public static void register (HudRegistration hud) {
		hud.owner(id("ring"), HudElement.RING, 10, new Owner(false));
		hud.owner(id("other_ring"), HudElement.RING, 0, new Owner(true));
		hud.owner(id("failure_ring"), HudElement.RING, -10, new Owner(false));
		hud.owner(id("double_ring"), HudElement.RING, -20, new Owner(false));
		hud.owner(id("pop_ring"), HudElement.RING, -30, new Owner(false));
		hud.owner(id("layout_ring"), HudElement.RING, -40, new Owner(false));
		hud.owner(id("numbers"), HudElement.NUMBERS, 0, new HudIntegration() {
			@Override
			public HudLayout layout (HudContext c, HudElement e, HudLayout original) {
				if (active) {
					record(c);
					numberLayouts++;
				}
				numbers = mode == Mode.MOVE && active ? new HudLayout(new HudBounds(12, 12, original.bounds().width(), original.bounds().height()), true, 0) : original;
				return numbers;
			}
		});
		hud.decorate(id("before_a"), HudElement.RING, HudRegistration.Phase.BEFORE, (g, c, l) -> {
			if (!active) return;
			record(c);
			before++;
			order.add("a");
			g.pose().pushMatrix();
			g.pose().translate(123, 456);
			g.enableScissor(0, 0, 1, 1);
		});
		hud.decorate(id("before_b"), HudElement.RING, HudRegistration.Phase.BEFORE, (g, c, l) -> {
			if (!active) return;
			record(c);
			order.add("b");
			require(g.pose().equals(new Matrix3x2f()), "Pose leaked between decorations");
			require(g.containsPointInScissor(c.screenWidth() / 2, c.screenHeight() / 2), "Scissor leaked between decorations");
		});
		hud.decorate(id("after"), HudElement.RING, HudRegistration.Phase.AFTER, (g, c, l) -> {
			if (!active) return;
			record(c);
			after++;
			order.add("after");
			require(g.pose().equals(new Matrix3x2f()), "Owner leaked pose");
			require(g.containsPointInScissor(c.screenWidth() / 2, c.screenHeight() / 2), "Owner leaked scissor");
			if (mode == Mode.FAIL_DECORATION) {
				failures++;
				throw new IllegalStateException("Expected decoration failure");
			}
		});
	}

	private static void record (HudContext next) {
		if (context != next) {
			context = next;
			order.clear();
		}
	}

	private record Owner(boolean secondary) implements HudIntegration {

		@Override
		public HudLayout layout (HudContext c, HudElement e, HudLayout original) {
			if (!active) return original;
			record(c);
			if (secondary) secondaryLayouts++;
			else primaryLayouts++;
			if (mode == Mode.FAIL_LAYOUT) {
				failures++;
				throw new IllegalStateException("Expected layout failure");
			}
			if (mode == Mode.HIDE) return ring = new HudLayout(original.bounds(), false, 0);
			if (mode == Mode.MOVE) return ring = new HudLayout(new HudBounds(18, 18, 16, 16), true, 0);
			return ring = original;
		}

		@Override
		public void render (GuiGraphicsExtractor graphics, HudContext c, HudLayout layout, HudDrawing drawing) {
			if (!active) {
				drawing.drawDefault();
				return;
			}
			require(context == c, "Owner layout and render received different frame contexts");
			renders++;
			order.add("owner");
			retainedDrawing = drawing;
			switch (mode) {
				case REPLACE -> graphics.fill(layout.bounds().x(), layout.bounds().y(), layout.bounds().x() + 16, layout.bounds().y() + 16, 0xFF55FF55);
				case HELPERS -> {
					drawing.ring(graphics, layout.bounds().x(), layout.bounds().y(), c.player().orElseThrow());
					drawing.numbers(graphics, c.screenWidth() - 20, 20, c.player().orElseThrow());
				}
				case FAIL_RENDER -> {
					failures++;
					graphics.pose().pushMatrix();
					graphics.pose().translate(30, 40);
					graphics.enableScissor(0, 0, 1, 1);
					throw new IllegalStateException("Expected owner failure");
				}
				case DOUBLE_DEFAULT -> {
					drawing.drawDefault();
					failures++;
					drawing.drawDefault();
				}
				case POP_PARENT -> {
					failures++;
					graphics.disableScissor();
				}
				default -> drawing.drawDefault();
			}
		}

	}

	public static void reset () {
		primaryLayouts = secondaryLayouts = numberLayouts = renders = before = after = failures = 0;
		context = null;
		ring = numbers = null;
		retainedDrawing = null;
		order.clear();
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

}
