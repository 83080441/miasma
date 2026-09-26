package dev.sdfg.mod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/** Colored mote that rises about two blocks off a pure-element liquid, like vapor. */
public class ElementMoteParticle extends SingleQuadParticle {
    private static final float PEAK_ALPHA = 0.85F;
    /** Blocks the mote climbs before it fades out. */
    private static final double RISE_BLOCKS = 2.0;

    private final SpriteSet sprites;

    public ElementMoteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            ColorParticleOption options
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.lifetime = 50 + this.random.nextInt(20);
        this.yd = RISE_BLOCKS / this.lifetime;
        this.xd = (this.random.nextDouble() - 0.5) * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.02;
        this.quadSize = 0.04F + this.random.nextFloat() * 0.025F;
        this.rCol = options.getRed();
        this.gCol = options.getGreen();
        this.bCol = options.getBlue();
        this.alpha = 0.2F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.xd += (this.random.nextDouble() - 0.5) * 0.002;
        this.zd += (this.random.nextDouble() - 0.5) * 0.002;
        this.xd *= 0.96;
        this.zd *= 0.96;
        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);

        float life = (float) this.age / (float) this.lifetime;
        if (life < 0.15F) {
            this.alpha = life / 0.15F * PEAK_ALPHA;
        } else if (life > 0.7F) {
            this.alpha = (1.0F - life) / 0.3F * PEAK_ALPHA;
        } else {
            this.alpha = PEAK_ALPHA;
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                ColorParticleOption type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new ElementMoteParticle(level, x, y, z, this.sprites, type);
        }
    }
}
