package com.likanug.dual.actor.player;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.playerEngine.PlayerEngine;
import com.likanug.dual.state.MovePlayerActorState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerActorTest {

    @Test
    void shortbowCooldownRecoversWhileThePlayerIsNotHoldingTheWeapon() {
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        PlayerActor player = new PlayerActor(engine, 255, new App());
        player.setShortbowCooldownFrameCount(2);

        player.update();
        assertEquals(1, player.getShortbowCooldownFrameCount());
        player.update();
        assertEquals(0, player.getShortbowCooldownFrameCount());
    }

    @Test
    void diagonalMovementUsesTheSameAccelerationMagnitudeAsCardinalMovement() {
        assertEquals(1.0F, PlayerActor.inputAccelerationScale(1.0F, 0.0F));
        assertEquals(1.0F, PlayerActor.inputAccelerationScale(0.0F, -1.0F));
        assertEquals((float) (1.0 / Math.sqrt(2.0)), PlayerActor.inputAccelerationScale(1.0F, 1.0F));
        assertEquals((float) (1.0 / Math.sqrt(2.0)), PlayerActor.inputAccelerationScale(-0.5F, 0.5F));
    }

    @Test
    void longbowRecoveryCountsDownAndRoundResetClearsIt() {
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        App app = new App();
        PlayerActor player = new PlayerActor(engine, 255, app);
        player.setLongbowRecoveryFrameCount(2);

        player.update();
        assertEquals(1, player.getLongbowRecoveryFrameCount());
        player.update();
        assertEquals(0, player.getLongbowRecoveryFrameCount());

        player.setLongbowRecoveryFrameCount(10);
        player.resetForRound(100.0F, 200.0F, new MovePlayerActorState(app));
        assertEquals(0, player.getLongbowRecoveryFrameCount());
    }

    @Test
    void shortbowBufferExpiresOnceAndRoundResetClearsIt() {
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        App app = new App();
        PlayerActor player = new PlayerActor(engine, 255, app);

        player.bufferShortbowInput();
        assertTrue(player.hasBufferedShortbowInput());
        for (int frame = 0; frame < GameConstants.SHORTBOW_INPUT_BUFFER_FRAMES; frame++) {
            player.update();
        }
        assertFalse(player.hasBufferedShortbowInput());

        player.bufferShortbowInput();
        player.clearShortbowInputBuffer();
        assertFalse(player.hasBufferedShortbowInput());

        player.bufferShortbowInput();
        player.resetForRound(100.0F, 200.0F, new MovePlayerActorState(app));
        assertEquals(0, player.getShortbowInputBufferFrameCount());
    }
}
