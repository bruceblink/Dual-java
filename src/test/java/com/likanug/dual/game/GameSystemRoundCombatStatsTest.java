package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameSystemRoundCombatStatsTest {

    @Test
    void productionShortbowFireRecordsOneShotAndRoundResetClearsIt() {
        App app = new App();
        KeyInput input = new KeyInput();
        app.setCurrentKeyInput(input);
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);
        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        input.setMouseShotPressed(true);

        player.act();

        assertEquals(1, system.getRoundCombatStats().playerOne().shortbowShots());
        assertEquals(0, system.getRoundCombatStats().playerTwo().shortbowShots());

        system.resetRound();
        assertEquals(0, system.getRoundCombatStats().playerOne().shortbowShots());
    }

    @Test
    void combatClockAdvancesDeterministicallyByFrame() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);

        assertEquals(0, system.getRoundCombatStats().activeFrameCount());
        system.advanceCombatFrame();
        system.advanceCombatFrame();

        assertEquals(2, system.getRoundCombatStats().activeFrameCount());
    }
}
