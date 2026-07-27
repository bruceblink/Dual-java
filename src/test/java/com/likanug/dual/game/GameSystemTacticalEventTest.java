package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.state.DrawLongbowPlayerActorState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSystemTacticalEventTest {

    @Test
    void pressureThenLongbowEntryAndFinishExposeOnlyConfirmedSequenceFacts() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();

        system.advanceCombatFrame();
        system.recordPressure(system.getMyGroup());
        new DrawLongbowPlayerActorState(app).entryState(player);
        system.recordLongbowFinish(system.getMyGroup());

        assertEquals(List.of(
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.PRESSURE, 1),
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.OPENING, 1),
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.FINISH, 1)
        ), system.getTacticalEventLog());
    }

    @Test
    void resetClearsFactsAndPreventsAnOldPressureFromProducingAFinish() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);

        system.recordPressure(system.getMyGroup());
        system.resetTacticalEvents();
        system.recordLongbowFinish(system.getMyGroup());

        assertTrue(system.getTacticalEventLog().isEmpty());
    }

    @Test
    void drainingTacticalEventsDeliversFactsOnlyOnce() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        system.recordPressure(system.getMyGroup());

        assertEquals(1, system.drainTacticalEvents().size());
        assertTrue(system.drainTacticalEvents().isEmpty());
    }
}
