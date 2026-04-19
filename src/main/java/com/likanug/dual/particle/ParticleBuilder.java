package com.likanug.dual.particle;

import com.likanug.dual.App;
import processing.core.PApplet;

import static com.likanug.dual.App.FPS;

public class ParticleBuilder {

    private final App app;
    private int particleTypeNumber = 0;

    private float xPosition = 0, yPosition = 0;
    private float xVelocity = 0, yVelocity = 0;
    private float directionAngle = 0, speed = 0;

    private float rotationAngle = 0;
    private int displayColor = 0;
    private float strokeWeightValue = 1;
    private int displaySize = 10;

    private float lifespanFrameCount = 60;

    public ParticleBuilder(App app) {
        this.app = app;
    }

    public ParticleBuilder initialize() {
        particleTypeNumber = 0;
        xPosition = 0;
        yPosition = 0;
        xVelocity = 0;
        yVelocity = 0;
        directionAngle = 0;
        speed = 0;
        rotationAngle = 0;
        displayColor = 0;
        strokeWeightValue = 1;
        displaySize = 10;
        lifespanFrameCount = 60;
        return this;
    }

    public ParticleBuilder type(int v) {
        particleTypeNumber = v;
        return this;
    }

    public ParticleBuilder position(float x, float y) {
        xPosition = x;
        yPosition = y;
        return this;
    }

    public ParticleBuilder polarVelocity(float dir, float spd) {
        directionAngle = dir;
        speed = spd;
        xVelocity = spd * PApplet.cos(dir);
        yVelocity = spd * PApplet.sin(dir);
        return this;
    }

    public ParticleBuilder rotation(float v) {
        rotationAngle = v;
        return this;
    }

    public ParticleBuilder particleColor(int c) {
        displayColor = c;
        return this;
    }

    public ParticleBuilder weight(float v) {
        strokeWeightValue = v;
        return this;
    }

    public ParticleBuilder particleSize(int v) {
        displaySize = v;
        return this;
    }

    public ParticleBuilder lifespan(float v) {
        lifespanFrameCount = v;
        return this;
    }

    public ParticleBuilder lifespanSecond(float v) {
        lifespan(v * FPS);
        return this;
    }

    public Particle build() {
        final Particle newParticle = app.getSystem().getCommonParticleSet().allocate();
        newParticle.setParticleTypeNumber(this.particleTypeNumber);
        newParticle.setxPosition(this.xPosition);
        newParticle.setyPosition(this.yPosition);
        newParticle.setxVelocity(this.xVelocity);
        newParticle.setyVelocity(this.yVelocity);
        newParticle.setDirectionAngle(this.directionAngle);
        newParticle.setSpeed(this.speed);
        newParticle.setRotationAngle(this.rotationAngle);
        newParticle.setDisplayColor(this.displayColor);
        newParticle.setStrokeWeightValue(this.strokeWeightValue);
        newParticle.setDisplaySize(this.displaySize);
        newParticle.setLifespanFrameCount(this.lifespanFrameCount);
        return newParticle;
    }

}
