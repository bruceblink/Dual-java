package com.likanug.dual.inputDevice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputDeviceTest {

    @Test
    void shortShotPressEdgeIsReportedForOneUpdateOnly() {
        InputDevice input = new InputDevice();

        input.operateShotButton(true);
        assertTrue(input.isShotButtonJustPressed());

        input.operateShotButton(true);
        assertFalse(input.isShotButtonJustPressed());

        input.operateShotButton(false);
        input.operateShotButton(true);
        assertTrue(input.isShotButtonJustPressed());
    }

    @Test
    void longShotEdgesAreReportedForOneUpdateOnly() {
        InputDevice input = new InputDevice();

        input.operateLongShotButton(true);
        assertTrue(input.isLongShotButtonJustPressed());
        assertFalse(input.isLongShotButtonJustReleased());

        input.operateLongShotButton(true);
        assertFalse(input.isLongShotButtonJustPressed());
        assertFalse(input.isLongShotButtonJustReleased());

        input.operateLongShotButton(false);
        assertFalse(input.isLongShotButtonJustPressed());
        assertTrue(input.isLongShotButtonJustReleased());

        input.operateLongShotButton(false);
        assertFalse(input.isLongShotButtonJustReleased());
    }
}
