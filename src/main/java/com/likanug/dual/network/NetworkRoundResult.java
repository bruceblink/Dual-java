package com.likanug.dual.network;

/** Immutable wire-level snapshot of one completed round from the sender's perspective. */
public record NetworkRoundResult(
        int roundNumber,
        int winnerSide,
        int playerOneWins,
        int playerTwoWins,
        boolean matchComplete
) {

    public static final int SIDE_ONE = 0;
    public static final int SIDE_TWO = 1;

    public NetworkRoundResult {
        if (roundNumber <= 0 || roundNumber > 255) {
            throw new IllegalArgumentException("Round number must fit in one positive byte.");
        }
        if (winnerSide != SIDE_ONE && winnerSide != SIDE_TWO) {
            throw new IllegalArgumentException("Winner side must be SIDE_ONE or SIDE_TWO.");
        }
        if (playerOneWins < 0 || playerOneWins > 255 || playerTwoWins < 0 || playerTwoWins > 255) {
            throw new IllegalArgumentException("Round scores must fit in one byte.");
        }
    }

    /** Returns the same outcome from the opponent's local perspective. */
    public NetworkRoundResult mirrored() {
        return new NetworkRoundResult(
                roundNumber,
                winnerSide == SIDE_ONE ? SIDE_TWO : SIDE_ONE,
                playerTwoWins,
                playerOneWins,
                matchComplete);
    }
}
