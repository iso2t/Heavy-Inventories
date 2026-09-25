package com.iso2t.heavyinventories.client;

import com.iso2t.heavyinventories.api.util.MeasuringSystem;
import java.text.NumberFormat;
import java.util.Locale;

/** One display-only conversion/rounding path for HUD and tooltips. */
public final class WeightDisplay {
    private static final int MAX_SMALL_FRACTION_DIGITS = 6;
    private static final double MIN_DISPLAY_VALUE = 0.000001;

    private WeightDisplay() {}
    public static String number(double value) {
        var format = NumberFormat.getNumberInstance(Locale.getDefault());
        format.setGroupingUsed(false);
        // Below a hundredth, keep one meaningful digit instead of rounding to zero.
        int digits = value > 0 && value < 0.01
                ? Math.min(MAX_SMALL_FRACTION_DIGITS, (int) Math.ceil(-Math.log10(value))) : 2;
        format.setMaximumFractionDigits(digits);
        if (value > 0 && value < MIN_DISPLAY_VALUE) return "<" + format.format(MIN_DISPLAY_VALUE);
        return format.format(value);
    }
    public static String weight(double stored, MeasuringSystem system) {
        return number(system.fromStored(stored)) + (system == MeasuringSystem.NONE ? "" : " " + system.getSub());
    }
}
