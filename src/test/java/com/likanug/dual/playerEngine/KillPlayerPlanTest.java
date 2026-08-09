package com.likanug.dual.playerEngine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillPlayerPlanTest {

    @Test
    void releaseRequiresCompletedChargeAndProfileProbability() {
        assertFalse(KillPlayerPlan.shouldReleaseLongbow(false, 0.0F, AiDifficulty.ADVANCED));
        assertTrue(KillPlayerPlan.shouldReleaseLongbow(true, 0.04F, AiDifficulty.STANDARD));
        assertFalse(KillPlayerPlan.shouldReleaseLongbow(true, 0.05F, AiDifficulty.STANDARD));
        assertTrue(KillPlayerPlan.shouldReleaseLongbow(true, 0.10F, AiDifficulty.ADVANCED));
    }
}
