package com.likanug.dual.game;

import java.util.Optional;

/**
 * Tracks a first-to-N match without depending on Processing or the game loop.
 * A round result reports its round winner and the optional match winner so
 * callers do not need to infer the meaning from loosely named booleans.
 */
public final class MatchScore {

    private final int roundsToWin;
    private int playerOneWins;
    private int playerTwoWins;

    public MatchScore(int roundsToWin) {
        if (roundsToWin <= 0) {
            throw new IllegalArgumentException("Rounds to win must be positive.");
        }
        this.roundsToWin = roundsToWin;
    }

    /**
     * Adds exactly one completed round and returns its immutable outcome.
     * Once either player reaches the target, later round results are rejected
     * so a delayed collision cannot change the completed match.
     */
    public RoundResult recordRoundWin(PlayerSide roundWinner) {
        if (roundWinner == null) {
            throw new IllegalArgumentException("Round winner is required.");
        }
        if (isMatchComplete()) {
            throw new IllegalStateException("A completed match cannot score another round.");
        }

        if (roundWinner == PlayerSide.ONE) {
            playerOneWins++;
        } else {
            playerTwoWins++;
        }

        Optional<PlayerSide> matchWinner = Optional.empty();
        if (getWins(roundWinner) >= roundsToWin) {
            matchWinner = Optional.of(roundWinner);
        }
        return new RoundResult(roundWinner, matchWinner, playerOneWins, playerTwoWins);
    }

    public int getRoundsToWin() {
        return roundsToWin;
    }

    public int getPlayerOneWins() {
        return playerOneWins;
    }

    public int getPlayerTwoWins() {
        return playerTwoWins;
    }

    public boolean isMatchComplete() {
        return playerOneWins >= roundsToWin || playerTwoWins >= roundsToWin;
    }

    public Optional<PlayerSide> getMatchWinner() {
        if (playerOneWins >= roundsToWin) return Optional.of(PlayerSide.ONE);
        if (playerTwoWins >= roundsToWin) return Optional.of(PlayerSide.TWO);
        return Optional.empty();
    }

    private int getWins(PlayerSide side) {
        return side == PlayerSide.ONE ? playerOneWins : playerTwoWins;
    }

    /** Immutable snapshot distinguishing the just-finished round from the match result. */
    public record RoundResult(
            PlayerSide roundWinner,
            Optional<PlayerSide> matchWinner,
            int playerOneWins,
            int playerTwoWins
    ) {
        public RoundResult {
            if (roundWinner == null || matchWinner == null) {
                throw new IllegalArgumentException("Round result fields are required.");
            }
        }
    }
}
