package com.likanug.dual.particle;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.common.GameObject;
import com.likanug.dual.pool.ObjectPool;
import com.likanug.dual.pool.Poolable;
import processing.core.PApplet;

import static com.likanug.dual.App.FPS;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.TWO_PI;

public class Particle extends GameObject implements Poolable<Particle> {

    private boolean allocatedIndicator;
    private ObjectPool<Particle> belongingPool;
    private int allocationIdentifier;

    private float rotationAngle;
    private int displayColor;
    private float strokeWeightValue;
    private float displaySize;

    private float lifespanFrameCount;
    private int properFrameCount;
    private int particleTypeNumber;

    public Particle(App app) {
        this.app = app;
    }

    // override methods of Poolable
    public boolean isAllocated() {
        return allocatedIndicator;
    }

    public void setAllocated(boolean indicator) {
        allocatedIndicator = indicator;
    }

    public ObjectPool<Particle> getBelongingPool() {
        return belongingPool;
    }

    public void setBelongingPool(ObjectPool<Particle> pool) {
        belongingPool = pool;
    }

    public int getAllocationIdentifier() {
        return allocationIdentifier;
    }

    public void setAllocationIdentifier(int id) {
        allocationIdentifier = id;
    }

    public void initialize() {
        xPosition = 0;
        yPosition = 0;
        xVelocity = 0;
        yVelocity = 0;
        directionAngle = 0;
        speed = 0;

        rotationAngle = 0;
        displayColor = app.color(0);
        strokeWeightValue = 1;
        displaySize = 10;

        lifespanFrameCount = 0;
        properFrameCount = 0;
        particleTypeNumber = 0;
    }


    public void update() {
        super.update();

        xVelocity *= GameConstants.PARTICLE_FRICTION;
        yVelocity *= GameConstants.PARTICLE_FRICTION;

        properFrameCount++;
        if (properFrameCount > lifespanFrameCount)
            app.getSystem().getCommonParticleSet().getRemovingParticleList().add(this);

        if (particleTypeNumber == 1) {    // Square
            rotationAngle += GameConstants.PARTICLE_SQUARE_ROT_SPEED * TWO_PI / FPS;
        }
    }

    public float getProgressRatio() {
        return PApplet.min(1, properFrameCount / lifespanFrameCount);
    }

    public float getFadeRatio() {
        return 1 - getProgressRatio();
    }

    public void display() {
        switch (particleTypeNumber) {
            case 0 ->  // Dot
                    app.set((int) xPosition, (int) yPosition, app.color(128 + 127 * getProgressRatio()));
            case 1 -> {  // Square
                app.noFill();
                app.stroke(displayColor, 255 * getFadeRatio());
                app.pushMatrix();
                app.translate(xPosition, yPosition);
                app.rotate(rotationAngle);
                app.rect(0, 0, displaySize, displaySize);
                app.popMatrix();
            }
            case 2 -> {  // Line
                app.stroke(displayColor, 128 * getFadeRatio());
                app.strokeWeight(strokeWeightValue * PApplet.pow(getFadeRatio(), 4));
                app.line(xPosition, yPosition,
                        xPosition + GameConstants.PARTICLE_LINE_LENGTH * cos(rotationAngle),
                        yPosition + GameConstants.PARTICLE_LINE_LENGTH * sin(rotationAngle));
                app.strokeWeight(1);
            }
            case 3 -> {  // Ring
                final float ringSizeExpandRatio = 2 * (PApplet.pow(getProgressRatio() - 1, 5) + 1);
                app.noFill();
                app.stroke(displayColor, 255 * getFadeRatio());
                app.strokeWeight(strokeWeightValue * getFadeRatio());
                app.ellipse(xPosition, yPosition, displaySize * (1 + ringSizeExpandRatio), displaySize * (1 + ringSizeExpandRatio));
                app.strokeWeight(1);
            }
            default -> {
            }
        }
    }

    public boolean isAllocatedIndicator() {
        return allocatedIndicator;
    }

    public void setAllocatedIndicator(boolean allocatedIndicator) {
        this.allocatedIndicator = allocatedIndicator;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public void setRotationAngle(float rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    public int getDisplayColor() {
        return displayColor;
    }

    public void setDisplayColor(int displayColor) {
        this.displayColor = displayColor;
    }

    public float getStrokeWeightValue() {
        return strokeWeightValue;
    }

    public void setStrokeWeightValue(float strokeWeightValue) {
        this.strokeWeightValue = strokeWeightValue;
    }

    public float getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(float displaySize) {
        this.displaySize = displaySize;
    }

    public float getLifespanFrameCount() {
        return lifespanFrameCount;
    }

    public void setLifespanFrameCount(float lifespanFrameCount) {
        this.lifespanFrameCount = lifespanFrameCount;
    }

    public int getProperFrameCount() {
        return properFrameCount;
    }

    public void setProperFrameCount(int properFrameCount) {
        this.properFrameCount = properFrameCount;
    }

    public int getParticleTypeNumber() {
        return particleTypeNumber;
    }

    public void setParticleTypeNumber(int particleTypeNumber) {
        this.particleTypeNumber = particleTypeNumber;
    }
}
