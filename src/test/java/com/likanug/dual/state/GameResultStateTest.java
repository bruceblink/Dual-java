package com.likanug.dual.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameResultStateTest {

    @Test
    void resultAndResetPromptAreCenteredAsAGroup() {
        assertEquals(0.0F, GameResultState.resultGroupCenterY());
        assertEquals(-GameResultState.RESULT_MESSAGE_Y, GameResultState.RESET_PROMPT_Y);
    }
}
