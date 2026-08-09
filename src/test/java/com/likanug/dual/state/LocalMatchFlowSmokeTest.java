package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.playerEngine.HumanPlayerEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMatchFlowSmokeTest {

    @Test
    void localPlayersCanFinishAStandardMatchAndReplayWithoutReplacingEitherEngine() {
        App app = new App();
        KeyInput playerOneInput = new KeyInput();
        KeyInput playerTwoInput = new KeyInput();
        app.setCurrentKeyInput(playerOneInput);
        app.setSecondKeyInput(playerTwoInput);
        GameSystem system = new GameSystem(false, false, app, true);
        app.setSystem(system);
        Object playerOneEngine = ((PlayerActor) system.getMyGroup().getPlayer()).getEngine();
        Object playerTwoEngine = ((PlayerActor) system.getOtherGroup().getPlayer()).getEngine();

        for (int roundNumber = 0; roundNumber < GameConstants.MATCH_ROUNDS_TO_WIN; roundNumber++) {
            finishRoundForPlayerOne(app, system);
            if (roundNumber + 1 < GameConstants.MATCH_ROUNDS_TO_WIN) {
                playerOneInput.isXPressed = true;
                ((GameResultState) system.getCurrentState()).finishFrame(system);
                playerOneInput.isXPressed = false;
                assertInstanceOf(StartGameState.class, system.getCurrentState());
            }
        }

        assertTrue(system.getMatchScore().isMatchComplete());
        playerOneInput.isXPressed = true;
        ((GameResultState) system.getCurrentState()).finishFrame(system);
        playerOneInput.isXPressed = false;

        assertEquals(0, system.getMatchScore().getPlayerOneWins());
        assertEquals(0, system.getMatchScore().getPlayerTwoWins());
        assertSame(playerOneEngine, ((PlayerActor) system.getMyGroup().getPlayer()).getEngine());
        assertSame(playerTwoEngine, ((PlayerActor) system.getOtherGroup().getPlayer()).getEngine());
        assertInstanceOf(StartGameState.class, system.getCurrentState());
        assertTrue(system.isLocalTwoPlayer());
        assertTrue(playerOneEngine instanceof HumanPlayerEngine);
        assertTrue(playerTwoEngine instanceof HumanPlayerEngine);
    }

    @Test
    void localMatchResultCanReturnToTheDemoWithoutLeakingEitherInput() {
        App app = new App();
        KeyInput playerOneInput = new KeyInput();
        KeyInput playerTwoInput = new KeyInput();
        app.setCurrentKeyInput(playerOneInput);
        app.setSecondKeyInput(playerTwoInput);
        GameSystem system = new GameSystem(false, false, app, true);
        app.setSystem(system);

        for (int roundNumber = 0; roundNumber < GameConstants.MATCH_ROUNDS_TO_WIN; roundNumber++) {
            finishRoundForPlayerOne(app, system);
            if (roundNumber + 1 < GameConstants.MATCH_ROUNDS_TO_WIN) {
                playerOneInput.isXPressed = true;
                ((GameResultState) system.getCurrentState()).finishFrame(system);
                playerOneInput.isXPressed = false;
            }
        }

        playerOneInput.isZPressed = true;
        playerTwoInput.isZPressed = true;
        ((GameResultState) system.getCurrentState()).finishFrame(system);

        assertTrue(app.getSystem().isDemoPlay());
        assertFalse(playerOneInput.isZPressed);
        assertFalse(playerTwoInput.isZPressed);
    }

    private static void finishRoundForPlayerOne(App app, GameSystem system) {
        PlayGameState playState = new PlayGameState(app);
        playState.killPlayer(system.getOtherGroup().getPlayer());
        playState.checkStateTransition(system);
        GameResultState resultState = assertInstanceOf(GameResultState.class, system.getCurrentState());
        for (int frame = 0; frame <= App.FPS; frame++) resultState.finishFrame(system);
    }
}
