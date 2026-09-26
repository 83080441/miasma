package dev.sdfg.mod.fluid;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Finite water-look fluid for one pure element. The body is untinted water;
 * motes use {@link Element#color()}.
 */
public abstract class PureElementFluid extends BaseFlowingFluid {
    private final Element element;

    protected PureElementFluid(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    public Element element() {
        return this.element;
    }

    @Override
    protected void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        float height = state.getOwnHeight();
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + height;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(
                ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), 0xFF000000 | this.element.color()),
                x,
                y,
                z,
                0.0,
                0.012,
                0.0
        );
    }

    public static final class Source extends PureElementFluid {
        public Source(Element element, Properties properties) {
            super(element, properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class Flowing extends PureElementFluid {
        public Flowing(Element element, Properties properties) {
            super(element, properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
