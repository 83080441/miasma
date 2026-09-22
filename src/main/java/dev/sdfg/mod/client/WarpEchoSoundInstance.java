package dev.sdfg.mod.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Delegates a {@link SoundInstance} while lowering pitch for the Warp Echo.
 * Volume is ducked via {@link WarpEchoState} category gains (avoid double-ducking here).
 */
public class WarpEchoSoundInstance implements SoundInstance {
    /** Pitch multiplier at full intensity (clamped by engine to ≥ 0.5). */
    public static final float MIN_PITCH = 0.55F;

    private final SoundInstance delegate;

    public WarpEchoSoundInstance(SoundInstance delegate) {
        this.delegate = delegate;
    }

    public static SoundInstance wrap(SoundInstance original) {
        if (original instanceof WarpEchoSoundInstance) {
            return original;
        }
        if (original instanceof TickableSoundInstance tickable) {
            return new Tickable(tickable);
        }
        return new WarpEchoSoundInstance(original);
    }

    public static float pitchFactor() {
        return Mth.lerp(WarpEchoState.getEchoIntensity(), 1.0F, MIN_PITCH);
    }

    @Override
    public Identifier getIdentifier() {
        return this.delegate.getIdentifier();
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) {
        return this.delegate.resolve(soundManager);
    }

    @Override
    public @Nullable Sound getSound() {
        return this.delegate.getSound();
    }

    @Override
    public SoundSource getSource() {
        return this.delegate.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.delegate.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.delegate.isRelative();
    }

    @Override
    public int getDelay() {
        return this.delegate.getDelay();
    }

    @Override
    public float getVolume() {
        return this.delegate.getVolume();
    }

    @Override
    public float getPitch() {
        return this.delegate.getPitch() * pitchFactor();
    }

    @Override
    public double getX() {
        return this.delegate.getX();
    }

    @Override
    public double getY() {
        return this.delegate.getY();
    }

    @Override
    public double getZ() {
        return this.delegate.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return this.delegate.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return this.delegate.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return this.delegate.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return this.delegate.getStream(soundBuffers, sound, looping);
    }

    public static final class Tickable extends WarpEchoSoundInstance implements TickableSoundInstance {
        private final TickableSoundInstance tickable;

        public Tickable(TickableSoundInstance tickable) {
            super(tickable);
            this.tickable = tickable;
        }

        @Override
        public boolean isStopped() {
            return this.tickable.isStopped();
        }

        @Override
        public void tick() {
            this.tickable.tick();
        }
    }
}
