package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComputerPlayerEngineTest {

    @Test
    void engineKeepsTheSelectedProfileAndReactionCadence() {
        ComputerPlayerEngine engine = new ComputerPlayerEngine(new App(), AiDifficulty.ADVANCED);

        assertEquals(AiDifficulty.ADVANCED, engine.getDifficulty());
        assertEquals(5, engine.getPlanUpdateFrameCount());
    }
}
