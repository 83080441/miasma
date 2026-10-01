package dev.sdfg.mod.client;

import com.mojang.serialization.MapCodec;
import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.item.VialItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Tints the vial's liquid layer with the mix, weighted by each element's amount. */
public final class VialTint implements ItemTintSource {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "vial");
    public static final VialTint INSTANCE = new VialTint();
    public static final MapCodec<VialTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private VialTint() {
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        ElementAmounts contents = VialItem.contentsOf(stack);
        if (contents.isEmpty()) {
            return 0;
        }
        return contents.blendColor();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
