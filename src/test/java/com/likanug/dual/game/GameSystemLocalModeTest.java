package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.playerEngine.HumanPlayerEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSystemLocalModeTest {

    @Test
    void localModeCreatesTwoHumanPlayersWithSeparateInputSnapshots() {
        App app = new App();
        KeyInput playerOneInput = new KeyInput();
        KeyInput playerTwoInput = new KeyInput();
        app.setCurrentKeyInput(playerOneInput);
        app.setSecondKeyInput(playerTwoInput);

        GameSystem system = new GameSystem(false, false, app, true);

        PlayerActor playerOne = (PlayerActor) system.getMyGroup().getPlayer();
        PlayerActor playerTwo = (PlayerActor) system.getOtherGroup().getPlayer();
        assertTrue(system.isLocalTwoPlayer());
        assertTrue(playerOne.getEngine() instanceof HumanPlayerEngine);
        assertTrue(playerTwo.getEngine() instanceof HumanPlayerEngine);
        assertSame(playerOneInput, ((HumanPlayerEngine) playerOne.getEngine()).getCurrentKeyInput());
        assertSame(playerTwoInput, ((HumanPlayerEngine) playerTwo.getEngine()).getCurrentKeyInput());
        assertFalse(system.isDemoPlay());
    }

    @Test
    void localRoundResetClearsBothInputSnapshots() {
        App app = new App();
        KeyInput playerOneInput = new KeyInput();
        KeyInput playerTwoInput = new KeyInput();
        app.setCurrentKeyInput(playerOneInput);
        app.setSecondKeyInput(playerTwoInput);
        GameSystem system = new GameSystem(false, false, app, true);

        playerOneInput.isZPressed = true;
        playerTwoInput.isXPressed = true;
        system.resetRound();

        assertFalse(playerOneInput.isZPressed);
        assertFalse(playerTwoInput.isXPressed);
    }
}
