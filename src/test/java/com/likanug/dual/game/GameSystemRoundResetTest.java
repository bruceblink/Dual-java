package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.particle.Particle;
import com.likanug.dual.state.MovePlayerActorState;
import com.likanug.dual.state.StartGameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSystemRoundResetTest {

    @Test
    void resetsRoundStateButPreservesMatchScoreAndInputEngines() {
        App app = new App();
        app.setCurrentKeyInput(new com.likanug.dual.inputDevice.KeyInput());
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);

        system.recordRoundWin(PlayerSide.ONE);
        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        player.setxPosition(240);
        player.setyPosition(180);
        player.setxVelocity(6);
        player.setyVelocity(-4);
        system.getMyGroup().addArrow(new ShortbowArrow(app));
        system.getCommonParticleSet().getParticleList().add(new Particle(app));
        system.recordPressure(system.getMyGroup());

        system.resetRound();

        PlayerActor resetPlayer = (PlayerActor) system.getMyGroup().getPlayer();
        assertEquals(1, system.getMatchScore().getPlayerOneWins());
        assertEquals(0, system.getMatchScore().getPlayerTwoWins());
        assertEquals(App.INTERNAL_CANVAS_WIDTH * 0.5F, resetPlayer.getxPosition());
        assertEquals(App.INTERNAL_CANVAS_HEIGHT - 100, resetPlayer.getyPosition());
        assertEquals(0, resetPlayer.getxVelocity());
        assertEquals(0, resetPlayer.getyVelocity());
        assertInstanceOf(MovePlayerActorState.class, resetPlayer.getState());
        assertTrue(system.getMyGroup().getArrowList().isEmpty());
        assertTrue(system.getCommonParticleSet().getParticleList().isEmpty());
        assertTrue(system.getTacticalEventLog().isEmpty());
        assertInstanceOf(StartGameState.class, system.getCurrentState());
    }

    @Test
    void canReplayTenCompleteMatchesWithoutReplacingTheGameSystem() {
        App app = new App();
        app.setCurrentKeyInput(new com.likanug.dual.inputDevice.KeyInput());
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);
        Object playerEngine = ((PlayerActor) system.getMyGroup().getPlayer()).getEngine();

        for (int matchNumber = 0; matchNumber < 10; matchNumber++) {
            for (int roundNumber = 0; roundNumber < GameConstants.MATCH_ROUNDS_TO_WIN; roundNumber++) {
                system.recordRoundWin(PlayerSide.ONE);
                if (roundNumber + 1 < GameConstants.MATCH_ROUNDS_TO_WIN) {
                    system.resetRound();
                }
            }

            assertTrue(system.getMatchScore().isMatchComplete());
            if (matchNumber + 1 < 10) system.resetMatch();
        }

        assertEquals(GameConstants.MATCH_ROUNDS_TO_WIN, system.getMatchScore().getPlayerOneWins());
        assertSame(playerEngine, ((PlayerActor) system.getMyGroup().getPlayer()).getEngine());
    }
}
