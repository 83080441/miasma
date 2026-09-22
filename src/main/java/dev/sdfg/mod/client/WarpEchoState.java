package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.entity.WarpEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Tracks Warp Echo intensity and ducks SoundSource category gains (volume).
 * Pitch is handled separately by {@link WarpEchoSounds}.
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpEchoState {
    /** Blocks from warp center where the echo is still audible. */
    public static final double MAX_RANGE = 16.0;

    /** Volume multiplier at full intensity (Distortion 100, standing on the warp). */
    public static final float MIN_VOLUME_GAIN = 0.05F;

    private static float echoIntensity;
    private static float lastAppliedGain = 1.0F;

    private WarpEchoState() {
    }

    public static float getEchoIntensity() {
        return echoIntensity;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            echoIntensity = 0.0F;
            applyCategoryGains(0.0F);
            return;
        }

        Vec3 playerPos = minecraft.player.position();
        AABB search = minecraft.player.getBoundingBox().inflate(MAX_RANGE);
        float best = 0.0F;

        for (WarpEntity warp : level.getEntitiesOfClass(WarpEntity.class, search)) {
            double dist = Math.sqrt(warp.distanceToSqr(playerPos));
            if (dist > MAX_RANGE) {
                continue;
            }
            float proximity = Mth.clamp((float) (1.0 - dist / MAX_RANGE), 0.0F, 1.0F);
            // Near-linear proximity so Distortion 50+ is clearly audible.
            float intensity = warp.getDistortionFactor() * proximity;
            if (intensity > best) {
                best = intensity;
            }
        }

        echoIntensity = best;
        applyCategoryGains(best);
    }

    private static void applyCategoryGains(float intensity) {
        float gain = Mth.lerp(intensity, 1.0F, MIN_VOLUME_GAIN);
        if (Math.abs(gain - lastAppliedGain) < 0.005F && intensity > 0.0F && lastAppliedGain < 1.0F) {
            return;
        }
        lastAppliedGain = gain;

        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        for (SoundSource source : SoundSource.values()) {
            if (source == SoundSource.MASTER || source == SoundSource.UI) {
                continue;
            }
            sounds.updateCategoryVolume(source, gain);
        }
    }
}
