package com.iso2t.heavyinventories.config;

import com.google.gson.JsonObject;
import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import java.util.Locale;

/** Parse fully before changing any local display preferences. */
public record ClientSettings(MeasuringSystem measure, boolean overlay, int normal, int encumbered, int overloaded) {
    public static final ClientSettings DEFAULT = new ClientSettings(MeasuringSystem.LBS, true, 0xFFFFFF, 0xFFFF55, 0xFF5555);
    public ClientSettings {
        if (measure == null) throw new IllegalArgumentException("Missing weight measure");
        for (int color : new int[]{normal, encumbered, overloaded})
            if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Text colors must be RGB values between 0 and 16777215");
    }
    public static ClientSettings current() {
        return new ClientSettings(ConfigOptions.WEIGHT_MEASURE, ConfigOptions.ENABLE_GUI_OVERLAY,
                ConfigOptions.NORMAL_TEXT_COLOR, ConfigOptions.ENCUMBERED_TEXT_COLOR, ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR);
    }
    public void apply() {
        ConfigOptions.WEIGHT_MEASURE = measure;
        ConfigOptions.ENABLE_GUI_OVERLAY = overlay;
        ConfigOptions.NORMAL_TEXT_COLOR = normal;
        ConfigOptions.ENCUMBERED_TEXT_COLOR = encumbered;
        ConfigOptions.OVER_ENCUMBERED_TEXT_COLOR = overloaded;
    }
    public static ClientSettings parse(JsonObject root) {
        var measure = DEFAULT.measure;
        if (root.has("weightMeasure")) {
            var value = root.get("weightMeasure");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("weightMeasure must be a string");
            measure = MeasuringSystem.valueOf(value.getAsString().toUpperCase(Locale.ROOT));
        }
        boolean overlay = DEFAULT.overlay;
        if (root.has("enableGuiOverlay")) {
            var value = root.get("enableGuiOverlay");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("enableGuiOverlay must be a boolean");
            overlay = value.getAsBoolean();
        }
        return new ClientSettings(measure, overlay, color(root, "normalTextColor", DEFAULT.normal),
                color(root, "encumberedTextColor", DEFAULT.encumbered), color(root, "overencumberedTextColor", DEFAULT.overloaded));
    }
    private static int color(JsonObject root, String key, int fallback) {
        if (!root.has(key)) return fallback;
        var value = root.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key + " must be an integer RGB color");
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException e) { throw new IllegalArgumentException(key + " must be an integer RGB color", e); }
    }
    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("weightMeasure", measure.name());
        json.addProperty("enableGuiOverlay", overlay);
        json.addProperty("normalTextColor", normal);
        json.addProperty("encumberedTextColor", encumbered);
        json.addProperty("overencumberedTextColor", overloaded);
        return json;
    }
}
