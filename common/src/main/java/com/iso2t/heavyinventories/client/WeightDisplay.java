package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import java.text.NumberFormat;
import java.util.Locale;

/** One display-only conversion/rounding path for HUD and tooltips. */
public final class WeightDisplay {
    private WeightDisplay() {}
    public static String number(double value) {
        var format = NumberFormat.getNumberInstance(Locale.getDefault());
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(2);
        if (value > 0 && value < 0.01) return "<" + format.format(0.01);
        return format.format(value);
    }
    public static String weight(double stored, MeasuringSystem system) {
        return number(system.fromStored(stored)) + (system == MeasuringSystem.NONE ? "" : " " + system.getSub());
    }
}
