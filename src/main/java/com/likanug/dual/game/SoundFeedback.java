package com.likanug.dual.game;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Produces tiny generated cues without adding a binary asset or a platform-specific audio dependency.
 * Audio failures are ignored because sound is feedback only and must never stop deterministic combat.
 */
public final class SoundFeedback {

    private static final int SAMPLE_RATE = 44_100;
    private static final int TONE_DURATION_MILLIS = 80;
    private static final int TONE_FREQUENCY_HZ = 880;
    private static final long MINIMUM_REPEAT_NANOS = 100_000_000L;
    private static volatile long lastChargeReadyNanos;
    private static volatile float volume = 1.0F;

    private SoundFeedback() {
    }

    /** Starts one short charge-ready cue on a daemon thread so the game loop never waits for audio I/O. */
    public static void playChargeReady() {
        if (volume <= 0.0F) return;
        final long now = System.nanoTime();
        if (now - lastChargeReadyNanos < MINIMUM_REPEAT_NANOS) return;
        lastChargeReadyNanos = now;
        Thread soundThread = new Thread(SoundFeedback::playTone, "dual-charge-ready-sound");
        soundThread.setDaemon(true);
        soundThread.start();
    }

    /** Applies the user-selected feedback volume without changing the deterministic game rules. */
    public static void setVolume(float newVolume) {
        volume = Math.max(0.0F, Math.min(1.0F, newVolume));
    }

    public static float getVolume() {
        return volume;
    }

    /** Creates a deterministic mono PCM tone and quietly skips it when no audio line is available. */
    private static void playTone() {
        final AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        final byte[] samples = createToneSamples();
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, samples.length);
            line.start();
            line.write(samples, 0, samples.length);
            line.drain();
        } catch (Exception ignored) {
            // Audio is optional feedback; unsupported or unavailable devices must not affect gameplay.
        }
    }

    /** Returns the signed PCM samples used by the cue so its duration and waveform remain testable. */
    static byte[] createToneSamples() {
        return createToneSamples(volume);
    }

    /** Builds the same cue at a clamped amplitude so muted settings remain completely silent. */
    static byte[] createToneSamples(float requestedVolume) {
        final int sampleCount = SAMPLE_RATE * TONE_DURATION_MILLIS / 1000;
        final byte[] samples = new byte[sampleCount];
        final float clampedVolume = Math.max(0.0F, Math.min(1.0F, requestedVolume));
        for (int index = 0; index < sampleCount; index++) {
            double phase = 2.0 * Math.PI * TONE_FREQUENCY_HZ * index / SAMPLE_RATE;
            samples[index] = (byte) Math.round(Math.sin(phase) * 90.0F * clampedVolume);
        }
        return samples;
    }
}
