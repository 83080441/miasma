package dev.sdfg.mod.block;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.inventory.VialMenu;
import dev.sdfg.mod.item.VialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores a mix of elements, and one vial slot that can draw a 100-unit share. */
public class ElementContainerBlockEntity extends BlockEntity implements Container, MenuProvider {
    private ElementAmounts contents = ElementAmounts.empty();
    private ItemStack vial = ItemStack.EMPTY;

    public ElementContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELEMENT_CONTAINER.get(), pos, state);
    }

    public ElementAmounts contents() {
        return this.contents;
    }

    /** Accepts as much of one element as the cap allows. Returns how much was stored. */
    public int insert(Element element, int amount) {
        if (element == null || amount <= 0) {
            return 0;
        }
        int space = ElementAmounts.MAX_AMOUNT - this.contents.get(element);
        if (space <= 0) {
            return 0;
        }
        int moved = Math.min(amount, space);
        this.contents.add(element, moved);
        Level level = this.level;
        if (level != null) {
            ElementContainerBlock.applyFill(level, this.worldPosition, this.contents);
        }
        this.sync();
        return moved;
    }

    /** Removes up to {@code amount} of one element. Returns how much left the tank. */
    public int drain(Element element, int amount) {
        if (element == null || amount <= 0) {
            return 0;
        }
        int have = this.contents.get(element);
        int take = Math.min(have, amount);
        if (take <= 0) {
            return 0;
        }
        this.contents.set(element, have - take);
        Level level = this.level;
        if (level != null) {
            ElementContainerBlock.applyFill(level, this.worldPosition, this.contents);
        }
        this.sync();
        return take;
    }

    /** Removes the drawn share. Called when a vial is filled. */
    public void extract(ElementAmounts drawn) {
        if (drawn == null || drawn.isEmpty()) {
            return;
        }
        for (Element element : Element.values()) {
            int take = drawn.get(element);
            if (take <= 0) {
                continue;
            }
            this.contents.set(element, Math.max(0, this.contents.get(element) - take));
        }
        Level level = this.level;
        if (level != null) {
            ElementContainerBlock.applyFill(level, this.worldPosition, this.contents);
        }
        this.sync();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.vial.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.vial : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || this.vial.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = this.vial.split(count);
        if (this.vial.isEmpty()) {
            this.vial = ItemStack.EMPTY;
        }
        this.setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = this.vial;
        this.vial = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        Level level = this.level;
        if (level != null && !level.isClientSide() && VialItem.isEmptyVial(stack)) {
            this.vial = VialItem.fillFrom(this, stack);
        } else {
            this.vial = stack;
        }
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return VialItem.isEmptyVial(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.vial = ItemStack.EMPTY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.sdfg.element_container");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VialMenu(containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }

    private void sync() {
        this.setChanged();
        Level level = this.level;
        if (level != null && !level.isClientSide()) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.contents.write(output);
        if (!this.vial.isEmpty()) {
            output.store("Vial", ItemStack.CODEC, this.vial);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contents = ElementAmounts.read(input);
        this.vial = input.read("Vial", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        Level level = this.level;
        if (level != null && !level.isClientSide()) {
            ElementContainerBlock.applyFill(level, this.worldPosition, this.contents);
        }
        if (level != null && level.isClientSide()) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
