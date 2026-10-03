package com.iso2t.heavyinventories.config;

import com.iso2t.easyconfig.api.Side;
import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.annotations.Config;
import com.iso2t.easyconfig.api.annotations.Translation;
import com.iso2t.easyconfig.api.value.wrappers.*;
import com.iso2t.heavyinventories.util.MeasuringSystem;
import lombok.NoArgsConstructor;

@Config(name = "heavyinventories", side = Side.CLIENT)
@NoArgsConstructor
public final class ClientConfig {

	@Comment("Display units only: stored weights are pounds; kilograms convert by 0.45359237. None shows raw values. Capacity and gameplay are unchanged.")
	@Translation(value = "option.heavyinventories.weight_measure", tooltip = "option.heavyinventories.weight_measure.tooltip")
	public final EnumValue<MeasuringSystem> measure = EnumValue.of(ClientSettings.DEFAULT.measure());

	@Comment("Enable GUI Overlay")
	@Translation(value = "option.heavyinventories.enable_gui_overlay")
	public final BooleanValue overlay = BooleanValue.of(ClientSettings.DEFAULT.overlay());

	@Comment("Standard Text Color")
	@Translation(value = "option.heavyinventories.normal_text_color")
	public final ColorValue normal = ColorValue.of(0xFF000000 | ClientSettings.DEFAULT.normal());

	@Comment("Encumbered Text Color")
	@Translation(value = "option.heavyinventories.encumbered_text_color")
	public final ColorValue encumbered = ColorValue.of(0xFF000000 | ClientSettings.DEFAULT.encumbered());

	@Comment("Over Encumbered Text Color")
	@Translation(value = "option.heavyinventories.over_encumbered_text_color")
	public final ColorValue overloaded = ColorValue.of(0xFF000000 | ClientSettings.DEFAULT.overloaded());

	@Comment("Weight display")
	@Translation(value = "option.heavyinventories.hud_mode", valuePrefix = "option.heavyinventories.hud_mode.")
	public final EnumValue<HudMode> hudMode = EnumValue.of(ClientSettings.DEFAULT.hudMode());

	@Comment("Moves the ring and XP number upward together in GUI pixels (0–64; default 7). Zero may overlap the XP bar. Try 12 for long XP numbers. Hiding the ring restores vanilla XP positioning.")
	@Translation(value = "option.heavyinventories.ring_vertical_offset", tooltip = "option.heavyinventories.ring_vertical_offset.tooltip")
	public final IntegerValue ringVerticalOffset = IntegerValue.of(ClientSettings.DEFAULT.ringVerticalOffset(), 0, 64);

	@Comment("Renderer ID: blank selects by priority; heavyinventories:default uses HI. A missing or failed selected renderer falls back to HI.")
	@Translation(value = "option.heavyinventories.ring_owner", tooltip = "option.heavyinventories.hud_owner.tooltip")
	public final StringValue ringOwner = StringValue.of("");

	@Comment("Renderer ID for the numeric display; uses the same selection rules as the ring.")
	@Translation(value = "option.heavyinventories.numbers_owner", tooltip = "option.heavyinventories.hud_owner.tooltip")
	public final StringValue numbersOwner = StringValue.of("");

	public ClientConfig (ClientSettings settings) {
		ringOwner.set(settings.ringOwner());
		numbersOwner.set(settings.numbersOwner());
		measure.set(settings.measure());
		overlay.set(settings.overlay());
		normal.set(0xFF000000 | settings.normal());
		encumbered.set(0xFF000000 | settings.encumbered());
		overloaded.set(0xFF000000 | settings.overloaded());
		hudMode.set(settings.hudMode());
		ringVerticalOffset.set(settings.ringVerticalOffset());
	}

	public ClientSettings settings () {
		return new ClientSettings(measure.get(), overlay.get(), normal.get() & 0xFFFFFF, encumbered.get() & 0xFFFFFF, overloaded.get() & 0xFFFFFF, hudMode.get(), ringVerticalOffset.get(), ringOwner.get(), numbersOwner.get());
	}
}
