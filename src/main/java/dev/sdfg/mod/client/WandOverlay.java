package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.item.WandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * While the wand is held, the helmet's element row sums the blocks around the player.
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WandOverlay {
    private WandOverlay() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !WandItem.isHeld(player)) {
            return;
        }

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        int centerX = gui.guiWidth() / 2;
        int centerY = gui.guiHeight() / 2;
        ElementStripHud.draw(
                gui,
                minecraft.font,
                centerX,
                centerY + ElementStripHud.BELOW_CROSSHAIR,
                WandItem.survey(player),
                player,
                true,
                false,
                true
        );
    }
}
