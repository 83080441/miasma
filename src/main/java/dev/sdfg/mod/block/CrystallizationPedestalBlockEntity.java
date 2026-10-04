package dev.sdfg.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Single quartz gem shown on the crystallization pedestal. */
public class CrystallizationPedestalBlockEntity extends BlockEntity {
    private ItemStack quartz = ItemStack.EMPTY;

    public CrystallizationPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZATION_PEDESTAL.get(), pos, state);
    }

    public boolean hasQuartz() {
        return !this.quartz.isEmpty() && this.quartz.is(Items.QUARTZ);
    }

    public ItemStack quartz() {
        return this.quartz;
    }

    public void setQuartz(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.QUARTZ)) {
            this.quartz = ItemStack.EMPTY;
        } else {
            this.quartz = stack.copyWithCount(1);
        }
        this.sync();
    }

    public ItemStack takeQuartz() {
        ItemStack taken = this.quartz;
        this.quartz = ItemStack.EMPTY;
        this.sync();
        return taken;
    }

    /** Used when the first crystal bud forms on top of the gem. */
    public void consumeQuartz() {
        if (this.quartz.isEmpty()) {
            return;
        }
        this.quartz = ItemStack.EMPTY;
        this.sync();
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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.quartz.isEmpty()) {
            Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.quartz);
            this.quartz = ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.quartz.isEmpty()) {
            output.store("Quartz", ItemStack.CODEC, this.quartz);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.quartz = input.read("Quartz", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!this.quartz.isEmpty() && !this.quartz.is(Items.QUARTZ)) {
            this.quartz = ItemStack.EMPTY;
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
