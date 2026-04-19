package com.likanug.dual.actor.player;

import com.likanug.dual.App;
import com.likanug.dual.playerEngine.PlayerEngine;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_SIDE_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_SIDE_WIDTH;
import static processing.core.PApplet.constrain;
import static processing.core.PApplet.sq;
import static processing.core.PConstants.TWO_PI;

public class PlayerActor extends AbstractPlayerActor {

    private final float bodySize = 32.0F;
    private final float halfBodySize = bodySize * 0.5F;
    private final int fillColor;

    private float aimAngle;
    private int chargedFrameCount;
    private int damageRemainingFrameCount;

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


    public void addVelocity(float xAcceleration, float yAcceleration) {
        xVelocity = constrain(xVelocity + xAcceleration, -10, 10);
        yVelocity = constrain(yVelocity + yAcceleration, -7, 7);
    }

    public void act() {
        engine.run(this);
        state.act(this);
    }

    public void update() {
        super.update();

        if (xPosition < halfBodySize) {
            xPosition = halfBodySize;
            xVelocity = (float) (-0.5 * xVelocity);
        }
        if (xPosition > INTERNAL_CANVAS_SIDE_WIDTH - halfBodySize) {
            xPosition = INTERNAL_CANVAS_SIDE_WIDTH - halfBodySize;
            xVelocity = (float) (-0.5 * xVelocity);
        }
        if (yPosition < halfBodySize) {
            yPosition = halfBodySize;
            yVelocity = (float) (-0.5 * yVelocity);
        }
        if (yPosition > INTERNAL_CANVAS_SIDE_HEIGHT - halfBodySize) {
            yPosition = INTERNAL_CANVAS_SIDE_HEIGHT - halfBodySize;
            yVelocity = (float) (-0.5 * yVelocity);
        }

        xVelocity = (float) (xVelocity * 0.92);
        yVelocity = (float) (yVelocity * 0.92);

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
