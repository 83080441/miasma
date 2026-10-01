package dev.sdfg.mod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/** Colored mote that keeps the velocity it was spawned with, toward the wand or along an arrow. */
public class FlightMoteParticle extends SingleQuadParticle {
    private static final float PEAK_ALPHA = 0.9F;

    private final SpriteSet sprites;

    public FlightMoteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            ColorParticleOption options
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.lifetime = 16 + this.random.nextInt(6);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = 0.055F + this.random.nextFloat() * 0.02F;
        this.rCol = options.getRed();
        this.gCol = options.getGreen();
        this.bCol = options.getBlue();
        this.alpha = 0.35F;
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
        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);
        float life = (float) this.age / (float) this.lifetime;
        if (life < 0.15F) {
            this.alpha = life / 0.15F * PEAK_ALPHA;
        } else if (life > 0.65F) {
            this.alpha = (1.0F - life) / 0.35F * PEAK_ALPHA;
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
            return new FlightMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, type);
        }
    }
}
