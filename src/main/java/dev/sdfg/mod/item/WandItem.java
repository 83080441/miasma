package dev.sdfg.mod.item;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.BlockResidue;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Stick-shaped wand. No recipe. While held, the helmet's element row
 * shows the sum of the blocks in the 3×3×3 around the player.
 */
public class WandItem extends Item {
    /** Half-width of the cube. 1 covers the player's block and one block each way. */
    public static final int RADIUS = 1;
    /** Right-click channel. Sixty ticks, one scroll element per third when the scroll lists three. */
    public static final int CHANNEL_TICKS = 60;

    public WandItem(Properties properties) {
        super(properties);
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().is(ExampleMod.WAND.get())
                || player.getOffhandItem().is(ExampleMod.WAND.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ScrollItem scroll = WandChannel.scrollInHotbar(player);
        if (scroll == null || !WandChannel.mayBegin(player)) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer server) {
            WandChannel.begin(server, scroll);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CHANNEL_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (entity instanceof ServerPlayer player) {
            WandChannel.tickDrain(player, ticksRemaining);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (entity instanceof ServerPlayer player) {
            WandChannel.cancel(player);
        }
        return false;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            WandChannel.complete(player);
        }
        return stack;
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

    /** What the block still has after any wand drain. Cauldron and glass use their mix. */
    private static ElementAmounts at(Level level, BlockPos pos) {
        return BlockResidue.amounts(level, pos);
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
