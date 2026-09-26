package dev.sdfg.mod.block;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.element.ElementLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Water in the bowl, plus anything an item has added to it.
 * Once an item is absorbed, the water stays.
 */
public class HeatedCauldronBlockEntity extends BlockEntity {
    private ElementAmounts contents = ElementAmounts.empty();
    private boolean sealed;

    public HeatedCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAULDRON.get(), pos, state);
    }

    public ElementAmounts contents() {
        return this.contents;
    }

    public boolean isSealed() {
        return this.sealed;
    }

    /** Fresh vanilla water. Can still be bucketed out. */
    public void fillWater() {
        this.contents = ElementAmounts.of(Element.WATER, ElementAmounts.MAX_AMOUNT);
        this.sealed = false;
        this.sync();
    }

    public void clearBrew() {
        this.contents = ElementAmounts.empty();
        this.sealed = false;
        this.sync();
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos pos = this.worldPosition;
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(HeatedCauldronBlock.LEVEL) || state.getValue(HeatedCauldronBlock.LEVEL) <= 0) {
            return;
        }
        // Full footprint plus a bit above the rim: thrown items rest on the lip, not in the hole.
        AABB bowl = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
        boolean absorbed = false;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bowl)) {
            if (!item.isAlive()) {
                continue;
            }
            ElementAmounts added = ElementLookup.of(item.getItem());
            for (Element element : Element.values()) {
                int amount = added.get(element);
                if (amount > 0) {
                    this.contents.add(element, amount);
                }
            }
            item.discard();
            absorbed = true;
        }
        if (!absorbed) {
            return;
        }
        this.sealed = true;
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.0F);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.contents.write(output);
        output.putBoolean("Sealed", this.sealed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.contents = ElementAmounts.read(input);
        this.sealed = input.getBooleanOr("Sealed", false);
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
