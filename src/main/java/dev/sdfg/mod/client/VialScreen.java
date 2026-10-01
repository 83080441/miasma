package dev.sdfg.mod.client;

import dev.sdfg.mod.inventory.VialMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Hopper panel with a single slot in the middle. The other painted holes are covered. */
public class VialScreen extends AbstractContainerScreen<VialMenu> {
    private static final Identifier HOPPER = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
    private static final int PANEL = 0xFFC6C6C6;

    public VialScreen(VialMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, HOPPER, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        graphics.fill(xo + 43, yo + 19, xo + 79, yo + 39, PANEL);
        graphics.fill(xo + 97, yo + 19, xo + 151, yo + 39, PANEL);
    }
}
