package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortbowPressureTest {

    @Test
    void allowsOnlyTheConfiguredNumberOfConsecutiveRefreshes() {
        ShortbowPressure pressure = new ShortbowPressure(2);

        assertTrue(pressure.recordHit());
        assertTrue(pressure.recordHit());
        assertFalse(pressure.recordHit());
        assertEquals(2, pressure.getConsecutiveRefreshes());
    }

    @Test
    void resetStartsANewPressureWindow() {
        ShortbowPressure pressure = new ShortbowPressure(2);
        pressure.recordHit();
        pressure.recordHit();

        pressure.reset();

        assertEquals(0, pressure.getConsecutiveRefreshes());
        assertTrue(pressure.recordHit());
    }

    @Test
    void rejectsAnInvalidRefreshLimit() {
        assertThrows(IllegalArgumentException.class, () -> new ShortbowPressure(0));
    }
}
