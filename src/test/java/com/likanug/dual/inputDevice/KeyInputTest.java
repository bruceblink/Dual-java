package com.likanug.dual.inputDevice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        input.isWPressed = true;
        input.isAPressed = true;
        input.isSPressed = true;
        input.isDPressed = true;
        input.setMouseShotPressed(true);
        input.setMouseLongShotPressed(true);

        input.clear();

        assertFalse(input.isUpPressed);
        assertFalse(input.isDownPressed);
        assertFalse(input.isLeftPressed);
        assertFalse(input.isRightPressed);
        assertFalse(input.isZPressed);
        assertFalse(input.isXPressed);
        assertFalse(input.isWPressed);
        assertFalse(input.isAPressed);
        assertFalse(input.isSPressed);
        assertFalse(input.isDPressed);
        assertFalse(input.isShotPressed());
        assertFalse(input.isLongShotPressed());
    }

    @Test
    void keyboardAliasesRemainPressedUntilBothPhysicalKeysAreReleased() {
        KeyInput input = new KeyInput();
        input.isUpPressed = true;
        input.isWPressed = true;

        input.isUpPressed = false;

        assertTrue(input.isMovingUp());
        input.isWPressed = false;
        assertFalse(input.isMovingUp());
    }

    @Test
    void mouseButtonsShareTheExistingWeaponActions() {
        KeyInput input = new KeyInput();
        input.setMouseShotPressed(true);
        input.setMouseLongShotPressed(true);

        assertTrue(input.isShotPressed());
        assertTrue(input.isLongShotPressed());
    }

    @Test
    void mouseAimKeepsTheLastAcceptedCanvasPosition() {
        KeyInput input = new KeyInput();
        input.updateMouseAim(123.5F, 456.25F);

        assertTrue(input.hasMouseAim());
        assertEquals(123.5F, input.getMouseAimX());
        assertEquals(456.25F, input.getMouseAimY());
    }
}
