package com.iso2t.heavyinventories.server.weight;

import com.iso2t.heavyinventories.HeavyInventories;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Stages a candidate only; gameplay adoption is a separate server-thread operation. */
public final class WeightPackReloadListener extends SimplePreparableReloadListener<WeightPackData.Result> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("heavyinventories", "weights");

    @Override protected WeightPackData.Result prepare(ResourceManager manager, ProfilerFiller profiler) {
        return WeightPackData.load(manager, BuiltInRegistries.ITEM::containsKey);
    }

    @Override protected void apply(WeightPackData.Result result, ResourceManager manager, ProfilerFiller profiler) {
        ((WeightPackAccess) manager).heavyinventories$setWeightPackData(result);
        result.warnings().forEach(problem -> HeavyInventories.LOGGER.warn("Weight datapack: {}", problem));
        if (result.valid()) {
            HeavyInventories.LOGGER.info("Loaded {} datapack weight definitions for server resolution", result.definitions().size());
        } else {
            result.errors().forEach(problem -> HeavyInventories.LOGGER.error("Weight datapack: {}", problem));
            HeavyInventories.LOGGER.error("Rejected datapack weight candidate; no partial definitions will be applied");
        }
    }
}
