package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.entity.WarpEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Revealing Lens HUD: debug stats to the right of crosshair;
 * present elements as icon + amount under the looked-at node (crosshair).
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpRevealOverlay {
    private static final double REVEAL_RANGE = 48.0;
    private static final int TEXT_COLOR = 0xFFE8E8F0;
    private static final int LABEL_COLOR = 0xFF9A9AB0;
    private static final int AMOUNT_COLOR = 0xFFE8E8F0;
    /** Draw icons at native 16×16 so they are not clipped. */
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEX = 16;
    private static final int SLOT_GAP = 6;
    private static final int AMOUNT_GAP = 2;

    private WarpRevealOverlay() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ExampleMod.REVEALING_HELMET.get())) {
            return;
        }

        WarpEntity warp = findLookedWarp(player);
        if (warp == null) {
            return;
        }

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = minecraft.font;
        int centerX = gui.guiWidth() / 2;
        int centerY = gui.guiHeight() / 2;

        drawDebugPanel(gui, font, centerX + 14, centerY - 40, warp, player);
        drawElementStrip(gui, font, centerX, centerY + 14, warp.getElementAmounts());
    }

    /** Text-only debug: damage / force / position — not mixed with element icons. */
    private static void drawDebugPanel(
            GuiGraphicsExtractor gui,
            Font font,
            int x,
            int y,
            WarpEntity warp,
            LocalPlayer player
    ) {
        double dist = player.getEyePosition().distanceTo(warp.position());
        float damageHp = warp.getForce() / 10.0F;

        gui.textWithBackdrop(font, net.minecraft.network.chat.Component.literal("Warp Debug"), x, y, 120, LABEL_COLOR);
        y += 12;
        y = drawLine(gui, font, x, y, "Subtype", warp.getSubtype().id());
        y = drawLine(gui, font, x, y, "Distortion", String.valueOf(warp.getDistortion()));
        y = drawLine(gui, font, x, y, "Force", String.valueOf(warp.getForce()));
        y = drawLine(gui, font, x, y, "Damage", String.format("%.1f HP / %.1f hearts", damageHp, damageHp / 2.0F));
        y = drawLine(gui, font, x, y, "Pos", String.format("%.1f %.1f %.1f", warp.getX(), warp.getY(), warp.getZ()));
        drawLine(gui, font, x, y, "Distance", String.format("%.1f", dist));
    }

    /**
     * Under the crosshair (looked-at node): each present element as icon with amount below.
     * Skips amount 0.
     */
    private static void drawElementStrip(GuiGraphicsExtractor gui, Font font, int centerX, int topY, ElementAmounts amounts) {
        List<Element> present = new ArrayList<>(8);
        for (Element element : Element.values()) {
            if (amounts.get(element) > 0) {
                present.add(element);
            }
        }
        if (present.isEmpty()) {
            return;
        }

        int slotWidth = ICON_SIZE;
        for (Element element : present) {
            int textW = font.width(String.valueOf(amounts.get(element)));
            slotWidth = Math.max(slotWidth, textW);
        }

        int totalWidth = present.size() * slotWidth + (present.size() - 1) * SLOT_GAP;
        int x = centerX - totalWidth / 2;

        for (Element element : present) {
            int amount = amounts.get(element);
            int iconX = x + (slotWidth - ICON_SIZE) / 2;
            gui.blit(
                    RenderPipelines.GUI_TEXTURED,
                    element.iconTexture(),
                    iconX,
                    topY,
                    0.0F,
                    0.0F,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_TEX,
                    ICON_TEX
            );

            String label = String.valueOf(amount);
            int textX = x + (slotWidth - font.width(label)) / 2;
            int textY = topY + ICON_SIZE + AMOUNT_GAP;
            gui.text(font, label, textX, textY, AMOUNT_COLOR, true);

            x += slotWidth + SLOT_GAP;
        }
    }

    private static int drawLine(GuiGraphicsExtractor gui, Font font, int x, int y, String label, String value) {
        gui.text(font, label + ": " + value, x, y, TEXT_COLOR, true);
        return y + 10;
    }

    private static WarpEntity findLookedWarp(LocalPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(REVEAL_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(REVEAL_RANGE)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                box,
                entity -> entity instanceof WarpEntity && entity.isAlive(),
                0.0F
        );
        if (hit == null) {
            return null;
        }
        Entity entity = hit.getEntity();
        return entity instanceof WarpEntity warp ? warp : null;
    }
}
