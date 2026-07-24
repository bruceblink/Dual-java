package com.likanug.dual.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static processing.core.PConstants.HALF_PI;

class PlayGameStateTest {

    @Test
    void shortbowAmmoHudKeepsTheAvailableAndMaximumValuesReadable() {
        assertEquals("Shortbow 3 / 3", PlayGameState.shortbowAmmoDisplayLabel(3, 3));
        assertEquals("Shortbow 0 / 3", PlayGameState.shortbowAmmoDisplayLabel(0, 3));
    }

    @Test
    void calculateThrustAngleUsesCenteredRandomOffset() {
        float base = 1.25f;

        assertEquals(base - HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 0.0f), 1e-6);
        assertEquals(base, PlayGameState.calculateThrustAngle(base, 0.5f), 1e-6);
        assertEquals(base + HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 1.0f), 1e-6);
    }
}
