package com.iso2t.heavyinventories.client;

import com.google.gson.JsonObject;
import com.iso2t.heavyinventories.api.player.PlayerHolder;
import com.iso2t.heavyinventories.config.EffectsSettings;
import com.iso2t.heavyinventories.config.ServerSettings;
import com.iso2t.heavyinventories.config.WalkingMode;
import com.iso2t.heavyinventories.network.ServerConfigUpdatePayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Displays the current server snapshot; saving sends a permission-checked request. */
public final class ServerConfigScreen {
	private ServerConfigScreen () {
	}

	public static ConfigBuilder create (java.util.function.Consumer<ServerConfigUpdatePayload> send) {
		var builder = ConfigBuilder.create().setParentScreen(null).setTitle(Component.translatable("title.heavyinventories.config.server"));
		var category = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.general"));
		var entries = builder.entryBuilder();
		var player = Minecraft.getInstance().player;
		if (player == null || !PlayerHolder.getOrCreate(player).hasServerState()) {
			category.addEntry(entries.startTextDescription(Component.translatable("config.heavyinventories.unavailable")).build());
			return builder;
		}
		var holder = PlayerHolder.getOrCreate(player);
		var initial = holder.serverSettings();
		var draft = initial.toJson();
		boolean editable = holder.canEditServerConfig();
		long revision = holder.serverRevision();
		List<Runnable> readFields = new ArrayList<>();
		var capacity = entries.startFloatField(Component.translatable("option.heavyinventories.starting_max_weight"), initial.startingWeight())
				.setDefaultValue(ServerSettings.DEFAULT.startingWeight()).setMin(Float.MIN_VALUE).setMax(ServerSettings.MAX_VALUE)
				.setTooltip(Component.translatable("option.heavyinventories.starting_max_weight.tooltip"))
				.setErrorSupplier(value -> {
					try { new ServerSettings(value); return Optional.empty(); }
					catch (IllegalArgumentException e) { return Optional.of(Component.literal(e.getMessage())); }
				}).build();
		capacity.setEditable(editable);
		readFields.add(() -> draft.addProperty("startingWeight", capacity.getValue()));
		category.addEntry(capacity);
		var mode = entries.startEnumSelector(Component.translatable("option.heavyinventories.walking_mode"), WalkingMode.class, initial.walkingMode())
				.setDefaultValue(ServerSettings.DEFAULT.walkingMode())
				.setEnumNameProvider(value -> Component.translatable("option.heavyinventories.walking_mode." + ((WalkingMode) value).id()))
				.setTooltip(Component.translatable("option.heavyinventories.walking_mode.tooltip")).build();
		mode.setEditable(editable);
		readFields.add(() -> draft.addProperty("walkingMode", mode.getValue().id()));
		category.addEntry(mode);
		category.addEntry(entries.startTextDescription(Component.translatable(editable ? "config.heavyinventories.edit_help" : "config.heavyinventories.read_only")).build());

		var effects = draft.getAsJsonObject("effects");
		var defaults = EffectsSettings.DEFAULT.toJson();
		var fluids = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects.fluids"));
		fluids.addEntry(entries.startTextDescription(Component.translatable("config.heavyinventories.fluid_active")).build());
		for (var entry : effects.entrySet()) {
			String group = entry.getKey();
			if (entry.getValue().isJsonObject()) {
				var section = builder.getOrCreateCategory(Component.translatable("category.heavyinventories.effects." + group));
				String notice = switch (group) {
					case "exhaustion" -> "config.heavyinventories.exhaustion_active";
					case "fallDamage" -> "config.heavyinventories.fall_active";
					case "swimming", "sinking", "upwardMovement" -> "config.heavyinventories.fluid_active";
					case "knockback" -> "config.heavyinventories.knockback_active";
					default -> "config.heavyinventories.effects_pending";
				};
				section.addEntry(entries.startTextDescription(Component.translatable(notice)).build());
				var values = entry.getValue().getAsJsonObject();
				for (String key : values.keySet())
					addEffectField(builder, section, values, defaults.getAsJsonObject(group), group, key, editable, readFields);
			} else addEffectField(builder, fluids, effects, defaults, "fluids", group, editable, readFields);
		}
		builder.setSavingRunnable(() -> {
			if (!editable) return;
			readFields.forEach(Runnable::run);
			var updated = ServerSettings.parse(draft);
			if (!updated.equals(initial)) send.accept(new ServerConfigUpdatePayload(updated, revision));
		});
		return builder;
	}

	private static void addEffectField (ConfigBuilder builder, ConfigCategory category, JsonObject values, JsonObject defaults,
	                                    String group, String key, boolean editable, List<Runnable> readFields) {
		String translation = "option.heavyinventories.effects." + group + "." + key;
		var entries = builder.entryBuilder();
		if (values.get(key).getAsJsonPrimitive().isBoolean()) {
			var field = entries.startBooleanToggle(Component.translatable(translation), values.get(key).getAsBoolean())
					.setDefaultValue(defaults.get(key).getAsBoolean()).setTooltip(Component.translatable(translation + ".tooltip")).build();
			field.setEditable(editable);
			readFields.add(() -> values.addProperty(key, field.getValue()));
			category.addEntry(field);
		} else {
			var field = entries.startFloatField(Component.translatable(translation), values.get(key).getAsFloat())
					.setDefaultValue(defaults.get(key).getAsFloat()).setMin(0).setMax(ServerSettings.MAX_VALUE)
					.setTooltip(Component.translatable(translation + ".tooltip"))
					.setErrorSupplier(value -> {
						readFields.forEach(Runnable::run);
						values.addProperty(key, value);
						var candidate = new JsonObject();
						candidate.add(group, values.deepCopy());
						try { EffectsSettings.parse(candidate); return Optional.empty(); }
						catch (IllegalArgumentException e) { return Optional.of(Component.literal(e.getMessage())); }
					}).build();
			field.setEditable(editable);
			readFields.add(() -> values.addProperty(key, field.getValue()));
			category.addEntry(field);
		}
	}
}
