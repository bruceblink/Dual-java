package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.LethalHitSnapshot;
import com.likanug.dual.game.MatchScore;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;

/** Freezes one confirmed lethal collision long enough to connect its cause to the result overlay. */
public final class LethalHitState extends GameSystemState {

    private final String resultMessage;
    private final TacticalEvent finishFeedback;
    private final MatchScore.RoundResult roundResult;
    private final LethalHitSnapshot snapshot;

    public LethalHitState(
            App app,
            String resultMessage,
            TacticalEvent finishFeedback,
            MatchScore.RoundResult roundResult,
            LethalHitSnapshot snapshot) {
        super(app);
        this.resultMessage = resultMessage;
        this.finishFeedback = finishFeedback;
        this.roundResult = roundResult;
        this.snapshot = snapshot;
    }

    /** Draws a frozen combat world while impact particles remain free to finish their short animation. */
    @Override
    public void runSystem(GameSystem system) {
        final float progress = freezeProgress(properFrameCount, GameConstants.LETHAL_HIT_FREEZE_FRAMES);
        displaySnapshot(app, snapshot, 1.0F, progress);
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
        system.getMyGroup().displayArrows();
        system.getOtherGroup().displayArrows();
        system.getCommonParticleSet().update();
        system.getCommonParticleSet().display();
    }

    /** Leaves the freeze free of result text; the following overlay reveals the updated score. */
    @Override
    public void displayMessage(GameSystem system) {
    }

    @Override
    public void checkStateTransition(GameSystem system) {
        if (properFrameCount < GameConstants.LETHAL_HIT_FREEZE_FRAMES) return;
        system.setCurrentState(new GameResultState(
                app, resultMessage, finishFeedback, roundResult, snapshot));
    }

    /** Converts elapsed freeze frames to the ring expansion fraction used by rendering and tests. */
    static float freezeProgress(int elapsedFrames, int totalFrames) {
        if (totalFrames <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, (float) elapsedFrames / totalFrames));
    }

    static String lethalFeedbackLabel(PlayerSide attacker, boolean tacticalFinish) {
        if (tacticalFinish) return PlayGameState.tacticalFeedbackLabel(attacker, com.likanug.dual.game.TacticalEventType.FINISH);
        return (attacker == PlayerSide.ONE ? "YOU" : "RIVAL") + ": LETHAL HIT";
    }

    /** Renders the saved path, target silhouette, and broken impact corners at a deterministic animation phase. */
    static void displaySnapshot(
            App app,
            LethalHitSnapshot snapshot,
            float alphaRatio,
            float progressRatio) {
        final float alpha = Math.max(0.0F, Math.min(1.0F, alphaRatio));
        final float progress = Math.max(0.0F, Math.min(1.0F, progressRatio));
        final float ringSize = GameConstants.LETHAL_IMPACT_RING_START_SIZE
                + (GameConstants.LETHAL_IMPACT_RING_END_SIZE - GameConstants.LETHAL_IMPACT_RING_START_SIZE)
                * progress;

        app.pushStyle();
        app.noFill();
        app.stroke(192, 64, 64, Math.round(48.0F * alpha));
        app.strokeWeight(12.0F);
        app.line(snapshot.launchX(), snapshot.launchY(), snapshot.impactX(), snapshot.impactY());
        app.stroke(192, 64, 64, Math.round(232.0F * alpha));
        app.strokeWeight(4.0F);
        app.line(snapshot.launchX(), snapshot.launchY(), snapshot.impactX(), snapshot.impactY());

        app.fill(snapshot.targetColor(), Math.round(208.0F * alpha));
        app.stroke(snapshot.targetColor() == 0 ? 232 : 32, Math.round(224.0F * alpha));
        app.strokeWeight(2.0F);
        app.rect(snapshot.targetX(), snapshot.targetY(), GameConstants.PLAYER_BODY_SIZE, GameConstants.PLAYER_BODY_SIZE);

        app.noFill();
        app.stroke(192, 64, 64, Math.round(224.0F * alpha));
        app.strokeWeight(3.0F);
        drawBrokenImpact(app, snapshot.impactX(), snapshot.impactY(), ringSize);
        app.popStyle();
    }

    /** Draws only short corner segments so the impact marker reads as a fractured geometric ring. */
    private static void drawBrokenImpact(App app, float x, float y, float size) {
        final float half = size * 0.5F;
        final float segment = size * 0.18F;
        app.line(x - half, y - half, x - half + segment, y - half);
        app.line(x - half, y - half, x - half, y - half + segment);
        app.line(x + half, y - half, x + half - segment, y - half);
        app.line(x + half, y - half, x + half, y - half + segment);
        app.line(x - half, y + half, x - half + segment, y + half);
        app.line(x - half, y + half, x - half, y + half - segment);
        app.line(x + half, y + half, x + half - segment, y + half);
        app.line(x + half, y + half, x + half, y + half - segment);
    }

    LethalHitSnapshot getSnapshot() {
        return snapshot;
    }
}
