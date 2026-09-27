package com.iso2t.heavyinventories.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegistryHelper {

	public static List<Item> getItemsFor (String modid) {
		return BuiltInRegistries.ITEM.stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(modid)).toList();
	}

	public static List<Block> getBlocksFor (String modid) {
		return BuiltInRegistries.BLOCK.stream().filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(modid)).toList();
	}

}
