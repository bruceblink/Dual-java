package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSystemArenaCollisionTest {

    @Test
    void centralCoverRemovesAnArrowThatEntersItsGeometry() {
        App app = new App();
        app.setCurrentKeyInput(new KeyInput());
        GameSystem system = new GameSystem(true, false, app, false, com.likanug.dual.playerEngine.AiDifficulty.STANDARD,
                ArenaLayout.centralCover());
        app.setSystem(system);
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(640.0F);
        arrow.setyPosition(360.0F);
        system.getMyGroup().addArrow(arrow);

        system.resolveArenaCollisions();

        assertTrue(system.getMyGroup().getRemovingArrowList().contains(arrow));
    }
}
