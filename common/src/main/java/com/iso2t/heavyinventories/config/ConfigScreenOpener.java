package com.iso2t.heavyinventories.config;

@FunctionalInterface
public interface ConfigScreenOpener {

	void openConfigScreen ();

	ConfigScreenOpener NO_OP = () -> {
	};

}
