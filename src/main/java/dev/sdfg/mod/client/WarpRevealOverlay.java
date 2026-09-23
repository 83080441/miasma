package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.entity.WarpEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
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

/**
 * While wearing the Revealing Lens helmet and looking at a Warp, shows node stats on the HUD.
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpRevealOverlay {
    private static final double REVEAL_RANGE = 48.0;
    private static final int TEXT_COLOR = 0xFFE8E8F0;
    private static final int LABEL_COLOR = 0xFF9A9AB0;

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
        int x = gui.guiWidth() / 2 + 12;
        int y = gui.guiHeight() / 2 - 42;
        double dist = player.getEyePosition().distanceTo(warp.position());

        gui.textWithBackdrop(font, net.minecraft.network.chat.Component.literal("Warp Reveal"), x, y, 120, LABEL_COLOR);
        y += 12;
        drawLine(gui, font, x, y, "Subtype", warp.getSubtype().id());
        y += 10;
        drawLine(gui, font, x, y, "Distortion", String.valueOf(warp.getDistortion()));
        y += 10;
        drawLine(gui, font, x, y, "Force", String.valueOf(warp.getForce()));
        y += 10;
        float damageHp = warp.getForce() / 10.0F;
        drawLine(gui, font, x, y, "Damage", String.format("%.1f HP / %.1f hearts", damageHp, damageHp / 2.0F));
        y += 10;
        drawLine(gui, font, x, y, "Pos", String.format(
                "%.1f %.1f %.1f",
                warp.getX(), warp.getY(), warp.getZ()
        ));
        y += 10;
        drawLine(gui, font, x, y, "Distance", String.format("%.1f", dist));
    }

    private static void drawLine(GuiGraphicsExtractor gui, Font font, int x, int y, String label, String value) {
        String line = label + ": " + value;
        gui.text(font, line, x, y, TEXT_COLOR, true);
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
