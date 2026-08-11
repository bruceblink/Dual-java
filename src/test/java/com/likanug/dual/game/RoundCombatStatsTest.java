package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoundCombatStatsTest {

    @Test
    void recordsBothPlayersIndependentlyAndReturnsImmutableSnapshots() {
        RoundCombatStats stats = new RoundCombatStats();
        stats.advanceFrame();
        stats.advanceFrame();
        stats.recordShortbowShot(PlayerSide.ONE);
        stats.recordShortbowHit(PlayerSide.ONE);
        stats.recordLongbowShot(PlayerSide.ONE);
        stats.recordLongbowHit(PlayerSide.ONE);
        stats.recordChargeBreak(PlayerSide.ONE);
        stats.recordInterception();

        RoundCombatStats.Snapshot snapshot = stats.snapshot();

        assertEquals(2, snapshot.activeFrameCount());
        assertEquals(1, snapshot.interceptionCount());
        assertEquals(new RoundCombatStats.PlayerSnapshot(1, 1, 1, 1, 1), snapshot.playerOne());
        assertEquals(new RoundCombatStats.PlayerSnapshot(0, 0, 0, 0, 0), snapshot.playerTwo());
        assertEquals(snapshot.playerOne(), snapshot.forSide(PlayerSide.ONE));
        assertThrows(IllegalArgumentException.class, () -> snapshot.forSide(null));
    }

    @Test
    void resetClearsRoundCountersWithoutReplacingTheRecorder() {
        RoundCombatStats stats = new RoundCombatStats();
        stats.advanceFrame();
        stats.recordShortbowShot(PlayerSide.TWO);

        stats.reset();

        assertEquals(0, stats.snapshot().activeFrameCount());
        assertEquals(0, stats.snapshot().interceptionCount());
        assertEquals(new RoundCombatStats.PlayerSnapshot(0, 0, 0, 0, 0), stats.snapshot().playerTwo());
    }
}
