package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.MatchScore;
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
        assertEquals(160.0F, GameResultState.RESET_PROMPT_Y - GameResultState.RESULT_MESSAGE_Y);
    }

    @Test
    void resultRetainsOnlyACompletedTacticalFinish() {
        TacticalEvent finish = new TacticalEvent(PlayerSide.ONE, TacticalEventType.FINISH, 30);
        GameResultState state = new GameResultState(new App(), "You win.", finish);

        assertEquals(finish, state.getFinishFeedback());
    }

    @Test
    void resultScoreLabelDistinguishesRoundAndMatchOutcomes() {
        MatchScore score = new MatchScore(3);
        MatchScore.RoundResult round = score.recordRoundWin(PlayerSide.ONE);
        assertEquals("ROUND WINNER: YOU | Score YOU 1 - 0", GameResultState.roundScoreDisplayLabel(round));

        score.recordRoundWin(PlayerSide.ONE);
        MatchScore.RoundResult match = score.recordRoundWin(PlayerSide.ONE);
        assertEquals("MATCH COMPLETE: YOU | Score YOU 3 - 0", GameResultState.roundScoreDisplayLabel(match));
    }

    @Test
    void resultPromptNamesReplayAndReturnActions() {
        MatchScore score = new MatchScore(1);
        MatchScore.RoundResult match = score.recordRoundWin(PlayerSide.ONE);
        GameResultState state = new GameResultState(new App(), "You win.", null, match);

        assertEquals("Press X to replay, Z for demo.", state.resetPromptLabel());
    }

    @Test
    void networkRematchTimeoutHasExplicitBound() {
        assertEquals(false, GameResultState.hasNetworkRematchTimedOut(299));
        assertEquals(true, GameResultState.hasNetworkRematchTimedOut(300));
    }
}
