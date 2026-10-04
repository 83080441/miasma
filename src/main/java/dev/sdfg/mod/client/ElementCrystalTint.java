package dev.sdfg.mod.client;

import dev.sdfg.mod.block.ElementCrystalBlock;
import dev.sdfg.mod.element.Element;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Tints condensed crystals with their stored element color. */
public final class ElementCrystalTint implements BlockTintSource {
    public static final ElementCrystalTint INSTANCE = new ElementCrystalTint();
    private static final int FALLBACK = 0xFF000000 | Element.AETHER.color();

    private ElementCrystalTint() {
    }

    @Override
    public int color(BlockState state) {
        if (!state.hasProperty(ElementCrystalBlock.ELEMENT)) {
            return FALLBACK;
        }
        Element element = Element.byNumber(state.getValue(ElementCrystalBlock.ELEMENT));
        return element == null ? FALLBACK : (0xFF000000 | element.color());
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(ElementCrystalBlock.ELEMENT);
    }
}
