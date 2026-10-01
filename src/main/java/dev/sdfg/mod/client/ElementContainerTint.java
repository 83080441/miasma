package dev.sdfg.mod.client;

import dev.sdfg.mod.block.ElementContainerBlockEntity;
import dev.sdfg.mod.element.Element;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tints the liquid volume inside {@code sdfg:element_container} with the stored mix. */
public final class ElementContainerTint implements BlockTintSource {
    public static final ElementContainerTint INSTANCE = new ElementContainerTint();
    private static final int FALLBACK = 0xFF000000 | Element.WATER.color();

    private ElementContainerTint() {
    }

    @Override
    public int color(BlockState state) {
        return FALLBACK;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ElementContainerBlockEntity container) {
            return container.contents().blendColor();
        }
        return FALLBACK;
    }
}
