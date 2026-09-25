package com.iso2t.heavyinventories.fabric.datagen;

import com.iso2t.heavyinventories.api.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class DataGenerators implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator (FabricDataGenerator fabricDataGenerator) {
		var pack = fabricDataGenerator.createPack();

		pack.addProvider(ModEnchantmentGenerator::new);
	}

	@Override
	public void buildRegistry (RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
	}
}
