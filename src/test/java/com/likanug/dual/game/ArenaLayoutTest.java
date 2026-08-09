package com.likanug.dual.game;

import com.likanug.dual.App;
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
    void obstacleRejectsNonPositiveDimensions() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ArenaRect(0.0F, 0.0F, 0.0F, 20.0F));
    }
}
