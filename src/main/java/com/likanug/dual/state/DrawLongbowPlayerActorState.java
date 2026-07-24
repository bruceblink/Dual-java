package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.LongbowArrowHead;
import com.likanug.dual.actor.arrow.LongbowArrowShaft;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;
import com.likanug.dual.particle.Particle;

import static com.likanug.dual.App.FPS;
import static processing.core.PApplet.*;

public class DrawLongbowPlayerActorState extends DrawBowPlayerActorState {

    enum ReleaseOutcome {
        KEEP_CHARGING,
        CANCEL,
        FIRE
    }

    private final float unitAngleSpeed = GameConstants.LONGBOW_AIM_SPEED_RATIO * TWO_PI / FPS;
    private final int chargeRequiredFrameCount = (int) (GameConstants.LONGBOW_CHARGE_SEC * FPS);
    private final int effectColor = app.color(192, 64, 64);
    private final int lockColor = app.color(64, 176, 128);
    private final int ringSize = GameConstants.LONGBOW_RING_SIZE;
    private final float ringStrokeWeight = GameConstants.LONGBOW_RING_STROKE;

    public DrawLongbowPlayerActorState(App app) {
        super(app);
    }

    public PlayerActorState entryState(PlayerActor parentActor) {
        parentActor.setChargedFrameCount(0);
        aim(parentActor, parentActor.getEngine().getControllingInputDevice());
        return this;
    }

    /**
     * Locks onto a living enemy inside the assist radius; outside it, mouse aim or the keyboard fallback remains active.
     * Re-evaluating every frame lets players move during the charge without losing a valid nearby target.
     */
    public void aim(PlayerActor parentActor, AbstractInputDevice input) {
        final AbstractPlayerActor enemyPlayer = getEnemyPlayer(parentActor);
        if (isAutoAimTargetAvailable(parentActor, enemyPlayer, GameConstants.LONGBOW_AUTO_AIM_RANGE)) {
            parentActor.setAimAngle(getEnemyPlayerActorAngle(parentActor));
            return;
        }

        if (input.hasAimAngle()) {
            parentActor.setAimAngle(input.getAimAngle());
        } else {
            parentActor.setAimAngle(parentActor.getAimAngle() + input.getHorizontalMoveButton() * unitAngleSpeed);
        }
    }

    public void fire(PlayerActor parentActor) {
        final float arrowComponentInterval = GameConstants.LONGBOW_COMPONENT_INTERVAL;
        final int arrowShaftNumber = GameConstants.LONGBOW_SHAFT_COUNT;
        for (int i = 0; i < arrowShaftNumber; i++) {
            LongbowArrowShaft newArrow = new LongbowArrowShaft(app);
            newArrow.setxPosition(parentActor.getxPosition() + i * arrowComponentInterval * cos(parentActor.getAimAngle()));
            newArrow.setyPosition(parentActor.getyPosition() + i * arrowComponentInterval * sin(parentActor.getAimAngle()));
            newArrow.setRotationAngle(parentActor.getAimAngle());
            newArrow.setVelocity(parentActor.getAimAngle(), GameConstants.LONGBOW_SPEED);

            parentActor.getGroup().addArrow(newArrow);
        }

        LongbowArrowHead newArrow = new LongbowArrowHead(app);
        newArrow.setxPosition(parentActor.getxPosition() + arrowShaftNumber * arrowComponentInterval * cos(parentActor.getAimAngle()));
        newArrow.setyPosition(parentActor.getyPosition() + arrowShaftNumber * arrowComponentInterval * sin(parentActor.getAimAngle()));
        newArrow.setRotationAngle(parentActor.getAimAngle());
        newArrow.setVelocity(parentActor.getAimAngle(), GameConstants.LONGBOW_SPEED);

        final Particle newParticle = app.getSystem().getCommonParticleSet().getBuilder()
                .type(2)  // Line
                .position(parentActor.getxPosition(), parentActor.getyPosition())
                .polarVelocity(0, 0)
                .rotation(parentActor.getAimAngle())
                .particleColor(app.color(192, 64, 64))
                .lifespanSecond(2)
                .weight(16)
                .build();
        app.getSystem().getCommonParticleSet().getParticleList().add(newParticle);
        parentActor.getGroup().addArrow(newArrow);
        app.getSystem().setScreenShakeValue(app.getSystem().getScreenShakeValue() + 10);
        parentActor.setChargedFrameCount(0);
        parentActor.setState(moveState.entryState(parentActor));
    }

    public void displayEffect(PlayerActor parentActor) {
        final AbstractPlayerActor enemyPlayer = getEnemyPlayer(parentActor);
        final boolean targetLocked = isAutoAimTargetAvailable(
                parentActor, enemyPlayer, GameConstants.LONGBOW_AUTO_AIM_RANGE);

        app.noFill();
        app.stroke(0);
        app.arc(0, 0, 100, 100, parentActor.getAimAngle() - QUARTER_PI, parentActor.getAimAngle() + QUARTER_PI);

        if (hasCompletedLongBowCharge(parentActor))
            app.stroke(effectColor);
        else
            app.stroke(0, 128);

        app.line(0, 0, 800 * cos(parentActor.getAimAngle()), 800 * sin(parentActor.getAimAngle()));

        if (targetLocked) {
            app.stroke(lockColor);
            app.strokeWeight(2);
            app.ellipse(
                    enemyPlayer.getxPosition() - parentActor.getxPosition(),
                    enemyPlayer.getyPosition() - parentActor.getyPosition(),
                    48,
                    48);
            app.strokeWeight(1);
        }

        app.rotate(-HALF_PI);
        app.strokeWeight(ringStrokeWeight);
        final float chargeProgress = calculateChargeProgress(
                parentActor.getChargedFrameCount(), chargeRequiredFrameCount);
        app.arc(0, 0, ringSize, ringSize, 0, TWO_PI * chargeProgress);
        if (chargeProgress >= 1.0F) {
            app.stroke(effectColor, 128 + (int) (127 * sin(app.frameCount * 0.2F)));
            app.ellipse(0, 0, ringSize + 14, ringSize + 14);
        }
        app.strokeWeight(1);
        app.rotate(+HALF_PI);
        parentActor.setChargedFrameCount(parentActor.getChargedFrameCount() + 1);
    }

    public void act(PlayerActor parentActor) {
        super.act(parentActor);

        if (parentActor.getChargedFrameCount() != chargeRequiredFrameCount) return;

        final Particle newParticle = app.getSystem().getCommonParticleSet().getBuilder()
                .type(3)  // Ring
                .position(parentActor.getxPosition(), parentActor.getyPosition())
                .polarVelocity(0, 0)
                .particleSize(ringSize)
                .particleColor(effectColor)
                .weight(ringStrokeWeight)
                .lifespanSecond(0)
                .build();
        app.getSystem().getCommonParticleSet().getParticleList().add(newParticle);
    }

    public boolean isDrawingLongBow() {
        return true;
    }

    public boolean hasCompletedLongBowCharge(PlayerActor parentActor) {
        return parentActor.getChargedFrameCount() >= chargeRequiredFrameCount;
    }

    public boolean buttonPressed(AbstractInputDevice input) {
        return input.isLongShotButtonPressed();
    }

    public boolean triggerPulled(PlayerActor parentActor) {
        return releaseOutcome(
                buttonPressed(parentActor.getEngine().getControllingInputDevice()),
                parentActor.getChargedFrameCount(),
                chargeRequiredFrameCount
        ) == ReleaseOutcome.FIRE;
    }

    @Override
    protected void onButtonReleased(PlayerActor parentActor) {
        if (releaseOutcome(false, parentActor.getChargedFrameCount(), chargeRequiredFrameCount)
                != ReleaseOutcome.CANCEL) return;

        parentActor.setChargedFrameCount(0);
        final Particle cancelParticle = app.getSystem().getCommonParticleSet().getBuilder()
                .type(3)
                .position(parentActor.getxPosition(), parentActor.getyPosition())
                .polarVelocity(0, 0)
                .particleSize(ringSize)
                .particleColor(app.color(96))
                .weight(3)
                .lifespanSecond(0.2F)
                .build();
        app.getSystem().getCommonParticleSet().getParticleList().add(cancelParticle);
    }

    @Override
    protected float getMoveRatio() {
        return GameConstants.LONGBOW_CHARGE_MOVE_RATIO;
    }

    static float calculateChargeProgress(int chargedFrameCount, int requiredFrameCount) {
        if (requiredFrameCount <= 0) return 1.0F;
        return min(1.0F, Math.max(0.0F, (float) chargedFrameCount / requiredFrameCount));
    }

    /** Returns whether a living target is close enough for deterministic longbow aim assistance. */
    static boolean isAutoAimTargetAvailable(
            PlayerActor player, AbstractPlayerActor target, float maximumRange) {
        if (target == null || target.isNull() || maximumRange < 0.0F) return false;
        final float deltaX = target.getxPosition() - player.getxPosition();
        final float deltaY = target.getyPosition() - player.getyPosition();
        return deltaX * deltaX + deltaY * deltaY <= maximumRange * maximumRange;
    }

    private AbstractPlayerActor getEnemyPlayer(PlayerActor parentActor) {
        if (parentActor.getGroup() == null || parentActor.getGroup().getEnemyGroup() == null) return null;
        return parentActor.getGroup().getEnemyGroup().getPlayer();
    }

    /** 根据按钮与蓄力帧数决定本帧继续、取消还是发射，避免状态层靠副作用猜测松开结果。 */
    static ReleaseOutcome releaseOutcome(boolean buttonPressed, int chargedFrameCount, int requiredFrameCount) {
        if (buttonPressed) return ReleaseOutcome.KEEP_CHARGING;
        return chargedFrameCount >= requiredFrameCount ? ReleaseOutcome.FIRE : ReleaseOutcome.CANCEL;
    }

}
