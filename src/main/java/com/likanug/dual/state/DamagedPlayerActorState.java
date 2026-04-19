package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.player.PlayerActor;

import static com.likanug.dual.App.FPS;

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

        parentActor.setDamageRemainingFrameCount(parentActor.getDamageRemainingFrameCount() - 1);
        if (parentActor.getDamageRemainingFrameCount() <= 0)
            parentActor.setState(moveState.entryState(parentActor));
    }

    public void displayEffect(PlayerActor parentActor) {
        app.noFill();
        app.stroke(192, 64, 64, (float) (255 * parentActor.getDamageRemainingFrameCount()) / durationFrameCount);
        app.ellipse(0, 0, 64, 64);
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        parentActor.setDamageRemainingFrameCount(durationFrameCount);
        return this;
    }

    public boolean isDamaged() {
        return true;
    }

}
