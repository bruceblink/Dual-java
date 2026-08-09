package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static processing.core.PConstants.HALF_PI;

class HumanPlayerEngineTest {

    @Test
    void runCombinesWasdMouseAimAndMouseWeaponsIntoPlayerIntent() {
        KeyInput rawInput = new KeyInput();
        rawInput.isWPressed = true;
        rawInput.isDPressed = true;
        rawInput.setMouseLongShotPressed(true);
        rawInput.updateMouseAim(100.0F, 200.0F);
        HumanPlayerEngine engine = new HumanPlayerEngine(rawInput);
        PlayerActor player = new PlayerActor(engine, 255, new App());
        player.setxPosition(100.0F);
        player.setyPosition(100.0F);

        engine.run(player);

        AbstractInputDevice intent = engine.getControllingInputDevice();
        assertEquals(1, intent.getHorizontalMoveButton());
        assertEquals(-1, intent.getVerticalMoveButton());
        assertTrue(intent.isLongShotButtonPressed());
        assertTrue(intent.hasAimAngle());
        assertEquals(HALF_PI, intent.getAimAngle(), 1.0E-6F);
    }

    @Test
    void runRetainsKeyboardWeaponCompatibilityWithoutMouseAim() {
        KeyInput rawInput = new KeyInput();
        rawInput.isZPressed = true;
        rawInput.isXPressed = true;
        HumanPlayerEngine engine = new HumanPlayerEngine(rawInput);
        PlayerActor player = new PlayerActor(engine, 255, new App());

        engine.run(player);

        AbstractInputDevice intent = engine.getControllingInputDevice();
        assertTrue(intent.isShotButtonPressed());
        assertTrue(intent.isLongShotButtonPressed());
        assertFalse(intent.hasAimAngle());
    }

    @Test
    void runUsesKeyboardAimWhenMouseAimIsUnavailable() {
        KeyInput rawInput = new KeyInput();
        rawInput.isAimUpPressed = true;
        rawInput.isAimRightPressed = true;
        HumanPlayerEngine engine = new HumanPlayerEngine(rawInput);
        PlayerActor player = new PlayerActor(engine, 255, new App());

        engine.run(player);

        assertTrue(engine.getControllingInputDevice().hasAimAngle());
        assertEquals(-0.7853982F, engine.getControllingInputDevice().getAimAngle(), 1.0E-6F);
    }
}
