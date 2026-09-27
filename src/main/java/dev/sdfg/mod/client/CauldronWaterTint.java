package dev.sdfg.mod.client;

import dev.sdfg.mod.block.HeatedCauldronBlockEntity;
import dev.sdfg.mod.element.Element;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tints the water surface in {@code sdfg:cauldron} with the brew's element mix. */
public final class CauldronWaterTint implements BlockTintSource {
    public static final CauldronWaterTint INSTANCE = new CauldronWaterTint();
    private static final int FALLBACK = 0xFF000000 | Element.WATER.color();

    private CauldronWaterTint() {
    }

    @Override
    public int color(BlockState state) {
        return FALLBACK;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HeatedCauldronBlockEntity cauldron) {
            return cauldron.contents().blendColor();
        }
        return FALLBACK;
    }
}
