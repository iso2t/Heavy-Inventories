package com.iso2t.heavyinventories.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NeoForge discovery marker. Fabric uses heavyinventories or heavyinventories_client entrypoints.
 * CLIENT classes are excluded before class loading on dedicated servers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HIPlugin {
	Side value () default Side.COMMON;

	enum Side {
		COMMON,
		CLIENT
	}
}
