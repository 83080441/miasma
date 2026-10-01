package dev.sdfg.mod.client;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.element.ElementDiscovery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Shared element row: icon and amount for each element that is actually present.
 * Absent elements are left out. An undiscovered element on a helmet target is a
 * single obfuscated mark; the wand never draws that mark.
 */
public final class ElementStripHud {
    public static final int BELOW_CROSSHAIR = 14;

    private static final int AMOUNT_COLOR = 0xFFE8E8F0;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEX = 16;
    private static final int SLOT_GAP = 6;
    private static final int AMOUNT_GAP = 2;

    private ElementStripHud() {
    }

    /**
     * Helmet row. Present and discovered elements show their icon and amount.
     * A present element the player has not discovered is one obfuscated mark.
     * Elements that are not on the target are not drawn.
     */
    public static void draw(
            GuiGraphicsExtractor gui,
            Font font,
            int centerX,
            int topY,
            ElementAmounts amounts,
            LocalPlayer player
    ) {
        if (amounts == null || amounts.isEmpty()) {
            return;
        }
        int[] totals = new int[Element.values().length];
        for (Element element : Element.values()) {
            totals[element.number() - 1] = amounts.get(element);
        }
        draw(gui, font, centerX, topY, totals, player, true, true);
    }

    /**
     * @param onlyDiscovered when true, an undiscovered element is not shown as its icon
     * @param markUnknown when true, that undiscovered element is an obfuscated mark;
     *                    when false, it is omitted
     */
    public static void draw(
            GuiGraphicsExtractor gui,
            Font font,
            int centerX,
            int topY,
            int[] totals,
            LocalPlayer player,
            boolean onlyDiscovered,
            boolean markUnknown
    ) {
        if (totals == null) {
            return;
        }
        List<Slot> slots = new ArrayList<>();
        for (Element element : Element.values()) {
            int amount = totalOf(totals, element);
            if (amount <= 0) {
                continue;
            }
            if (!onlyDiscovered || ElementDiscovery.knows(player, element)) {
                slots.add(new Slot(element, amount, false));
            } else if (markUnknown) {
                slots.add(new Slot(element, amount, true));
            }
        }
        if (slots.isEmpty()) {
            return;
        }

        Component hidden = Component.literal("?").withStyle(ChatFormatting.OBFUSCATED);
        int hiddenWidth = font.width(hidden);
        int slotWidth = ICON_SIZE;
        if (markUnknown) {
            slotWidth = Math.max(slotWidth, hiddenWidth);
        }
        for (Slot slot : slots) {
            if (!slot.unknown) {
                slotWidth = Math.max(slotWidth, font.width(String.valueOf(slot.amount)));
            }
        }

        int totalWidth = slots.size() * slotWidth + (slots.size() - 1) * SLOT_GAP;
        int x = centerX - totalWidth / 2;
        for (Slot slot : slots) {
            if (slot.unknown) {
                gui.text(
                        font,
                        hidden,
                        x + (slotWidth - hiddenWidth) / 2,
                        topY + (ICON_SIZE - font.lineHeight) / 2,
                        AMOUNT_COLOR,
                        true
                );
            } else {
                gui.blit(
                        RenderPipelines.GUI_TEXTURED,
                        slot.element.iconTexture(),
                        x + (slotWidth - ICON_SIZE) / 2,
                        topY,
                        0.0F,
                        0.0F,
                        ICON_SIZE,
                        ICON_SIZE,
                        ICON_TEX,
                        ICON_TEX
                );
                String label = String.valueOf(slot.amount);
                gui.text(font, label, x + (slotWidth - font.width(label)) / 2, topY + ICON_SIZE + AMOUNT_GAP, AMOUNT_COLOR, true);
            }
            x += slotWidth + SLOT_GAP;
        }
    }

    private static int totalOf(int[] totals, Element element) {
        int index = element.number() - 1;
        return index >= 0 && index < totals.length ? totals[index] : 0;
    }

    private record Slot(Element element, int amount, boolean unknown) {
    }
}
