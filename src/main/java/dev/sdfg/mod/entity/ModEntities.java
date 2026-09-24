package dev.sdfg.mod.entity;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(ExampleMod.MODID);

    /** Warp distortion entity (strength 1–100 via NBT {@code Distortion}). */
    public static final DeferredHolder<EntityType<?>, EntityType<WarpEntity>> ENTITY1 = ENTITY_TYPES.registerEntityType(
            "entity1",
            WarpEntity::new,
            MobCategory.MISC,
            builder -> builder.sized(0.5F, 0.5F).eyeHeight(0.25F).clientTrackingRange(10).updateInterval(20).fireImmune()
    );

    /** Peaceful gnome — Blockbench husk mesh. See {@code plan/GNOME_README.md}. */
    public static final DeferredHolder<EntityType<?>, EntityType<GnomeEntity>> GNOME = ENTITY_TYPES.registerEntityType(
            "gnome",
            GnomeEntity::new,
            MobCategory.CREATURE,
            builder -> builder
                    .sized(0.6F, 1.4F)
                    .eyeHeight(1.1F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .noLootTable()
    );

    private ModEntities() {
    }

    /** Called from the mod bus in {@link ExampleMod}. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GNOME.get(), GnomeEntity.createAttributes().build());
    }
}
