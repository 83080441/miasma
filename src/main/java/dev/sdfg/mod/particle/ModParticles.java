package dev.sdfg.mod.particle;

import com.mojang.serialization.MapCodec;
import dev.sdfg.mod.ExampleMod;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WARP_MOTE =
            PARTICLE_TYPES.register("warp_mote", () -> new SimpleParticleType(false));

    /** Colored mote used by pure-element liquids. RGB comes from {@link dev.sdfg.mod.element.Element#color()}. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ELEMENT_MOTE =
            PARTICLE_TYPES.register("element_mote", ColoredParticleType::new);

    /** Bubble inside a heated cauldron. RGB comes from the water's elements. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> CAULDRON_BUBBLE =
            PARTICLE_TYPES.register("cauldron_bubble", ColoredParticleType::new);

    /** Mote that flies along a velocity, used by the wand drain and the element arrows. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> FLIGHT_MOTE =
            PARTICLE_TYPES.register("flight_mote", ColoredParticleType::new);

    /** Soft gray mote. The client stacks these on a block in proportion to energy it lost. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> DRAIN_WASH =
            PARTICLE_TYPES.register("drain_wash", ColoredParticleType::new);

    /** Hang drip (cave style), tinted with an element color. Spawns {@link #ELEMENT_DRIP_FALL}. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ELEMENT_DRIP_HANG =
            PARTICLE_TYPES.register("element_drip_hang", ColoredParticleType::new);

    /** Falling drip after the hang, tinted with the same element color. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ELEMENT_DRIP_FALL =
            PARTICLE_TYPES.register("element_drip_fall", ColoredParticleType::new);

    private ModParticles() {
    }

    private static final class ColoredParticleType extends ParticleType<ColorParticleOption> {
        private ColoredParticleType() {
            super(false);
        }

        @Override
        public MapCodec<ColorParticleOption> codec() {
            return ColorParticleOption.codec(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
            return (StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption>) (StreamCodec<?, ?>) ColorParticleOption.streamCodec(this);
        }
    }
}
