package com.likanug.dual.actor.player;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.playerEngine.PlayerEngine;

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
    private int damageRemainingFrameCount;
    private int shortbowCooldownFrameCount;
    private final ShortbowAmmo shortbowAmmo = new ShortbowAmmo(
            GameConstants.SHORTBOW_MAX_AMMO,
            Math.round(GameConstants.SHORTBOW_AMMO_RECOVERY_SEC * FPS));

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

    public int getDamageRemainingFrameCount() {
        return damageRemainingFrameCount;
    }

    public void setDamageRemainingFrameCount(int damageRemainingFrameCount) {
        this.damageRemainingFrameCount = damageRemainingFrameCount;
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


    public void addVelocity(float xAcceleration, float yAcceleration) {
        xVelocity = constrain(xVelocity + xAcceleration, -GameConstants.PLAYER_MAX_VX, GameConstants.PLAYER_MAX_VX);
        yVelocity = constrain(yVelocity + yAcceleration, -GameConstants.PLAYER_MAX_VY, GameConstants.PLAYER_MAX_VY);
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
