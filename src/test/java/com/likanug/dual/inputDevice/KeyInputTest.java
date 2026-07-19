package com.likanug.dual.inputDevice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class KeyInputTest {

    @Test
    void clearReleasesEveryAction() {
        KeyInput input = new KeyInput();
        input.isUpPressed = true;
        input.isDownPressed = true;
        input.isLeftPressed = true;
        input.isRightPressed = true;
        input.isZPressed = true;
        input.isXPressed = true;

        input.clear();

        assertFalse(input.isUpPressed);
        assertFalse(input.isDownPressed);
        assertFalse(input.isLeftPressed);
        assertFalse(input.isRightPressed);
        assertFalse(input.isZPressed);
        assertFalse(input.isXPressed);
    }
}
