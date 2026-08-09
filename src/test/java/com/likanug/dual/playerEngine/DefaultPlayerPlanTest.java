package com.likanug.dual.playerEngine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlayerPlanTest {

    @Test
    void fakeChargeRequiresAnUndamagedTargetAndAdvancedProbabilityRoll() {
        assertFalse(DefaultPlayerPlan.shouldStartFakeCharge(true, 0.0F, AiDifficulty.ADVANCED));
        assertFalse(DefaultPlayerPlan.shouldStartFakeCharge(false, 0.15F, AiDifficulty.ADVANCED));
        assertTrue(DefaultPlayerPlan.shouldStartFakeCharge(false, 0.14F, AiDifficulty.ADVANCED));
        assertFalse(DefaultPlayerPlan.shouldStartFakeCharge(false, 0.01F, AiDifficulty.STANDARD));
    }
}
