package dev.sdfg.mod.item;

import java.util.function.Consumer;

import dev.sdfg.mod.element.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A paper scroll. Each kind names a future spell and lists the elements it needs
 * as the gray line under the name. There is no recipe yet.
 */
public class ScrollItem extends Item {
    /** Lightning bolt. Needs earth, fire, and water. */
    public static final Kind BOLT = new Kind("bolt", Element.EARTH, Element.FIRE, Element.WATER);

    private final Kind kind;

    public ScrollItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public static DeferredItem<ScrollItem> register(DeferredRegister.Items items, Kind kind) {
        return items.registerItem("scroll_" + kind.id(), properties -> new ScrollItem(properties, kind));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("item.sdfg.scroll.elements", elementList(this.kind.elements()))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static Component elementList(Element[] elements) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                line.append(", ");
            }
            line.append(Component.translatable("element.sdfg." + elements[i].id()));
        }
        return line;
    }

    /** Stable id plus the elements this scroll asks for, in display order. */
    public record Kind(String id, Element... elements) {
    }
}
