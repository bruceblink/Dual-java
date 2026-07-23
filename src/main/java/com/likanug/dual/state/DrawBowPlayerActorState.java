package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

public abstract class DrawBowPlayerActorState extends PlayerActorState {

    protected PlayerActorState moveState;

    public DrawBowPlayerActorState(App app) {
        super(app);
    }

    public void act(PlayerActor parentActor) {
        final AbstractInputDevice input = parentActor.getEngine().getControllingInputDevice();
        aim(parentActor, input);

        final float moveRatio = getMoveRatio();
        parentActor.addVelocity(moveRatio * input.getHorizontalMoveButton(), moveRatio * input.getVerticalMoveButton());

        if (triggerPulled(parentActor)) fire(parentActor);

        if (!buttonPressed(input)) {
            parentActor.setState(moveState.entryState(parentActor));
        }
    }

    protected abstract void aim(PlayerActor parentActor, AbstractInputDevice input);

    protected abstract void fire(PlayerActor parentActor);

    protected abstract boolean buttonPressed(AbstractInputDevice input);

    protected abstract boolean triggerPulled(PlayerActor parentActor);

    /** 返回当前拉弓状态的移动加速度倍率；长弓可单独调节手感而不改变短弓规则。 */
    protected float getMoveRatio() {
        return 0.25F;
    }

    public PlayerActorState getMoveState() {
        return moveState;
    }

    public void setMoveState(PlayerActorState moveState) {
        this.moveState = moveState;
    }
}
