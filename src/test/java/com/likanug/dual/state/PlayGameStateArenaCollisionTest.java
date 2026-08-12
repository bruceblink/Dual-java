package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.game.ArenaLayout;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayGameStateArenaCollisionTest {

    @Test
    void coverRemovedArrowsDoNotCreateAnInterceptEventInTheSameFrame() {
        App app = new App();
        app.setCurrentKeyInput(new KeyInput());
        GameSystem system = new GameSystem(true, false, app, false,
                com.likanug.dual.playerEngine.AiDifficulty.STANDARD, ArenaLayout.centralCover());
        app.setSystem(system);
        ShortbowArrow myArrow = new ShortbowArrow(app);
        ShortbowArrow enemyArrow = new ShortbowArrow(app);
        myArrow.setxPosition(520.0F);
        myArrow.setyPosition(360.0F);
        myArrow.setLaunchPosition(480.0F, 360.0F);
        enemyArrow.setxPosition(760.0F);
        enemyArrow.setyPosition(360.0F);
        enemyArrow.setLaunchPosition(800.0F, 360.0F);
        system.getMyGroup().addArrow(myArrow);
        system.getOtherGroup().addArrow(enemyArrow);
        system.resolveArrowCoverCollisions();

        new PlayGameState(app).checkCollision(system);

        assertTrue(system.getMyGroup().getRemovingArrowList().contains(myArrow));
        assertTrue(system.getOtherGroup().getRemovingArrowList().contains(enemyArrow));
        assertTrue(system.getTacticalEventLog().isEmpty());
        assertTrue(system.getCombatPauseFrameCount() < GameConstants.INTERCEPT_HIT_STOP_FRAMES);
    }
}
