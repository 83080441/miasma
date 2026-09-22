package dev.sdfg.mod.worldgen;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldGen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ExampleMod.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> WARP_SURFACE =
            FEATURES.register("warp_surface", () -> new WarpSurfaceFeature(NoneFeatureConfiguration.CODEC));

    private ModWorldGen() {
    }
}
