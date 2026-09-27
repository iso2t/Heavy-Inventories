package com.iso2t.heavyinventories.api.config;

import com.iso2t.heavyinventories.config.ConfigScreenOpener;
import com.iso2t.heavyinventories.platform.Services;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigScreens {

	private static final AtomicReference<ConfigScreenOpener> OPENER = new AtomicReference<>(ConfigScreenOpener.NO_OP);

	/**
	 * @deprecated Use {@link #openClientConfig()}, {@link #openServerConfig()}, or {@link #openCommonConfig()} instead.
	 */
	@Deprecated(forRemoval = true)
	public static void register (ConfigScreenOpener opener) {
		OPENER.set(opener == null ? ConfigScreenOpener.NO_OP : opener);
	}

	/**
	 * @deprecated Use {@link #openClientConfig()}, {@link #openServerConfig()}, or {@link #openCommonConfig()} instead.
	 */
	@Deprecated(forRemoval = true)
	public static boolean open () {
		var opener = OPENER.get();
		if (opener == ConfigScreenOpener.NO_OP) return false;
		opener.openConfigScreen();
		return true;
	}

	public static void openClientConfig () {
		if (Services.CONFIG_SCREEN.isClientSide()) {
			Services.CONFIG_SCREEN.openClientConfig();
		}
	}

	public static void openServerConfig () {
		if (Services.CONFIG_SCREEN.isClientSide()) {
			Services.CONFIG_SCREEN.openServerConfig();
		}
	}

	public static void openCommonConfig () {
		if (Services.CONFIG_SCREEN.isClientSide()) {
			Services.CONFIG_SCREEN.openCommonConfig();
		}
	}
}
