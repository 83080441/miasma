package dev.sdfg.mod.client;

import dev.sdfg.mod.element.BlockResidue;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/** Keeps a gray dusting on blocks the wand has drained but not emptied. */
public final class DrainWash {
    private static final int GRAY = 0xFF8A8A8A;

    private DrainWash() {
    }

    public static void tick(Player player) {
        if (!(player.level() instanceof ClientLevel level) || player.tickCount % 4 != 0) {
            return;
        }
        ColorParticleOption gray = ColorParticleOption.create(ModParticles.DRAIN_WASH.get(), GRAY);
        RandomSource random = level.getRandom();
        BlockResidue.visitNearby(level, player.blockPosition(), 2, pos -> {
            float loss = BlockResidue.loss(level, pos);
            if (loss < 0.05F || level.getBlockState(pos).isAir()) {
                return;
            }
            int count = 1 + Math.round(loss * 5.0F);
            for (int i = 0; i < count; i++) {
                level.addParticle(
                        gray,
                        pos.getX() + random.nextDouble(),
                        pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(),
                        0.0,
                        0.0,
                        0.0
                );
            }
        });
    }
}
