package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundFeedbackTest {

    @Test
    void chargeReadyToneHasTheConfiguredShortDeterministicShape() {
        byte[] samples = SoundFeedback.createToneSamples();

        assertEquals(3_528, samples.length);
        assertEquals(0, samples[0]);
        assertTrue(samples[100] != 0);
    }
}
