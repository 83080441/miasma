package dev.sdfg.mod.block;

import java.util.ArrayList;
import java.util.List;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Locks onto one present element from the tank above or a networked container,
 * then drips it into the air block below. Ten drips finish a solid crystal when
 * a sturdy block sits under that empty space.
 */
public class CondensationFilterBlockEntity extends BlockEntity {
    /** Liquid taken from the source per successful drip. */
    public static final int DROP_AMOUNT = 10;
    /** Soonest wait between drips, in ticks. */
    public static final int DRIP_MIN = 20;
    /** Latest wait between drips, in ticks. */
    public static final int DRIP_MAX = 40;

    private Element locked;
    private int cooldown;

    public CondensationFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSATION_FILTER.get(), pos, state);
        this.cooldown = DRIP_MIN;
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        this.cooldown = nextWait(level.getRandom());

        ElementContainerBlockEntity source = findSource(level);
        if (source == null || source.contents().isEmpty()) {
            return;
        }

        BlockPos crystalPos = this.worldPosition.below();
        BlockPos supportPos = crystalPos.below();
        BlockState crystalState = level.getBlockState(crystalPos);
        BlockState support = level.getBlockState(supportPos);
        if (!support.isFaceSturdy(level, supportPos, Direction.UP)) {
            return;
        }

        Element element = resolveElement(level, source, crystalState);
        if (element == null || source.contents().get(element) <= 0) {
            return;
        }
        if (crystalState.getBlock() instanceof ElementCrystalBlock) {
            if (ElementCrystalBlock.elementOf(crystalState) != element) {
                return;
            }
            if (ElementCrystalBlock.isSolid(crystalState)) {
                return;
            }
        } else if (!crystalState.isAir()) {
            return;
        }

        int taken = source.drain(element, DROP_AMOUNT);
        if (taken <= 0) {
            return;
        }
        this.locked = element;
        growCrystal(level, crystalPos, crystalState, element);
        dripFx(level, element);
        this.setChanged();
    }

    private Element resolveElement(Level level, ElementContainerBlockEntity source, BlockState crystalState) {
        if (crystalState.getBlock() instanceof ElementCrystalBlock) {
            return ElementCrystalBlock.elementOf(crystalState);
        }
        if (this.locked != null && source.contents().get(this.locked) > 0) {
            return this.locked;
        }
        List<Element> present = new ArrayList<>(source.contents().present());
        if (present.isEmpty()) {
            this.locked = null;
            return null;
        }
        Element picked = present.get(level.getRandom().nextInt(present.size()));
        this.locked = picked;
        return picked;
    }

    private static void growCrystal(Level level, BlockPos crystalPos, BlockState crystalState, Element element) {
        if (crystalState.isAir()) {
            level.setBlock(
                    crystalPos,
                    ElementCrystalBlock.fresh(element),
                    Block.UPDATE_ALL
            );
            return;
        }
        if (crystalState.getBlock() instanceof ElementCrystalBlock) {
            level.setBlock(crystalPos, ElementCrystalBlock.grow(crystalState), Block.UPDATE_ALL);
        }
    }

    private ElementContainerBlockEntity findSource(Level level) {
        BlockPos above = this.worldPosition.above();
        if (level.getBlockEntity(above) instanceof ElementContainerBlockEntity tank
                && !tank.contents().isEmpty()) {
            return tank;
        }
        BlockPos networked = PipeNetwork.findContainer(level, this.worldPosition);
        if (networked != null && level.getBlockEntity(networked) instanceof ElementContainerBlockEntity tank) {
            return tank;
        }
        return null;
    }

    private void dripFx(Level level, Element element) {
        level.playSound(null, this.worldPosition, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.35F, 1.2F);
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int color = 0xFF000000 | element.color();
        server.sendParticles(
                ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), color),
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.15,
                this.worldPosition.getZ() + 0.5,
                3, 0.05, 0.15, 0.05, 0.01
        );
        server.sendParticles(
                ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), color),
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() - 0.35,
                this.worldPosition.getZ() + 0.5,
                2, 0.04, 0.2, 0.04, 0.0
        );
    }

    private static int nextWait(RandomSource random) {
        return DRIP_MIN + random.nextInt(DRIP_MAX - DRIP_MIN + 1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.locked != null) {
            output.putString("Locked", this.locked.id());
        }
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String lockedId = input.getStringOr("Locked", "");
        this.locked = lockedId.isEmpty() ? null : Element.byId(lockedId);
        this.cooldown = Math.max(0, input.getIntOr("Cooldown", DRIP_MIN));
    }
}
