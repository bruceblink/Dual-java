package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class MatchFlowSmokeTest {

    @Test
    void completesTenMatchesThroughRoundResultsAndReplayInput() {
        App app = new App();
        KeyInput input = new KeyInput();
        app.setCurrentKeyInput(input);
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);
        Object playerEngine = ((PlayerActor) system.getMyGroup().getPlayer()).getEngine();
        int completedMatchCount = 0;

        for (int matchNumber = 0; matchNumber < 10; matchNumber++) {
            for (int roundNumber = 0; roundNumber < GameConstants.MATCH_ROUNDS_TO_WIN; roundNumber++) {
                PlayGameState playState = new PlayGameState(app);
                playState.killPlayer(system.getOtherGroup().getPlayer());
                playState.checkStateTransition(system);
                GameResultState resultState = assertInstanceOf(GameResultState.class, system.getCurrentState());

                // Advance the real result-state timer before sending the replay action.
                for (int frame = 0; frame <= App.FPS; frame++) {
                    resultState.finishFrame(system);
                }
                input.isXPressed = true;
                resultState.finishFrame(system);
                input.isXPressed = false;

                if (roundNumber + 1 < GameConstants.MATCH_ROUNDS_TO_WIN) {
                    assertEquals(roundNumber + 1, system.getMatchScore().getPlayerOneWins());
                    assertInstanceOf(StartGameState.class, system.getCurrentState());
                }
            }

            completedMatchCount++;
            assertEquals(0, system.getMatchScore().getPlayerOneWins());
            assertEquals(0, system.getMatchScore().getPlayerTwoWins());
            assertSame(playerEngine, ((PlayerActor) system.getMyGroup().getPlayer()).getEngine());
        }

        assertEquals(10, completedMatchCount);
        assertInstanceOf(StartGameState.class, system.getCurrentState());
    }
}
