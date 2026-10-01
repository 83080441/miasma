package dev.sdfg.mod.element;

import dev.sdfg.mod.item.VialItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

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
        ElementAmounts carried = VialItem.contentsOf(stack);
        if (!carried.isEmpty()) {
            return carried;
        }
        return of(stack.getItem());
    }

    /**
     * Same reading the revealing helmet uses on a looked-at block:
     * the block's item, or the bucket of its fluid when the block itself has none.
     */
    public static ElementAmounts of(BlockState state) {
        if (state == null) {
            return ElementAmounts.empty();
        }
        Item item = state.getBlock().asItem();
        ElementAmounts amounts = item == Items.AIR ? ElementAmounts.empty() : of(item);
        if (!amounts.isEmpty()) {
            return amounts;
        }
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty()) {
            return ElementAmounts.empty();
        }
        Item bucket = fluid.getType().getBucket();
        return bucket == null || bucket == Items.AIR ? ElementAmounts.empty() : of(bucket);
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
