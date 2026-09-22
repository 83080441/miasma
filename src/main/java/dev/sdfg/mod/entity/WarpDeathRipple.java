package dev.sdfg.mod.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import dev.sdfg.mod.ExampleMod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dark shockwave emitted when a Warp (or another ripple) kills a mob: falls with gravity,
 * then spreads slowly across the surface like water (radius 14), dealing 7 hearts to mobs.
 * Killing a mob with the ripple spawns a new wave at that position.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class WarpDeathRipple {
    public static final double MAX_RADIUS = 14.0;
    /** Blocks per tick while spreading (~7s to reach 14). */
    private static final double SPREAD_SPEED = 0.10;
    /** Initial downward speed (blocks/tick). */
    private static final double FALL_SPEED = 0.22;
    /** 7 hearts = 14 HP. */
    private static final float MOB_DAMAGE = 14.0F;
    private static final double RING_THICKNESS = 0.75;
    /** Packed RGB — near-black violet. */
    private static final int DUST_COLOR = 0x12101A;
    private static final DustParticleOptions DARK_DUST = new DustParticleOptions(DUST_COLOR, 1.35F);

    private static final List<WarpDeathRipple> ACTIVE = new ArrayList<>();
    /** Queued during tick so chain-kills do not mutate ACTIVE while iterating. */
    private static final List<WarpDeathRipple> PENDING = new ArrayList<>();

    private final ServerLevel level;
    private final double x;
    private final double z;
    private double y;
    private double radius;
    private boolean spreading;
    private final Set<UUID> hit = new HashSet<>();

    private WarpDeathRipple(ServerLevel level, double x, double y, double z) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = 0.0;
        this.spreading = false;
    }

    /** Starts a ripple at the given position (Warp or chain-kill origin). */
    public static void spawn(ServerLevel level, Vec3 origin) {
        PENDING.add(new WarpDeathRipple(level, origin.x, origin.y, origin.z));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
        if (ACTIVE.isEmpty()) {
            return;
        }

        // Snapshot iteration — chain spawns go to PENDING, not ACTIVE mid-loop.
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            if (!ACTIVE.get(i).tick()) {
                ACTIVE.remove(i);
            }
        }
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
    }

    /** @return false when finished */
    private boolean tick() {
        if (this.level.getServer() == null || this.level.getServer().isStopped()) {
            return false;
        }

        if (!this.spreading) {
            return this.tickFall();
        }
        return this.tickSpread();
    }

    private boolean tickFall() {
        this.y -= FALL_SPEED;
        int groundY = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(this.x), Mth.floor(this.z));
        double surface = groundY + 0.15;

        this.level.sendParticles(ParticleTypes.SQUID_INK, this.x, this.y, this.z, 4, 0.12, 0.08, 0.12, 0.01);
        this.level.sendParticles(DARK_DUST, this.x, this.y, this.z, 3, 0.1, 0.06, 0.1, 0.0);
        this.level.sendParticles(ParticleTypes.SMOKE, this.x, this.y, this.z, 2, 0.08, 0.05, 0.08, 0.0);

        if (this.y <= surface) {
            this.y = surface;
            this.spreading = true;
            this.radius = 0.35;
            this.level.sendParticles(ParticleTypes.SQUID_INK, this.x, this.y, this.z, 18, 0.35, 0.05, 0.35, 0.02);
            this.level.sendParticles(DARK_DUST, this.x, this.y, this.z, 12, 0.4, 0.04, 0.4, 0.0);
        }
        return true;
    }

    private boolean tickSpread() {
        this.radius += SPREAD_SPEED;
        if (this.radius > MAX_RADIUS) {
            return false;
        }

        this.spawnRingParticles();
        this.damageMobsOnRing();
        return true;
    }

    private void spawnRingParticles() {
        int samples = Math.max(8, (int) (this.radius * 10.0));
        for (int i = 0; i < samples; i++) {
            double angle = (Math.PI * 2.0 * i) / samples;
            double px = this.x + Mth.cos((float) angle) * this.radius;
            double pz = this.z + Mth.sin((float) angle) * this.radius;
            int gx = Mth.floor(px);
            int gz = Mth.floor(pz);
            double py = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) + 0.12;

            double vx = Mth.cos((float) angle) * 0.02;
            double vz = Mth.sin((float) angle) * 0.02;

            if (i % 2 == 0) {
                this.level.sendParticles(DARK_DUST, px, py, pz, 1, 0.04, 0.02, 0.04, 0.0);
            }
            if (i % 3 == 0) {
                this.level.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 1, 0.03, 0.02, 0.03, 0.0);
            }
            if (i % 4 == 0) {
                this.level.sendParticles(ParticleTypes.SMOKE, px, py + 0.05, pz, 1, vx, 0.01, vz, 0.0);
            }
        }
    }

    private void damageMobsOnRing() {
        double inner = Math.max(0.0, this.radius - RING_THICKNESS);
        double outer = this.radius + RING_THICKNESS;
        AABB box = new AABB(
                this.x - outer, this.y - 1.5, this.z - outer,
                this.x + outer, this.y + 2.5, this.z + outer
        );

        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!living.isAlive() || living instanceof Player || living.isSpectator()) {
                continue;
            }

            double dx = living.getX() - this.x;
            double dz = living.getZ() - this.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz < inner || horiz > outer) {
                continue;
            }
            if (!this.hit.add(living.getUUID())) {
                continue;
            }

            Vec3 deathPos = living.position();
            if (living.hurtServer(this.level, this.level.damageSources().magic(), MOB_DAMAGE)) {
                if (living.isDeadOrDying() || !living.isAlive()) {
                    // Chain: a kill from the ink wave spawns another wave.
                    WarpDeathRipple.spawn(this.level, deathPos);
                }
            }
        }
    }
}
