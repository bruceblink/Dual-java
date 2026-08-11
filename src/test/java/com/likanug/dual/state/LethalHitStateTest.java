package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.LethalHitSnapshot;
import com.likanug.dual.game.MatchScore;
import com.likanug.dual.game.PlayerSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class LethalHitStateTest {

    @Test
    void normalAndTacticalKillsUseDistinctCauseLabels() {
        assertEquals("YOU: LETHAL HIT", LethalHitState.lethalFeedbackLabel(PlayerSide.ONE, false));
        assertEquals("RIVAL: LETHAL HIT", LethalHitState.lethalFeedbackLabel(PlayerSide.TWO, false));
        assertEquals("YOU: FINISH", LethalHitState.lethalFeedbackLabel(PlayerSide.ONE, true));
    }

    @Test
    void freezeProgressIsClampedAtBothEnds() {
        assertEquals(0.0F, LethalHitState.freezeProgress(-1, 12));
        assertEquals(0.5F, LethalHitState.freezeProgress(6, 12));
        assertEquals(1.0F, LethalHitState.freezeProgress(12, 12));
        assertEquals(1.0F, LethalHitState.freezeProgress(1, 0));
    }

    @Test
    void lethalFreezeHandsTheSameSnapshotToTheResultBridge() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        MatchScore.RoundResult result = system.recordRoundWin(PlayerSide.ONE);
        LethalHitSnapshot snapshot = new LethalHitSnapshot(
                PlayerSide.ONE, 640.0F, 620.0F, 640.0F, 120.0F, 640.0F, 100.0F, 0);
        LethalHitState state = new LethalHitState(app, "You win.", null, result, snapshot);
        system.setCurrentState(state);

        for (int frame = 0; frame < GameConstants.LETHAL_HIT_FREEZE_FRAMES; frame++) {
            state.finishFrame(system);
        }
        assertSame(state, system.getCurrentState());

        state.finishFrame(system);

        GameResultState resultState = assertInstanceOf(GameResultState.class, system.getCurrentState());
        assertEquals(snapshot, resultState.getLethalHitSnapshot());
    }
}
