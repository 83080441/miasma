package dev.sdfg.mod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/** Short bubble that rises inside a heated cauldron of vanilla water. */
public class CauldronBubbleParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    public CauldronBubbleParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            ColorParticleOption options
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.rCol = options.getRed();
        this.gCol = options.getGreen();
        this.bCol = options.getBlue();
        this.hasPhysics = false;
        this.gravity = -0.01F;
        this.friction = 0.98F;
        this.lifetime = 12 + this.random.nextInt(8);
        this.quadSize = 0.04F + this.random.nextFloat() * 0.02F;
        this.xd = (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = 0.02 + this.random.nextDouble() * 0.02;
        this.zd = (this.random.nextDouble() - 0.5) * 0.01;
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
        if (this.age > this.lifetime * 0.7F) {
            this.alpha = (this.lifetime - this.age) / (this.lifetime * 0.3F);
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
            return new CauldronBubbleParticle(level, x, y, z, this.sprites, type);
        }
    }
}
