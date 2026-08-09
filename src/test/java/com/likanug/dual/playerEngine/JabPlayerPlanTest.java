package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JabPlayerPlanTest {

    @Test
    void aiReleasesShortbowInputWhenReserveIsEmptyAndRearmsAfterRecovery() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        PlayerActor player = new PlayerActor(engine, 255, app);
        JabPlayerPlan plan = new JabPlayerPlan(app);

        player.getShortbowAmmo().consume();
        player.getShortbowAmmo().consume();
        player.getShortbowAmmo().consume();
        plan.execute(player, engine.getControllingInputDevice());
        assertFalse(engine.getControllingInputDevice().isShotButtonPressed());

        while (!player.getShortbowAmmo().canFire()) {
            player.getShortbowAmmo().tickRecovery();
        }
        plan.execute(player, engine.getControllingInputDevice());
        assertTrue(engine.getControllingInputDevice().isShotButtonPressed());
        assertTrue(engine.getControllingInputDevice().isShotButtonJustPressed());
    }
}
