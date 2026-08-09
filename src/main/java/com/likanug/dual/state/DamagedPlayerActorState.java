package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;

import static com.likanug.dual.App.FPS;
import static processing.core.PConstants.HALF_PI;
import static processing.core.PConstants.TWO_PI;

public class DamagedPlayerActorState extends PlayerActorState {

    private PlayerActorState moveState;
    private final int durationFrameCount = (int) (GameConstants.DAMAGED_DURATION_SEC * FPS);

    public DamagedPlayerActorState(App app) {
        super(app);
    }

    public PlayerActorState getMoveState() {
        return moveState;
    }

    public void setMoveState(PlayerActorState moveState) {
        this.moveState = moveState;
    }

    public int getDurationFrameCount() {
        return durationFrameCount;
    }

    public void act(PlayerActor parentActor) {
        int remainingFrames = Math.max(0, parentActor.getDamageRemainingFrameCount() - 1);
        parentActor.setDamageRemainingFrameCount(remainingFrames);
        if (remainingFrames == 0) {
            parentActor.getShortbowPressure().reset();
            parentActor.setDamageEndFeedbackFrameCount(GameConstants.DAMAGED_END_FEEDBACK_FRAMES);
            parentActor.setState(moveState.entryState(parentActor));
        }
    }

    /** Draws a neutral countdown arc whose visible sweep is the remaining damage duration. */
    public void displayEffect(PlayerActor parentActor) {
        final float remainingRatio = damageProgress(
                parentActor.getDamageRemainingFrameCount(), durationFrameCount);
        app.pushStyle();
        app.noFill();
        app.stroke(232, 192, 96, 255 * remainingRatio);
        app.strokeWeight(GameConstants.DAMAGED_RING_STROKE);
        app.arc(
                0,
                0,
                GameConstants.DAMAGED_RING_SIZE,
                GameConstants.DAMAGED_RING_SIZE,
                -HALF_PI,
                -HALF_PI + TWO_PI * remainingRatio
        );
        app.popStyle();
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        // 受击会打断正在进行的长弓蓄力，恢复移动后不能沿用旧进度。
        if (parentActor.getShortbowPressure().recordHit()) {
            parentActor.setChargedFrameCount(0);
            parentActor.setDamageRemainingFrameCount(durationFrameCount);
            parentActor.setDamageEndFeedbackFrameCount(0);
        }
        return this;
    }

    /** Converts remaining frames to a clamped visual ratio so timing never produces invalid arc angles. */
    static float damageProgress(int remainingFrames, int durationFrames) {
        if (durationFrames <= 0) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, (float) remainingFrames / durationFrames));
    }

    public boolean isDamaged() {
        return true;
    }

}
