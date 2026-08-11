package com.likanug.dual.actor.player;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.playerEngine.PlayerEngine;
import com.likanug.dual.game.ShortbowPressure;
import com.likanug.dual.state.PlayerActorState;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PApplet.constrain;
import static processing.core.PApplet.sq;
import static processing.core.PConstants.TWO_PI;

public class PlayerActor extends AbstractPlayerActor {

    private final float bodySize = GameConstants.PLAYER_BODY_SIZE;
    private final float halfBodySize = bodySize * 0.5F;
    private final int fillColor;

    private float aimAngle;
    private int chargedFrameCount;
    private boolean chargeReadyFeedbackShown;
    private int longbowRecoveryFrameCount;
    private int damageRemainingFrameCount;
    private int damageEndFeedbackFrameCount;
    private int shortbowCooldownFrameCount;
    private final ShortbowAmmo shortbowAmmo = new ShortbowAmmo(
            GameConstants.SHORTBOW_MAX_AMMO,
            Math.round(GameConstants.SHORTBOW_AMMO_RECOVERY_SEC * FPS));
    private final ShortbowPressure shortbowPressure = new ShortbowPressure(
            GameConstants.SHORTBOW_MAX_CONSECUTIVE_PRESSURES);

    public PlayerActor(PlayerEngine _engine, int col, App app) {
        super(16, _engine, app);
        fillColor = col;
    }

    public float getBodySize() {
        return bodySize;
    }

    public float getHalfBodySize() {
        return halfBodySize;
    }

    public int getFillColor() {
        return fillColor;
    }

    public float getAimAngle() {
        return aimAngle;
    }

    public void setAimAngle(float aimAngle) {
        this.aimAngle = aimAngle;
    }

    public int getChargedFrameCount() {
        return chargedFrameCount;
    }

    public void setChargedFrameCount(int chargedFrameCount) {
        this.chargedFrameCount = chargedFrameCount;
    }

    public boolean isChargeReadyFeedbackShown() {
        return chargeReadyFeedbackShown;
    }

    public void setChargeReadyFeedbackShown(boolean chargeReadyFeedbackShown) {
        this.chargeReadyFeedbackShown = chargeReadyFeedbackShown;
    }

    public int getLongbowRecoveryFrameCount() {
        return longbowRecoveryFrameCount;
    }

    public void setLongbowRecoveryFrameCount(int longbowRecoveryFrameCount) {
        this.longbowRecoveryFrameCount = Math.max(0, longbowRecoveryFrameCount);
    }

    public int getDamageRemainingFrameCount() {
        return damageRemainingFrameCount;
    }

    public void setDamageRemainingFrameCount(int damageRemainingFrameCount) {
        this.damageRemainingFrameCount = damageRemainingFrameCount;
    }

    public int getDamageEndFeedbackFrameCount() {
        return damageEndFeedbackFrameCount;
    }

    public void setDamageEndFeedbackFrameCount(int damageEndFeedbackFrameCount) {
        this.damageEndFeedbackFrameCount = damageEndFeedbackFrameCount;
    }

    public int getShortbowCooldownFrameCount() {
        return shortbowCooldownFrameCount;
    }

    public void setShortbowCooldownFrameCount(int shortbowCooldownFrameCount) {
        this.shortbowCooldownFrameCount = shortbowCooldownFrameCount;
    }

    public ShortbowAmmo getShortbowAmmo() {
        return shortbowAmmo;
    }

    public ShortbowPressure getShortbowPressure() {
        return shortbowPressure;
    }

    /** Restores movement, weapon state, and reserve ammo to the supplied round spawn point. */
    public void resetForRound(float spawnX, float spawnY, PlayerActorState moveState) {
        xPosition = spawnX;
        yPosition = spawnY;
        xVelocity = 0;
        yVelocity = 0;
        directionAngle = 0;
        speed = 0;
        rotationAngle = 0;
        aimAngle = 0;
        chargedFrameCount = 0;
        chargeReadyFeedbackShown = false;
        longbowRecoveryFrameCount = 0;
        damageRemainingFrameCount = 0;
        damageEndFeedbackFrameCount = 0;
        shortbowCooldownFrameCount = 0;
        shortbowAmmo.reset();
        shortbowPressure.reset();
        state = moveState.entryState(this);
    }


    public void addVelocity(float xAcceleration, float yAcceleration) {
        // Keep diagonal input at the same acceleration magnitude as a cardinal direction before applying caps.
        final float accelerationScale = inputAccelerationScale(xAcceleration, yAcceleration);
        xVelocity = constrain(
                xVelocity + xAcceleration * accelerationScale,
                -GameConstants.PLAYER_MAX_VX,
                GameConstants.PLAYER_MAX_VX);
        yVelocity = constrain(
                yVelocity + yAcceleration * accelerationScale,
                -GameConstants.PLAYER_MAX_VY,
                GameConstants.PLAYER_MAX_VY);
    }

    /** Returns the deterministic scale that prevents simultaneous horizontal and vertical input from being faster. */
    static float inputAccelerationScale(float xAcceleration, float yAcceleration) {
        return xAcceleration != 0.0F && yAcceleration != 0.0F
                ? (float) (1.0 / Math.sqrt(2.0))
                : 1.0F;
    }

    public void act() {
        engine.run(this);
        state.act(this);
    }

    public void update() {
        super.update();
        shortbowAmmo.tickRecovery();
        // Cooldown advances during every movement state so a released shortbow cannot leave it frozen.
        shortbowCooldownFrameCount = Math.max(0, shortbowCooldownFrameCount - 1);
        longbowRecoveryFrameCount = Math.max(0, longbowRecoveryFrameCount - 1);
        damageEndFeedbackFrameCount = Math.max(0, damageEndFeedbackFrameCount - 1);

        if (xPosition < halfBodySize) {
            xPosition = halfBodySize;
            xVelocity *= -GameConstants.PLAYER_BOUNCE;
        }
        if (xPosition > INTERNAL_CANVAS_WIDTH - halfBodySize) {
            xPosition = INTERNAL_CANVAS_WIDTH - halfBodySize;
            xVelocity *= -GameConstants.PLAYER_BOUNCE;
        }
        if (yPosition < halfBodySize) {
            yPosition = halfBodySize;
            yVelocity *= -GameConstants.PLAYER_BOUNCE;
        }
        if (yPosition > INTERNAL_CANVAS_HEIGHT - halfBodySize) {
            yPosition = INTERNAL_CANVAS_HEIGHT - halfBodySize;
            yVelocity *= -GameConstants.PLAYER_BOUNCE;
        }

        xVelocity *= GameConstants.PLAYER_FRICTION;
        yVelocity *= GameConstants.PLAYER_FRICTION;

        rotationAngle += (float) ((0.1 + 0.04 * (sq(xVelocity) + sq(yVelocity))) * TWO_PI / FPS);
    }

    public void display() {
        app.stroke(0);
        app.fill(fillColor);
        app.pushMatrix();
        app.translate(xPosition, yPosition);
        app.pushMatrix();
        app.rotate(rotationAngle);
        app.rect(0, 0, 32, 32);
        app.popMatrix();
        state.displayEffect(this);
        app.popMatrix();
    }
}
