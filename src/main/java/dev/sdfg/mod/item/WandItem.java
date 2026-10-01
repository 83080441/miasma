package dev.sdfg.mod.item;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.block.ElementContainerBlock;
import dev.sdfg.mod.block.ElementContainerBlockEntity;
import dev.sdfg.mod.block.HeatedCauldronBlock;
import dev.sdfg.mod.block.HeatedCauldronBlockEntity;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.element.ElementLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stick-shaped wand. No recipe. While held, the helmet's element row
 * shows the sum of the blocks in the 3×3×3 around the player.
 */
public class WandItem extends Item {
    /** Half-width of the cube. 1 covers the player's block and one block each way. */
    public static final int RADIUS = 1;

    public WandItem(Properties properties) {
        super(properties);
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().is(ExampleMod.WAND.get())
                || player.getOffhandItem().is(ExampleMod.WAND.get());
    }

    /**
     * Sums every element of the blocks in the 3×3×3 whose center is the block
     * the player is standing on. Totals are not capped.
     */
    public static int[] survey(Player player) {
        int[] totals = new int[Element.values().length];
        BlockPos center = player.getOnPos();
        Level level = player.level();
        BlockPos min = center.offset(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = center.offset(RADIUS, RADIUS, RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            addAmounts(totals, at(level, pos));
        }
        return totals;
    }

    /** Cauldron and glass read their stored mix; every other block uses the helmet lookup. */
    private static ElementAmounts at(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeatedCauldronBlock
                && level.getBlockEntity(pos) instanceof HeatedCauldronBlockEntity cauldron) {
            return cauldron.contents();
        }
        if (state.getBlock() instanceof ElementContainerBlock
                && level.getBlockEntity(pos) instanceof ElementContainerBlockEntity container) {
            return container.contents();
        }
        return ElementLookup.of(state);
    }

    private static void addAmounts(int[] totals, ElementAmounts part) {
        if (part == null || part.isEmpty()) {
            return;
        }
        for (Element element : Element.values()) {
            int each = part.get(element);
            if (each > 0) {
                totals[element.number() - 1] += each;
            }
        }
    }
}
