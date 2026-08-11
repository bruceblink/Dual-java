package com.likanug.dual.game;

import java.util.EnumMap;

/** Records deterministic round actions for the local result report without changing combat outcomes. */
public final class RoundCombatStats {

    private final EnumMap<PlayerSide, MutablePlayerStats> playerStats = new EnumMap<>(PlayerSide.class);
    private int activeFrameCount;
    private int interceptionCount;

    public RoundCombatStats() {
        for (PlayerSide side : PlayerSide.values()) playerStats.put(side, new MutablePlayerStats());
    }

    public void advanceFrame() {
        activeFrameCount++;
    }

    public void recordShortbowShot(PlayerSide side) {
        statsFor(side).shortbowShots++;
    }

    public void recordShortbowHit(PlayerSide side) {
        statsFor(side).shortbowHits++;
    }

    public void recordLongbowShot(PlayerSide side) {
        statsFor(side).longbowShots++;
    }

    public void recordLongbowHit(PlayerSide side) {
        statsFor(side).longbowHits++;
    }

    public void recordChargeBreak(PlayerSide side) {
        statsFor(side).chargeBreaks++;
    }

    public void recordInterception() {
        interceptionCount++;
    }

    /** Returns an immutable view so result UI cannot mutate the next round's counters. */
    public Snapshot snapshot() {
        return new Snapshot(
                activeFrameCount,
                interceptionCount,
                statsFor(PlayerSide.ONE).snapshot(),
                statsFor(PlayerSide.TWO).snapshot());
    }

    /** Clears every round-scoped count while leaving the recorder attached to the current game mode. */
    public void reset() {
        activeFrameCount = 0;
        interceptionCount = 0;
        playerStats.values().forEach(MutablePlayerStats::reset);
    }

    private MutablePlayerStats statsFor(PlayerSide side) {
        if (side == null) throw new IllegalArgumentException("Player side is required.");
        return playerStats.get(side);
    }

    public record Snapshot(
            int activeFrameCount,
            int interceptionCount,
            PlayerSnapshot playerOne,
            PlayerSnapshot playerTwo) {
        public Snapshot {
            if (activeFrameCount < 0 || interceptionCount < 0) {
                throw new IllegalArgumentException("Round counts must not be negative.");
            }
            if (playerOne == null || playerTwo == null) {
                throw new IllegalArgumentException("Both player snapshots are required.");
            }
        }

        public PlayerSnapshot forSide(PlayerSide side) {
            if (side == null) throw new IllegalArgumentException("Player side is required.");
            return side == PlayerSide.ONE ? playerOne : playerTwo;
        }
    }

    public record PlayerSnapshot(
            int shortbowShots,
            int shortbowHits,
            int longbowShots,
            int longbowHits,
            int chargeBreaks) {

        public PlayerSnapshot {
            if (shortbowShots < 0 || shortbowHits < 0 || longbowShots < 0 || longbowHits < 0
                    || chargeBreaks < 0) {
                throw new IllegalArgumentException("Combat counts must not be negative.");
            }
        }
    }

    private static final class MutablePlayerStats {
        private int shortbowShots;
        private int shortbowHits;
        private int longbowShots;
        private int longbowHits;
        private int chargeBreaks;

        private PlayerSnapshot snapshot() {
            return new PlayerSnapshot(
                    shortbowShots, shortbowHits, longbowShots, longbowHits, chargeBreaks);
        }

        private void reset() {
            shortbowShots = 0;
            shortbowHits = 0;
            longbowShots = 0;
            longbowHits = 0;
            chargeBreaks = 0;
        }
    }
}
