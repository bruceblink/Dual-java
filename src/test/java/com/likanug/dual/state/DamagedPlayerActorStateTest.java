package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.playerEngine.HumanPlayerEngine;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamagedPlayerActorStateTest {

    @Test
    void enteringDamageStateClearsAnInterruptedLongbowCharge() {
        PlayerActor player = new PlayerActor(
                new HumanPlayerEngine(new KeyInput()),
                255,
                new App()
        );
        player.setChargedFrameCount(20);
        DamagedPlayerActorState state = new DamagedPlayerActorState(new App());

        state.entryState(player);

        assertEquals(0, player.getChargedFrameCount());
        assertEquals(state.getDurationFrameCount(), player.getDamageRemainingFrameCount());
        assertEquals(0, player.getDamageEndFeedbackFrameCount());
    }

    @Test
    void damageRingProgressClampsToTheRemainingWindow() {
        assertEquals(1.0F, DamagedPlayerActorState.damageProgress(90, 90));
        assertEquals(0.5F, DamagedPlayerActorState.damageProgress(45, 90));
        assertEquals(0.0F, DamagedPlayerActorState.damageProgress(-1, 90));
        assertEquals(1.0F, DamagedPlayerActorState.damageProgress(100, 90));
        assertEquals(0.0F, DamagedPlayerActorState.damageProgress(1, 0));
    }

    @Test
    void damageStateEmitsAShortEndFeedbackBeforeReturningToMovement() {
        PlayerActor player = new PlayerActor(
                new HumanPlayerEngine(new KeyInput()),
                255,
                new App()
        );
        DamagedPlayerActorState state = new DamagedPlayerActorState(new App());
        MovePlayerActorState moveState = new MovePlayerActorState(new App());
        state.setMoveState(moveState);
        state.entryState(player);
        player.setDamageRemainingFrameCount(1);

        state.act(player);

        assertEquals(0, player.getDamageRemainingFrameCount());
        assertEquals(GameConstants.DAMAGED_END_FEEDBACK_FRAMES, player.getDamageEndFeedbackFrameCount());
        assertEquals(moveState, player.getState());
    }
}
