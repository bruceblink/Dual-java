package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;
import com.likanug.dual.playerEngine.AiDifficulty;
import com.likanug.dual.playerEngine.ComputerPlayerEngine;

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
        if (parentActor.getEngine() instanceof ComputerPlayerEngine computer
                && shouldAimAtIncomingArrow(app.random(1.0F), computer.getDifficulty())) {
            final AbstractArrowActor arrow = nearestEnemyArrow(parentActor);
            if (arrow != null) {
                parentActor.setAimAngle(parentActor.getAngle(arrow));
                return;
            }
        }
        parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
    }

    /** Runs a fixed full-movement follow-through and releases it independently of button hold duration. */
    @Override
    public void act(PlayerActor parentActor) {
        final AbstractInputDevice input = parentActor.getEngine().getControllingInputDevice();
        aim(parentActor, input);
        parentActor.addVelocity(input.getHorizontalMoveButton(), input.getVerticalMoveButton());
        if (parentActor.getShortbowActionFrameCount() <= 0) {
            parentActor.setState(moveState.entryState(parentActor));
        }
    }

    private AbstractArrowActor nearestEnemyArrow(PlayerActor parentActor) {
        AbstractArrowActor nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (AbstractArrowActor arrow : parentActor.getGroup().getEnemyGroup().getArrowList()) {
            final float distance = parentActor.getDistancePow2(arrow);
            final float toPlayerX = parentActor.getxPosition() - arrow.getxPosition();
            final float toPlayerY = parentActor.getyPosition() - arrow.getyPosition();
            final float approachDot = arrow.getxVelocity() * toPlayerX + arrow.getyVelocity() * toPlayerY;
            if (approachDot > 0.0F && distance < nearestDistance) {
                nearest = arrow;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** Keeps intercept aiming opt-in and lets every other roll use the established enemy auto-aim. */
    static boolean shouldAimAtIncomingArrow(float randomValue, AiDifficulty difficulty) {
        return randomValue < difficulty.getInterceptAimProbability();
    }

    public void fire(PlayerActor parentActor) {
        if (!parentActor.getShortbowAmmo().consume()) return;

        ShortbowArrow newArrow = new ShortbowArrow(app);
        final float directionAngle = parentActor.getAimAngle();
        newArrow.setxPosition(parentActor.getxPosition() + 24 * cos(directionAngle));
        newArrow.setyPosition(parentActor.getyPosition() + 24 * sin(directionAngle));
        newArrow.setLaunchPosition(parentActor.getxPosition(), parentActor.getyPosition());
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

    /** Checks cadence and reserve before Move commits a buffered press to the weapon state. */
    boolean isReadyToFire(PlayerActor player) {
        return player.getShortbowCooldownFrameCount() <= 0 && player.getShortbowAmmo().canFire();
    }

    @Override
    protected float getMoveRatio() {
        return 1.0F;
    }

}
