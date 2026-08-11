package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

import static processing.core.PConstants.HALF_PI;
import static processing.core.PConstants.TWO_PI;

public class MovePlayerActorState extends PlayerActorState {

    private DrawShortbowPlayerActorState drawShortbowState;
    private PlayerActorState drawLongbowState;

    public MovePlayerActorState(App app) {
        super(app);
    }

    public DrawShortbowPlayerActorState getDrawShortbowState() {
        return drawShortbowState;
    }

    public void setDrawShortbowState(DrawShortbowPlayerActorState drawShortbowState) {
        this.drawShortbowState = drawShortbowState;
    }

    public PlayerActorState getDrawLongbowState() {
        return drawLongbowState;
    }

    public void setDrawLongbowState(PlayerActorState drawLongbowState) {
        this.drawLongbowState = drawLongbowState;
    }

    public void act(PlayerActor parentActor) {
        final AbstractInputDevice input = parentActor.getEngine().getControllingInputDevice();
        parentActor.addVelocity(input.getHorizontalMoveButton(), input.getVerticalMoveButton());

        // Longbow recovery preserves movement but blocks both weapons so a missed shot creates counterplay.
        if (parentActor.getLongbowRecoveryFrameCount() > 0) return;

        final boolean shortbowRequested = input.isShotButtonJustPressed()
                || parentActor.hasBufferedShortbowInput();
        if (shortbowRequested && drawShortbowState.isReadyToFire(parentActor)) {
            parentActor.clearShortbowInputBuffer();
            parentActor.setState(drawShortbowState.entryState(parentActor));
            parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
            drawShortbowState.fire(parentActor);
            return;
        }
        if (input.isLongShotButtonPressed()) {
            parentActor.clearShortbowInputBuffer();
            parentActor.setState(drawLongbowState.entryState(parentActor));
        }
    }

    public void displayEffect(PlayerActor parentActor) {
        displayLongbowRecovery(parentActor);

        final int remainingFrames = parentActor.getDamageEndFeedbackFrameCount();
        if (remainingFrames <= 0) return;

        // A short neutral flash makes the exact end of the vulnerable window visible after control returns.
        final int alpha = Math.round(
                255.0F * remainingFrames / GameConstants.DAMAGED_END_FEEDBACK_FRAMES);
        app.pushStyle();
        app.noFill();
        app.stroke(224, 224, 224, alpha);
        app.strokeWeight(2.0F);
        app.ellipse(0, 0, GameConstants.DAMAGED_RING_SIZE + 8.0F, GameConstants.DAMAGED_RING_SIZE + 8.0F);
        app.popStyle();
    }

    /** Draws a shrinking commitment arc while movement remains available but attacks are recovering. */
    private void displayLongbowRecovery(PlayerActor parentActor) {
        final int totalFrames = Math.round(GameConstants.LONGBOW_RECOVERY_SEC * App.FPS);
        final float progress = recoveryProgress(parentActor.getLongbowRecoveryFrameCount(), totalFrames);
        if (progress <= 0.0F) return;

        app.pushStyle();
        app.noFill();
        app.stroke(192, 64, 64, Math.round(96.0F + 159.0F * progress));
        app.strokeWeight(3.0F);
        app.arc(
                0,
                0,
                GameConstants.LONGBOW_RING_SIZE - 10.0F,
                GameConstants.LONGBOW_RING_SIZE - 10.0F,
                -HALF_PI,
                -HALF_PI + TWO_PI * progress);
        app.popStyle();
    }

    /** Converts remaining recovery frames to a clamped visual ratio for the shrinking arc. */
    static float recoveryProgress(int remainingFrames, int totalFrames) {
        if (totalFrames <= 0) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, (float) remainingFrames / totalFrames));
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        return this;
    }

}
