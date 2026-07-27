package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameResultStateTest {

    @Test
    void resultContentIsCenteredAsAGroup() {
        assertEquals(App.INTERNAL_CANVAS_WIDTH * 0.5F, GameResultState.RESULT_MESSAGE_X);
        assertEquals(GameResultState.RESULT_MESSAGE_X, GameResultState.RESET_PROMPT_X);
        assertEquals(App.INTERNAL_CANVAS_HEIGHT * 0.5F, GameResultState.resultGroupCenterY());
        assertEquals(App.INTERNAL_CANVAS_HEIGHT * 0.5F, GameResultState.tacticalResultGroupCenterY());
        assertEquals(80.0F, GameResultState.RESET_PROMPT_Y - GameResultState.RESULT_MESSAGE_Y);
    }

    @Test
    void resultRetainsOnlyACompletedTacticalFinish() {
        TacticalEvent finish = new TacticalEvent(PlayerSide.ONE, TacticalEventType.FINISH, 30);
        GameResultState state = new GameResultState(new App(), "You win.", finish);

        assertEquals(finish, state.getFinishFeedback());
    }
}
