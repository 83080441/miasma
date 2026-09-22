package dev.sdfg.mod.client;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * Warp Echo pitch (and level-path volume backup) for world + music sounds.
 * UI is left alone. Category volume ducking lives in {@link WarpEchoState}.
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpEchoSounds {
    private WarpEchoSounds() {
    }

    @SubscribeEvent
    static void onPlaySound(PlaySoundEvent event) {
        if (WarpEchoState.getEchoIntensity() <= 0.01F) {
            return;
        }

        SoundInstance sound = event.getSound();
        if (sound == null || sound instanceof WarpEchoSoundInstance) {
            return;
        }

        if (sound.getSource() == SoundSource.UI) {
            return;
        }

        event.setSound(WarpEchoSoundInstance.wrap(sound));
    }

    @SubscribeEvent
    static void onPlayLevelSound(PlayLevelSoundEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        float intensity = WarpEchoState.getEchoIntensity();
        if (intensity <= 0.01F) {
            return;
        }
        if (event.getSource() == SoundSource.UI) {
            return;
        }

        // Level.playSound path (blocks, footsteps, animals): pitch here; volume via category gains.
        event.setNewPitch(event.getNewPitch() * WarpEchoSoundInstance.pitchFactor());
    }
}
