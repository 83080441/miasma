package dev.sdfg.mod.inventory;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.item.VialItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** One slot: an empty vial goes in, and the container fills it. */
public class VialMenu extends AbstractContainerMenu {
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 20;
    private final Container container;

    public VialMenu(int containerId, Inventory inventory, Container container) {
        super(ExampleMod.VIAL_MENU.get(), containerId);
        this.container = container;
        this.addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return VialItem.isEmptyVial(stack);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 51);
    }

    public static VialMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        Container container = blockEntity instanceof Container found ? found : new net.minecraft.world.SimpleContainer(1);
        return new VialMenu(containerId, inventory, container);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }
}
