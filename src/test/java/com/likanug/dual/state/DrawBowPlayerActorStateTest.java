package com.likanug.dual.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawBowPlayerActorStateTest {

    @Test
    void shortbowFiresImmediatelyOnAPressAndRespectsItsOwnCooldown() {
        assertTrue(DrawShortbowPlayerActorState.canFire(true, 0));
        assertFalse(DrawShortbowPlayerActorState.canFire(false, 0));
        assertFalse(DrawShortbowPlayerActorState.canFire(true, 1));

        assertEquals(11, DrawShortbowPlayerActorState.tickCooldown(12));
        assertEquals(0, DrawShortbowPlayerActorState.tickCooldown(0));
    }

    @Test
    void longbowChargeProgressIsContinuousAndClamped() {
        assertEquals(0.0F, DrawLongbowPlayerActorState.calculateChargeProgress(-1, 30));
        assertEquals(0.5F, DrawLongbowPlayerActorState.calculateChargeProgress(15, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(30, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(31, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(0, 0));
    }
}
