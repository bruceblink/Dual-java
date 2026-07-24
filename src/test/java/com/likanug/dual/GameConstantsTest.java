package com.likanug.dual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameConstantsTest {

    @Test
    void arenaGridRemainsSubtleAgainstTheDarkCanvas() {
        assertTrue(GameConstants.ARENA_GRID_COLOR > GameConstants.ARENA_BACKGROUND_COLOR);
        assertTrue(GameConstants.ARENA_BACKGROUND_COLOR >= 80);
        assertTrue(GameConstants.ARENA_GRID_COLOR - GameConstants.ARENA_BACKGROUND_COLOR <= 24);
        assertTrue(GameConstants.ARENA_GRID_LINES_PER_AXIS <= 6);
        assertTrue(GameConstants.ARENA_GRID_MAX_ACCELERATION < 0.1F);
    }
}
