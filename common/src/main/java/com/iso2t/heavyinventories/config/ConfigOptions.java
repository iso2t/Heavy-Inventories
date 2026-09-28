package com.iso2t.heavyinventories.config;

import com.iso2t.heavyinventories.util.MeasuringSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigOptions {

	public static MeasuringSystem WEIGHT_MEASURE             = MeasuringSystem.LBS;
	public static boolean         ENABLE_GUI_OVERLAY         = true;
	public static HudMode         HUD_MODE                   = HudMode.RING;
	public static int             RING_VERTICAL_OFFSET       = 7;
	public static int             NORMAL_TEXT_COLOR          = 0xFFFFFF;
	public static int             ENCUMBERED_TEXT_COLOR      = 0xFFFF55;
	public static int             OVER_ENCUMBERED_TEXT_COLOR = 0xFF5555;

}
