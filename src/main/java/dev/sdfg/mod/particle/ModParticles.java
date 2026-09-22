package dev.sdfg.mod.particle;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WARP_MOTE =
            PARTICLE_TYPES.register("warp_mote", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
