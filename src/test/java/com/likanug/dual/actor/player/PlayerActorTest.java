package com.likanug.dual.actor.player;

import com.likanug.dual.App;
import com.likanug.dual.playerEngine.PlayerEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
