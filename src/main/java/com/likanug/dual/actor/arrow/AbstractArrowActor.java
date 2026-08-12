package com.likanug.dual.actor.arrow;

import com.likanug.dual.App;
import com.likanug.dual.actor.Actor;

import java.util.Optional;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;

public abstract class AbstractArrowActor extends Actor {

    protected final float halfLength;
    private float launchX;
    private float launchY;
    private boolean hasLaunchPosition;
    private float previousXPosition;
    private float previousYPosition;
    private boolean hasPreviousPosition;

    public AbstractArrowActor(float _collisionRadius, float _halfLength, App app) {
        super(_collisionRadius, app);
        halfLength = _halfLength;
    }

    public void update() {
        previousXPosition = xPosition;
        previousYPosition = yPosition;
        hasPreviousPosition = true;
        super.update();
        if (
                xPosition < -halfLength ||
                        xPosition > INTERNAL_CANVAS_WIDTH + halfLength ||
                        yPosition < -halfLength ||
                        yPosition > INTERNAL_CANVAS_HEIGHT + halfLength
        ) {
            group.getRemovingArrowList().add(this);
        }
    }

    public abstract boolean isLethal();

    /**
     * Finds the first contact of two circular arrows during their latest movement step.
     * This keeps fast projectiles interactive even when their frame-end circles no longer overlap.
     */
    public Optional<ArrowCollision> findCollision(AbstractArrowActor other) {
        if (!hasPreviousPosition || !other.hasPreviousPosition) {
            if (isNotCollided(other)) return Optional.empty();
            return Optional.of(new ArrowCollision(
                    1.0F,
                    (xPosition + other.xPosition) * 0.5F,
                    (yPosition + other.yPosition) * 0.5F));
        }

        final float myStepX = xPosition - previousXPosition;
        final float myStepY = yPosition - previousYPosition;
        final float otherStepX = other.xPosition - other.previousXPosition;
        final float otherStepY = other.yPosition - other.previousYPosition;
        final float relativeStartX = previousXPosition - other.previousXPosition;
        final float relativeStartY = previousYPosition - other.previousYPosition;
        final float relativeStepX = myStepX - otherStepX;
        final float relativeStepY = myStepY - otherStepY;
        final float collisionDistance = collisionRadius + other.collisionRadius;
        final float startDistanceSquared = relativeStartX * relativeStartX + relativeStartY * relativeStartY;
        final float collisionDistanceSquared = collisionDistance * collisionDistance;
        final float relativeSpeedSquared = relativeStepX * relativeStepX + relativeStepY * relativeStepY;
        final float approachDot = relativeStartX * relativeStepX + relativeStartY * relativeStepY;
        final float firstContactTime;
        if (startDistanceSquared < collisionDistanceSquared) {
            firstContactTime = 0.0F;
        } else if (relativeSpeedSquared <= 0.0F || approachDot >= 0.0F) {
            return Optional.empty();
        } else {
            final float distanceOffset = startDistanceSquared - collisionDistanceSquared;
            final float discriminant = approachDot * approachDot - relativeSpeedSquared * distanceOffset;
            if (discriminant <= 0.0F) return Optional.empty();
            firstContactTime = (-approachDot - (float) Math.sqrt(discriminant)) / relativeSpeedSquared;
            if (firstContactTime < 0.0F || firstContactTime > 1.0F) return Optional.empty();
        }

        final float myImpactX = previousXPosition + myStepX * firstContactTime;
        final float myImpactY = previousYPosition + myStepY * firstContactTime;
        final float otherImpactX = other.previousXPosition + otherStepX * firstContactTime;
        final float otherImpactY = other.previousYPosition + otherStepY * firstContactTime;
        return Optional.of(new ArrowCollision(
                firstContactTime,
                (myImpactX + otherImpactX) * 0.5F,
                (myImpactY + otherImpactY) * 0.5F));
    }

    /** Describes the normalized frame time and world point used by interception feedback. */
    public record ArrowCollision(float timeRatio, float impactX, float impactY) {
    }

    /** Saves the firing point once so later hit feedback can show the arrow's truthful path. */
    public void setLaunchPosition(float launchX, float launchY) {
        this.launchX = launchX;
        this.launchY = launchY;
        hasLaunchPosition = true;
    }

    public boolean hasLaunchPosition() {
        return hasLaunchPosition;
    }

    public float getLaunchX() {
        return launchX;
    }

    public float getLaunchY() {
        return launchY;
    }
}
