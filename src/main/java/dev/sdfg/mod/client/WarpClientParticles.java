package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.entity.WarpEntity;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Pale-violet motes that drift from the gravity-well rim into each warp center.
 * Count and speed scale with {@link WarpEntity#getForce()} (same well as item/living pull).
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpClientParticles {
    /** Match {@link WarpEntity#PULL_RANGE} so motes visualize the same well. */
    private static final double SPAWN_DISTANCE = WarpEntity.PULL_RANGE;
    private static final double MAX_PLAYER_RANGE_SQ = 48.0 * 48.0;

    private WarpClientParticles() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        RandomSource random = level.getRandom();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof WarpEntity warp)) {
                continue;
            }
            if (warp.distanceToSqr(playerPos) > MAX_PLAYER_RANGE_SQ) {
                continue;
            }
            spawnTowardCenter(level, warp, random);
        }
    }

    private static void spawnTowardCenter(ClientLevel level, WarpEntity warp, RandomSource random) {
        float force = warp.getForceFactor();
        // Force 25 ≈ soft trickle; Force 100 ≈ dense inward rain.
        float expected = 0.02F + force * 1.8F;

        Vec3 center = warp.position();
        while (expected > 0.0F) {
            float chance = Math.min(1.0F, expected);
            expected -= 1.0F;
            if (random.nextFloat() > chance) {
                continue;
            }

            float yaw = random.nextFloat() * Mth.TWO_PI;
            float pitch = (float) Math.asin(random.nextFloat() * 2.0F - 1.0F);
            float cp = Mth.cos(pitch);
            double dx = Mth.cos(yaw) * cp;
            double dy = Mth.sin(pitch);
            double dz = Mth.sin(yaw) * cp;

            double distance = SPAWN_DISTANCE + (random.nextDouble() - 0.5) * 1.0;
            double x = center.x + dx * distance;
            double y = center.y + dy * distance;
            double z = center.z + dz * distance;

            // Always slow: Force only thickens the stream, not a rush inward.
            double speed = 0.008 + force * 0.022 + random.nextDouble() * 0.006;
            double vx = -dx * speed;
            double vy = -dy * speed;
            double vz = -dz * speed;

            level.addParticle(ModParticles.WARP_MOTE.get(), x, y, z, vx, vy, vz);
        }
    }
}
