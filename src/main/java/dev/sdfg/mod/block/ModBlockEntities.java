package dev.sdfg.mod.block;

import java.util.Set;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatedCauldronBlockEntity>> CAULDRON =
            BLOCK_ENTITIES.register("cauldron", () -> new BlockEntityType<>(
                    HeatedCauldronBlockEntity::new,
                    Set.of(ExampleMod.CAULDRON.get())
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CauldronLidBlockEntity>> CAULDRON_LID =
            BLOCK_ENTITIES.register("cauldron_lid", () -> new BlockEntityType<>(
                    CauldronLidBlockEntity::new,
                    Set.of(ExampleMod.CAULDRON_LID.get())
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElementContainerBlockEntity>> ELEMENT_CONTAINER =
            BLOCK_ENTITIES.register("element_container", () -> new BlockEntityType<>(
                    ElementContainerBlockEntity::new,
                    Set.of(ExampleMod.ELEMENT_CONTAINER.get())
            ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondensationFilterBlockEntity>> CONDENSATION_FILTER =
            BLOCK_ENTITIES.register("condensation_filter", () -> new BlockEntityType<>(
                    CondensationFilterBlockEntity::new,
                    Set.of(ExampleMod.CONDENSATION_FILTER.get())
            ));

    private ModBlockEntities() {
    }
}
