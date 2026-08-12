package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.LongbowArrowHead;
import com.likanug.dual.actor.arrow.LongbowArrowShaft;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        arrow.setxPosition(480.0F);
        arrow.setyPosition(360.0F);
        arrow.setVelocity(0.0F, 24.0F);
        system.getMyGroup().addArrow(arrow);
        arrow.update();

        system.resolveArenaCollisions();

        assertTrue(system.getMyGroup().getRemovingArrowList().contains(arrow));
        assertEquals(GameConstants.COVER_IMPACT_PARTICLE_COUNT + 1,
                system.getCommonParticleSet().getParticleList().size());
        system.getCommonParticleSet().getParticleList().forEach(particle -> {
            assertEquals(500.0F, particle.getxPosition(), 1.0E-3F);
            assertEquals(360.0F, particle.getyPosition(), 1.0E-3F);
        });
        system.getCommonParticleSet().getParticleList().stream().skip(1).forEach(particle ->
                assertTrue(particle.getxVelocity() < 0.0F));
        assertTrue(system.getTacticalEventLog().isEmpty());
        assertEquals(0, system.getCombatPauseFrameCount());
        assertEquals(0, system.getRoundCombatStats().interceptionCount());
    }

    @Test
    void longbowHeadCreatesOneCoverBurstWhileItsShaftStaysSilent() {
        App app = new App();
        app.setCurrentKeyInput(new KeyInput());
        GameSystem system = new GameSystem(true, false, app, false,
                com.likanug.dual.playerEngine.AiDifficulty.STANDARD, ArenaLayout.centralCover());
        app.setSystem(system);
        LongbowArrowShaft shaft = new LongbowArrowShaft(app);
        LongbowArrowHead head = new LongbowArrowHead(app);
        for (com.likanug.dual.actor.arrow.AbstractArrowActor arrow : java.util.List.of(shaft, head)) {
            arrow.setxPosition(640.0F);
            arrow.setyPosition(296.0F);
            arrow.setLaunchPosition(640.0F, 416.0F);
            arrow.setVelocity(-processing.core.PConstants.HALF_PI, GameConstants.LONGBOW_SPEED);
            system.getMyGroup().addArrow(arrow);
        }

        assertFalse(ArenaLayout.centralCover().blocksCircle(
                head.getxPosition(), head.getyPosition(), head.getCollisionRadius()));

        system.resolveArenaCollisions();

        assertTrue(system.getMyGroup().getRemovingArrowList().contains(shaft));
        assertTrue(system.getMyGroup().getRemovingArrowList().contains(head));
        assertEquals(GameConstants.COVER_IMPACT_PARTICLE_COUNT + 1,
                system.getCommonParticleSet().getParticleList().size());
    }
}
