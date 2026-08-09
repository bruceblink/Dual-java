package com.likanug.dual.actor.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortbowAmmoTest {

    @Test
    void reserveStartsFullAndRejectsFiringAfterAllThreeArrowsAreSpent() {
        ShortbowAmmo ammo = new ShortbowAmmo(3, 5);

        assertEquals(3, ammo.getAvailableAmmo());
        assertTrue(ammo.consume());
        assertTrue(ammo.consume());
        assertTrue(ammo.consume());
        assertFalse(ammo.canFire());
        assertFalse(ammo.consume());
        assertEquals(0, ammo.getAvailableAmmo());
    }

    @Test
    void eachRecoveryIntervalRestoresExactlyOneArrow() {
        ShortbowAmmo ammo = new ShortbowAmmo(3, 3);
        ammo.consume();
        ammo.consume();

        ammo.tickRecovery();
        ammo.tickRecovery();
        assertEquals(1, ammo.getAvailableAmmo());

        ammo.tickRecovery();
        assertEquals(2, ammo.getAvailableAmmo());

        ammo.tickRecovery();
        ammo.tickRecovery();
        ammo.tickRecovery();
        assertEquals(3, ammo.getAvailableAmmo());
    }

    @Test
    void firingAgainRestartsThePartialRecoveryTimer() {
        ShortbowAmmo ammo = new ShortbowAmmo(3, 3);
        ammo.consume();
        ammo.tickRecovery();
        ammo.tickRecovery();
        ammo.consume();

        ammo.tickRecovery();
        ammo.tickRecovery();
        assertEquals(1, ammo.getAvailableAmmo());
        ammo.tickRecovery();
        assertEquals(2, ammo.getAvailableAmmo());
    }

    @Test
    void recoveryProgressReportsTheNextArrowAndResetsWhenItArrives() {
        ShortbowAmmo ammo = new ShortbowAmmo(3, 4);

        assertEquals(0.0F, ammo.getRecoveryProgressRatio());
        ammo.consume();
        assertEquals(0.0F, ammo.getRecoveryProgressRatio());
        ammo.tickRecovery();
        assertEquals(0.25F, ammo.getRecoveryProgressRatio());
        ammo.tickRecovery();
        ammo.tickRecovery();
        ammo.tickRecovery();
        assertEquals(0.0F, ammo.getRecoveryProgressRatio());
    }
}
