package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.playerEngine.PlayerEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaLayoutTest {

    @Test
    void openArenaHasNoObstaclesAndCentralCoverIsMirrorCentered() {
        ArenaLayout open = ArenaLayout.open();
        ArenaLayout cover = ArenaLayout.centralCover();

        assertTrue(open.getObstacles().isEmpty());
        assertEquals(1, cover.getObstacles().size());
        ArenaRect obstacle = cover.getObstacles().getFirst();
        assertEquals(App.INTERNAL_CANVAS_WIDTH * 0.5F, obstacle.centerX());
        assertEquals(App.INTERNAL_CANVAS_HEIGHT * 0.5F, obstacle.centerY());
    }

    @Test
    void centralCoverBlocksOnlyTheMiddleAndLeavesBothSpawnsOpen() {
        ArenaLayout cover = ArenaLayout.centralCover();

        assertTrue(cover.blocksCircle(640.0F, 360.0F, 16.0F));
        assertFalse(cover.blocksCircle(640.0F, 100.0F, 16.0F));
        assertFalse(cover.blocksCircle(640.0F, 620.0F, 16.0F));
        assertFalse(cover.blocksCircle(200.0F, 360.0F, 16.0F));
    }

    @Test
    void coverImpactUsesTheFirstContactAndProjectsItOntoTheWall() {
        ArenaLayout.CoverImpact impact = ArenaLayout.centralCover()
                .findCoverImpact(480.0F, 360.0F, 504.0F, 360.0F, 8.0F)
                .orElseThrow();

        assertEquals(0.5F, impact.timeRatio(), 2.0E-4F);
        assertEquals(500.0F, impact.x(), 1.0E-3F);
        assertEquals(360.0F, impact.y(), 1.0E-3F);
        assertEquals(-1.0F, impact.normalX(), 1.0E-4F);
        assertEquals(0.0F, impact.normalY(), 1.0E-4F);
    }

    @Test
    void coverImpactFindsAPathThatCrossesWithBothEndpointsOutside() {
        ArenaLayout.CoverImpact lowerImpact = ArenaLayout.centralCover()
                .findCoverImpact(640.0F, 416.0F, 640.0F, 296.0F, 16.0F)
                .orElseThrow();
        ArenaLayout.CoverImpact upperImpact = ArenaLayout.centralCover()
                .findCoverImpact(640.0F, 304.0F, 640.0F, 424.0F, 16.0F)
                .orElseThrow();

        assertEquals(0.0F, lowerImpact.timeRatio(), 1.0E-6F);
        assertEquals(0.0F, upperImpact.timeRatio(), 1.0E-6F);
        assertEquals(640.0F, lowerImpact.x(), 1.0E-4F);
        assertEquals(640.0F, upperImpact.x(), 1.0E-4F);
        assertEquals(400.0F, lowerImpact.y(), 1.0E-4F);
        assertEquals(320.0F, upperImpact.y(), 1.0E-4F);
        assertEquals(-lowerImpact.normalY(), upperImpact.normalY(), 1.0E-4F);
    }

    @Test
    void roundedCoverCornerRejectsANearMissOutsideTheCircleRadius() {
        assertTrue(ArenaLayout.centralCover()
                .findCoverImpact(480.0F, 300.1F, 520.0F, 300.1F, 20.0F)
                .isPresent());
        assertTrue(ArenaLayout.centralCover()
                .findCoverImpact(480.0F, 299.9F, 520.0F, 299.9F, 20.0F)
                .isEmpty());
    }

    @Test
    void projectileStartingTangentAndMovingAwayIsNotAbsorbed() {
        assertTrue(ArenaLayout.centralCover()
                .findCoverImpact(484.0F, 360.0F, 460.0F, 360.0F, 16.0F)
                .isEmpty());
        assertTrue(ArenaLayout.centralCover()
                .findCoverImpact(484.0F, 360.0F, 520.0F, 360.0F, 16.0F)
                .isPresent());
    }

    @Test
    void projectilePathVisibilityMatchesTheCoverCollisionRule() {
        ArenaLayout cover = ArenaLayout.centralCover();

        assertFalse(cover.hasClearProjectilePath(640.0F, 620.0F, 640.0F, 100.0F, 16.0F));
        assertTrue(cover.hasClearProjectilePath(300.0F, 620.0F, 300.0F, 100.0F, 16.0F));
        assertTrue(cover.hasClearProjectilePath(640.0F, 416.0F, 640.0F, 460.0F, 16.0F));
    }

    @Test
    void obstacleRejectsNonPositiveDimensions() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ArenaRect(0.0F, 0.0F, 0.0F, 20.0F));
    }

    @Test
    void resolvingAPlayerAtTheCenterPushesItToASafeSide() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        PlayerActor player = new PlayerActor(engine, 255, app);
        player.setxPosition(640.0F);
        player.setyPosition(360.0F);

        ArenaLayout.centralCover().resolvePlayer(player);

        assertFalse(ArenaLayout.centralCover().blocksCircle(
                player.getxPosition(), player.getyPosition(), player.getHalfBodySize()));
    }
}
