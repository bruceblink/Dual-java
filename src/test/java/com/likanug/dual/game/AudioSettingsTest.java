package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioSettingsTest {

    @Test
    void volumeLevelsClampAndMuteWithStableLabels() {
        AudioSettings settings = new AudioSettings();

        assertEquals("100%", settings.displayLabel());
        settings.decreaseVolume();
        assertEquals("50%", settings.displayLabel());
        settings.decreaseVolume();
        settings.decreaseVolume();
        assertEquals("Muted", settings.displayLabel());
        settings.increaseVolume();
        assertEquals("50%", settings.displayLabel());
        settings.toggleMute();
        assertEquals("Muted", settings.displayLabel());
        settings.toggleMute();
        assertEquals("100%", settings.displayLabel());
    }
}
