package dev.sdfg.mod.element;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves {@link ElementAmounts} for items and entity types from datapack defaults.
 * Missing definition → empty. Warps use their own synced storage, not this lookup.
 */
public final class ElementLookup {
    private ElementLookup() {
    }

    public static ElementAmounts of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ElementAmounts.empty();
        }
        return of(stack.getItem());
    }

    public static ElementAmounts of(Item item) {
        if (item == null) {
            return ElementAmounts.empty();
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return ElementDefinitions.forItem(id);
    }

    public static ElementAmounts of(Entity entity) {
        if (entity == null) {
            return ElementAmounts.empty();
        }
        return of(entity.getType());
    }

    public static ElementAmounts of(EntityType<?> type) {
        if (type == null) {
            return ElementAmounts.empty();
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return ElementDefinitions.forEntityType(id);
    }
}
