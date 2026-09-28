package com.iso2t.heavyinventories;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeavyInventories {

	public static final String MOD_ID   = "heavyinventories";
	public static final String MOD_NAME = "Heavy Inventories";
	public static final Logger LOGGER   = LoggerFactory.getLogger(MOD_NAME);

	public static Identifier get (String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
