package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundFeedbackTest {

    @Test
    void chargeReadyToneHasTheConfiguredShortDeterministicShape() {
        byte[] samples = SoundFeedback.createToneSamples(1.0F);

        assertEquals(3_528, samples.length);
        assertEquals(0, samples[0]);
        assertTrue(samples[100] != 0);
    }

    @Test
    void toneVolumeScalesAmplitudeAndMuteProducesSilence() {
        byte[] full = SoundFeedback.createToneSamples(1.0F);
        byte[] half = SoundFeedback.createToneSamples(0.5F);
        byte[] muted = SoundFeedback.createToneSamples(0.0F);

        assertEquals(full.length, half.length);
        assertEquals(full.length, muted.length);
        assertTrue(Math.abs(half[100]) <= Math.abs(full[100]));
        for (byte sample : muted) assertEquals(0, sample);
    }
}
