package dev.sdfg.mod.item;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.ElementAmounts;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ElementAmounts>> VIAL_CONTENTS =
            DATA_COMPONENTS.registerComponentType("vial_contents", builder -> builder.persistent(ElementAmounts.CODEC));

    private ModDataComponents() {
    }
}
