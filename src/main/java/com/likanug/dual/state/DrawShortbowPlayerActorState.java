package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

import static com.likanug.dual.App.FPS;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.QUARTER_PI;

public class DrawShortbowPlayerActorState extends DrawBowPlayerActorState {

    private final int fireIntervalFrameCount = (int) (FPS * GameConstants.SHORTBOW_FIRE_INTERVAL_SEC);

    public DrawShortbowPlayerActorState(App app) {
        super(app);
    }

    public void aim(PlayerActor parentActor, AbstractInputDevice input) {
        parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
    }

    public void fire(PlayerActor parentActor) {
        if (!parentActor.getShortbowAmmo().consume()) return;

        ShortbowArrow newArrow = new ShortbowArrow(app);
        final float directionAngle = parentActor.getAimAngle();
        newArrow.setxPosition(parentActor.getxPosition() + 24 * cos(directionAngle));
        newArrow.setyPosition(parentActor.getyPosition() + 24 * sin(directionAngle));
        newArrow.setRotationAngle(directionAngle);
        newArrow.setVelocity(directionAngle, 24);

        parentActor.getGroup().addArrow(newArrow);
        parentActor.setShortbowCooldownFrameCount(fireIntervalFrameCount);
    }

    public void displayEffect(PlayerActor parentActor) {
        app.line(0, 0, 70 * cos(parentActor.getAimAngle()), 70 * sin(parentActor.getAimAngle()));
        app.noFill();
        app.arc(0, 0, 100, 100, parentActor.getAimAngle() - QUARTER_PI, parentActor.getAimAngle() + QUARTER_PI);
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        return this;
    }

    public boolean buttonPressed(AbstractInputDevice input) {
        return input.isShotButtonPressed();
    }

    public boolean triggerPulled(PlayerActor parentActor) {
        return canFire(
                parentActor.getEngine().getControllingInputDevice().isShotButtonJustPressed(),
                parentActor.getShortbowCooldownFrameCount(),
                parentActor.getShortbowAmmo().canFire());
    }

    static boolean canFire(boolean shotButtonPressed, int cooldownFrameCount) {
        return canFire(shotButtonPressed, cooldownFrameCount, true);
    }

    /** Requires button intent, a ready cadence timer, and an arrow in the player's shared reserve. */
    static boolean canFire(boolean shotButtonPressed, int cooldownFrameCount, boolean hasAmmo) {
        return shotButtonPressed && cooldownFrameCount <= 0 && hasAmmo;
    }

}
