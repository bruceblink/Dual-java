package com.likanug.dual.state;

import com.likanug.dual.App;
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
    }
}
