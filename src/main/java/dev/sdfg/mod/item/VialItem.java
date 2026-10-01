package dev.sdfg.mod.item;

import java.util.function.Consumer;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.block.ElementContainerBlockEntity;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Corked glass vial. Empty until a container fills it with up to 100 of its mix. */
public class VialItem extends Item {
    public static final int CAPACITY = 100;

    public VialItem(Properties properties) {
        super(properties);
    }

    public static boolean isEmptyVial(ItemStack stack) {
        return stack.is(ExampleMod.VIAL.get()) && contentsOf(stack).isEmpty();
    }

    public static ElementAmounts contentsOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ExampleMod.VIAL.get())) {
            return ElementAmounts.empty();
        }
        ElementAmounts stored = stack.get(ModDataComponents.VIAL_CONTENTS.get());
        return stored == null ? ElementAmounts.empty() : stored;
    }

    /** Pulls a 100-unit share out of the tank. Returns the same stack when the tank has nothing to give. */
    public static ItemStack fillFrom(ElementContainerBlockEntity tank, ItemStack emptyVial) {
        ElementAmounts drawn = tank.contents().portion(CAPACITY);
        if (drawn.isEmpty()) {
            return emptyVial;
        }
        tank.extract(drawn);
        ItemStack filled = emptyVial.copyWithCount(1);
        filled.set(ModDataComponents.VIAL_CONTENTS.get(), drawn);
        if (tank.getLevel() != null) {
            tank.getLevel().playSound(null, tank.getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return filled;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        ElementAmounts contents = contentsOf(stack);
        for (Element element : Element.values()) {
            int amount = contents.get(element);
            if (amount <= 0) {
                continue;
            }
            tooltip.accept(Component.translatable("element.sdfg." + element.id())
                    .append(Component.literal(" " + amount))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
