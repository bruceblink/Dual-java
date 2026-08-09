package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

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

        if (input.isShotButtonJustPressed()) {
            parentActor.setState(drawShortbowState.entryState(parentActor));
            parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
            if (drawShortbowState.triggerPulled(parentActor)) {
                drawShortbowState.fire(parentActor);
            }
            return;
        }
        if (input.isLongShotButtonPressed()) {
            parentActor.setState(drawLongbowState.entryState(parentActor));
        }
    }

    public void displayEffect(PlayerActor parentActor) {
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

    public PlayerActorState entryState(PlayerActor parentActor) {
        return this;
    }

}
