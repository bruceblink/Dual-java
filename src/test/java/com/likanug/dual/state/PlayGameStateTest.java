package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static processing.core.PConstants.HALF_PI;

class PlayGameStateTest {

    @Test
    void shortbowAmmoHudKeepsTheAvailableAndMaximumValuesReadable() {
        assertEquals("Shortbow 3 / 3", PlayGameState.shortbowAmmoDisplayLabel(3, 3));
        assertEquals("Shortbow 0 / 3", PlayGameState.shortbowAmmoDisplayLabel(0, 3));
    }

    @Test
    void calculateThrustAngleUsesCenteredRandomOffset() {
        float base = 1.25f;

        assertEquals(base - HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 0.0f), 1e-6);
        assertEquals(base, PlayGameState.calculateThrustAngle(base, 0.5f), 1e-6);
        assertEquals(base + HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 1.0f), 1e-6);
    }

    @Test
    void shortbowCollisionRecordsPressureForItsOwningPlayer() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);

        state.checkCollision(system);

        assertEquals(
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.PRESSURE, 0),
                system.getTacticalEventLog().getFirst());
    }

    @Test
    void interceptedArrowCannotAlsoRecordPressureAgainstAPlayer() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);
        system.getMyGroup().getRemovingArrowList().add(arrow);

        state.checkCollision(system);

        assertEquals(0, system.getTacticalEventLog().size());
    }

    @Test
    void tacticalFeedbackLabelsIdentifyBothThePlayerAndEventType() {
        assertEquals("P1 OPENING", PlayGameState.tacticalFeedbackLabel(PlayerSide.ONE, TacticalEventType.OPENING));
        assertEquals("P2 FINISH", PlayGameState.tacticalFeedbackLabel(PlayerSide.TWO, TacticalEventType.FINISH));
    }
}
