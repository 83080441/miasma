package dev.sdfg.mod.block;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores a single mix of elements delivered by the pipe network. */
public class ElementContainerBlockEntity extends BlockEntity {
    private ElementAmounts contents = ElementAmounts.empty();

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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contents = ElementAmounts.read(input);
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
