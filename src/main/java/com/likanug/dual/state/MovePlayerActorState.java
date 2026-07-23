package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

public class MovePlayerActorState extends PlayerActorState {

    private PlayerActorState drawShortbowState;
    private PlayerActorState drawLongbowState;

    public MovePlayerActorState(App app) {
        super(app);
    }

    public PlayerActorState getDrawShortbowState() {
        return drawShortbowState;
    }

    public void setDrawShortbowState(PlayerActorState drawShortbowState) {
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

        if (input.isShotButtonPressed()) {
            parentActor.setState(drawShortbowState.entryState(parentActor));
            parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
            return;
        }
        if (input.isLongShotButtonPressed()) {
            parentActor.setState(drawLongbowState.entryState(parentActor));
            parentActor.setAimAngle(input.hasAimAngle() ? input.getAimAngle() : getEnemyPlayerActorAngle(parentActor));
        }
    }

    public void displayEffect(PlayerActor parentActor) {
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        return this;
    }

}
