package com.iso2t.heavyinventories.neoforge.datagen;

import com.iso2t.heavyinventories.HeavyInventories;
import com.iso2t.heavyinventories.enchantment.ModEnchantments;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = HeavyInventories.MOD_ID)
public class DataGenerators {

	@SubscribeEvent
	public static void gatherData (GatherDataEvent.Client event) {
		event.createWorldRegistryObjects(new RegistrySetBuilder().add(Registries.ENCHANTMENT, ModEnchantments::bootstrap));
	}

}
