package com.likanug.dual.playerEngine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiDifficultyTest {

    @Test
    void profilesIncreaseReactionAndDecisionPressureWithoutChangingRules() {
        assertEquals(18, AiDifficulty.BASIC.getPlanUpdateFrameCount());
        assertEquals(10, AiDifficulty.STANDARD.getPlanUpdateFrameCount());
        assertEquals(5, AiDifficulty.ADVANCED.getPlanUpdateFrameCount());
        assertTrue(AiDifficulty.BASIC.getEvadeProbability() < AiDifficulty.STANDARD.getEvadeProbability());
        assertTrue(AiDifficulty.STANDARD.getEvadeProbability() < AiDifficulty.ADVANCED.getEvadeProbability());
        assertTrue(AiDifficulty.BASIC.getKillAttemptProbability()
                < AiDifficulty.ADVANCED.getKillAttemptProbability());
        assertTrue(AiDifficulty.BASIC.getLongbowReleaseProbability()
                < AiDifficulty.ADVANCED.getLongbowReleaseProbability());
    }
}
