package com.likanug.dual.state;

import com.likanug.dual.App;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameResultStateTest {

    @Test
    void resultContentIsCenteredAsAGroup() {
        assertEquals(App.INTERNAL_CANVAS_WIDTH * 0.5F, GameResultState.RESULT_MESSAGE_X);
        assertEquals(GameResultState.RESULT_MESSAGE_X, GameResultState.RESET_PROMPT_X);
        assertEquals(App.INTERNAL_CANVAS_HEIGHT * 0.5F, GameResultState.resultGroupCenterY());
        assertEquals(80.0F, GameResultState.RESET_PROMPT_Y - GameResultState.RESULT_MESSAGE_Y);
    }
}
