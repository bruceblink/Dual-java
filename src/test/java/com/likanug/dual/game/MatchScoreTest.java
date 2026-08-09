package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScoreTest {

    @Test
    void startsWithZeroScoresAndConfiguredTarget() {
        MatchScore score = new MatchScore(3);

        assertEquals(3, score.getRoundsToWin());
        assertEquals(0, score.getPlayerOneWins());
        assertEquals(0, score.getPlayerTwoWins());
        assertFalse(score.isMatchComplete());
        assertTrue(score.getMatchWinner().isEmpty());
    }

    @Test
    void recordsWinsForBothPlayersAndReportsRoundWinnerSeparately() {
        MatchScore score = new MatchScore(3);

        MatchScore.RoundResult first = score.recordRoundWin(PlayerSide.ONE);
        MatchScore.RoundResult second = score.recordRoundWin(PlayerSide.TWO);

        assertEquals(PlayerSide.ONE, first.roundWinner());
        assertTrue(first.matchWinner().isEmpty());
        assertEquals(PlayerSide.TWO, second.roundWinner());
        assertTrue(second.matchWinner().isEmpty());
        assertEquals(1, score.getPlayerOneWins());
        assertEquals(1, score.getPlayerTwoWins());
    }

    @Test
    void firstPlayerToThreeWinsCompletesTheMatch() {
        MatchScore score = new MatchScore(3);

        score.recordRoundWin(PlayerSide.ONE);
        score.recordRoundWin(PlayerSide.TWO);
        score.recordRoundWin(PlayerSide.ONE);
        MatchScore.RoundResult result = score.recordRoundWin(PlayerSide.ONE);

        assertEquals(PlayerSide.ONE, result.roundWinner());
        assertEquals(PlayerSide.ONE, result.matchWinner().orElseThrow());
        assertEquals(3, score.getPlayerOneWins());
        assertEquals(1, score.getPlayerTwoWins());
        assertTrue(score.isMatchComplete());
        assertEquals(PlayerSide.ONE, score.getMatchWinner().orElseThrow());
    }

    @Test
    void rejectsInvalidTargetsNullWinnersAndRepeatedScoringAfterCompletion() {
        assertThrows(IllegalArgumentException.class, () -> new MatchScore(0));

        MatchScore score = new MatchScore(1);
        assertThrows(IllegalArgumentException.class, () -> score.recordRoundWin(null));
        score.recordRoundWin(PlayerSide.TWO);

        assertThrows(IllegalStateException.class, () -> score.recordRoundWin(PlayerSide.ONE));
    }

    @Test
    void resetMakesACompletedMatchReadyForReplay() {
        MatchScore score = new MatchScore(1);
        score.recordRoundWin(PlayerSide.TWO);

        score.reset();

        assertFalse(score.isMatchComplete());
        assertTrue(score.getMatchWinner().isEmpty());
        assertEquals(0, score.getPlayerOneWins());
        assertEquals(0, score.getPlayerTwoWins());
    }
}
