package dev.sdfg.mod.block;

import java.util.ArrayList;
import java.util.List;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Locks onto one present element from the tank above or a networked container,
 * then drips it like cave dripstone into the air block below. Transfer chance
 * matches pointed dripstone filling a cauldron (water / lava odds).
 */
public class CondensationFilterBlockEntity extends BlockEntity {
    /** Liquid taken from the source per successful drip. */
    public static final int DROP_AMOUNT = 10;
    /**
     * Same as vanilla: {@code 50 + fallDistance}. Our tip to crystal gap is 1 block.
     */
    public static final int FALL_DELAY = 50 + 1;
    /** Spout tip Y inside the filter block (pixels / 16). */
    public static final double TIP_Y = 2.0 / 16.0;

    private Element locked;
    /** Element currently in flight as a hanging/falling drop. */
    private Element pendingElement;

    public CondensationFilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSATION_FILTER.get(), pos, state);
    }

    /**
     * Called from the block's {@code randomTick}. {@code randomValue} is one
     * {@link net.minecraft.util.RandomSource#nextFloat()} like dripstone uses.
     */
    public void maybeBeginDrip(float randomValue) {
        Level level = this.level;
        if (!(level instanceof ServerLevel server) || this.pendingElement != null) {
            return;
        }

        DripPlan plan = planDrip(level);
        if (plan == null) {
            return;
        }
        float chance = transferChance(plan.element);
        if (randomValue >= chance) {
            return;
        }

        this.locked = plan.element;
        this.pendingElement = plan.element;
        spawnHangDrop(server, plan.element);
        server.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), FALL_DELAY);
        this.setChanged();
    }

    /** Called when the scheduled fall delay finishes. */
    public void landDrop() {
        Level level = this.level;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        Element element = this.pendingElement;
        this.pendingElement = null;
        if (element == null) {
            this.setChanged();
            return;
        }

        DripPlan plan = planDrip(level);
        if (plan == null || plan.element != element) {
            this.setChanged();
            return;
        }

        int taken = plan.source.drain(element, DROP_AMOUNT);
        if (taken <= 0) {
            this.setChanged();
            return;
        }
        if (plan.starting) {
            plan.pedestal.consumeQuartz();
        }
        growCrystal(level, plan.crystalPos, plan.crystalState, element);
        level.playSound(
                null,
                plan.crystalPos,
                SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                SoundSource.BLOCKS,
                0.4F,
                0.9F + level.getRandom().nextFloat() * 0.2F
        );
        this.setChanged();
    }

    private DripPlan planDrip(Level level) {
        ElementContainerBlockEntity source = findSource(level);
        if (source == null || source.contents().isEmpty()) {
            return null;
        }

        BlockPos crystalPos = this.worldPosition.below();
        BlockPos supportPos = crystalPos.below();
        BlockState crystalState = level.getBlockState(crystalPos);
        if (!(level.getBlockEntity(supportPos) instanceof CrystallizationPedestalBlockEntity pedestal)) {
            return null;
        }
        boolean starting = crystalState.isAir();
        if (starting && !pedestal.hasQuartz()) {
            return null;
        }

        Element element = resolveElement(level, source, crystalState);
        if (element == null || source.contents().get(element) <= 0) {
            return null;
        }
        if (crystalState.getBlock() instanceof ElementCrystalBlock) {
            if (ElementCrystalBlock.elementOf(crystalState) != element) {
                return null;
            }
            if (ElementCrystalBlock.isSolid(crystalState)) {
                return null;
            }
        } else if (!starting) {
            return null;
        }
        return new DripPlan(source, pedestal, crystalPos, crystalState, element, starting);
    }

    /** Same odds as dripstone lava into a cauldron (~5.9% per random tick). */
    public static float transferChance(Element element) {
        return PointedDripstoneBlock.LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK;
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
            level.setBlock(crystalPos, ElementCrystalBlock.fresh(element), Block.UPDATE_ALL);
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

    private void spawnHangDrop(ServerLevel server, Element element) {
        server.sendParticles(
                dripOf(element),
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + TIP_Y,
                this.worldPosition.getZ() + 0.5,
                1, 0.0, 0.0, 0.0, 0.0
        );
    }

    /** Hang drip tinted with the element / liquid color. */
    public static ColorParticleOption dripOf(Element element) {
        return ColorParticleOption.create(ModParticles.ELEMENT_DRIP_HANG.get(), 0xFF000000 | element.color());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.locked != null) {
            output.putString("Locked", this.locked.id());
        }
        if (this.pendingElement != null) {
            output.putString("Pending", this.pendingElement.id());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String lockedId = input.getStringOr("Locked", "");
        this.locked = lockedId.isEmpty() ? null : Element.byId(lockedId);
        String pendingId = input.getStringOr("Pending", "");
        this.pendingElement = pendingId.isEmpty() ? null : Element.byId(pendingId);
    }

    private record DripPlan(
            ElementContainerBlockEntity source,
            CrystallizationPedestalBlockEntity pedestal,
            BlockPos crystalPos,
            BlockState crystalState,
            Element element,
            boolean starting
    ) {
    }
}
