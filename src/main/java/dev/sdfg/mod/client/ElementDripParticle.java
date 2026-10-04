package dev.sdfg.mod.client;

import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/**
 * Cave-style hang then fall drip, tinted with an element color.
 * Hang lasts ~40 ticks, then spawns {@link ModParticles#ELEMENT_DRIP_FALL}.
 */
public class ElementDripParticle extends SingleQuadParticle {
    private ElementDripParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            TextureAtlasSprite sprite,
            ColorParticleOption options
    ) {
        super(level, x, y, z, sprite);
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.rCol = options.getRed();
        this.gCol = options.getGreen();
        this.bCol = options.getBlue();
        this.alpha = 1.0F;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.preMoveUpdate();
        if (!this.removed) {
            this.yd -= this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.postMoveUpdate();
            if (!this.removed) {
                this.xd *= 0.98F;
                this.yd *= 0.98F;
                this.zd *= 0.98F;
            }
        }
    }

    protected void preMoveUpdate() {
        if (this.lifetime-- <= 0) {
            this.remove();
        }
    }

    protected void postMoveUpdate() {
    }

    private static final class Hang extends ElementDripParticle {
        private final int color;

        Hang(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, ColorParticleOption options) {
            super(level, x, y, z, sprite, options);
            this.color = pack(options);
            this.gravity *= 0.02F;
            this.lifetime = 40;
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(
                        ColorParticleOption.create(ModParticles.ELEMENT_DRIP_FALL.get(), this.color),
                        this.x,
                        this.y,
                        this.z,
                        this.xd,
                        this.yd,
                        this.zd
                );
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02;
            this.yd *= 0.02;
            this.zd *= 0.02;
        }
    }

    private static final class Fall extends ElementDripParticle {
        Fall(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, ColorParticleOption options) {
            super(level, x, y, z, sprite, options);
            this.lifetime = (int) (64.0 / (this.random.nextFloat() * 0.8 + 0.2));
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
            }
        }
    }

    private static int pack(ColorParticleOption options) {
        int r = Math.round(options.getRed() * 255.0F);
        int g = Math.round(options.getGreen() * 255.0F);
        int b = Math.round(options.getBlue() * 255.0F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static class HangProvider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public HangProvider(SpriteSet sprites) {
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
            return new Hang(level, x, y, z, this.sprites.get(random), type);
        }
    }

    public static class FallProvider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public FallProvider(SpriteSet sprites) {
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
            return new Fall(level, x, y, z, this.sprites.get(random), type);
        }
    }
}
