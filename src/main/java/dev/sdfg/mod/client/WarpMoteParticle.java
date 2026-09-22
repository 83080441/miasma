package dev.sdfg.mod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Soft pale-violet mote ({@code #4D4D6B}) that drifts toward the warp center.
 * Homing strength follows inbound speed so low-Force wells stay slow.
 */
public class WarpMoteParticle extends SingleQuadParticle {
    /** Pale violet #4D4D6B */
    private static final float COLOR_R = 0x4D / 255.0F;
    private static final float COLOR_G = 0x4D / 255.0F;
    private static final float COLOR_B = 0x6B / 255.0F;
    private static final float PEAK_ALPHA = 0.32F;

    private final SpriteSet sprites;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float homingStrength;

    public WarpMoteParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SpriteSet sprites
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.97F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed < 1.0E-4) {
            speed = 0.012;
        }
        // Travel along inbound velocity toward the warp (spawn rim ≈ PULL_RANGE).
        double travel = 12.0;
        this.targetX = x + (vx / speed) * travel;
        this.targetY = y + (vy / speed) * travel;
        this.targetZ = z + (vz / speed) * travel;
        // Gentle homing — Force changes density/speed at spawn, not a snap inward.
        this.homingStrength = (float) Mth.clamp(0.004 + speed * 0.35, 0.004, 0.022);

        this.lifetime = Mth.clamp((int) (travel / Math.max(speed, 0.008)) + this.random.nextInt(40), 80, 360);
        this.quadSize = 0.035F + this.random.nextFloat() * 0.025F;
        this.rCol = COLOR_R;
        this.gCol = COLOR_G;
        this.bCol = COLOR_B;
        this.alpha = 0.12F + this.random.nextFloat() * 0.10F;
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

        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.12) {
            this.remove();
            return;
        }

        double pull = this.homingStrength + Math.min(this.homingStrength * 0.5, 0.02 / dist);
        this.xd = this.xd * this.friction + (dx / dist) * pull;
        this.yd = this.yd * this.friction + (dy / dist) * pull;
        this.zd = this.zd * this.friction + (dz / dist) * pull;

        // Hard cap so motes never rush.
        double maxSpeed = 0.045;
        double cur = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        if (cur > maxSpeed) {
            double s = maxSpeed / cur;
            this.xd *= s;
            this.yd *= s;
            this.zd *= s;
        }

        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);

        float life = (float) this.age / (float) this.lifetime;
        if (life < 0.15F) {
            this.alpha = life / 0.15F * PEAK_ALPHA;
        } else if (life > 0.75F) {
            this.alpha = (1.0F - life) / 0.25F * PEAK_ALPHA;
        } else {
            this.alpha = PEAK_ALPHA;
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void move(double xa, double ya, double za) {
        this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
        this.setLocationFromBoundingbox();
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new WarpMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
