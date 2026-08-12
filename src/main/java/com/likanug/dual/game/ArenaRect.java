package com.likanug.dual.game;

import java.util.Optional;

/** Describes one axis-aligned arena obstacle in fixed internal-canvas coordinates. */
public record ArenaRect(float centerX, float centerY, float width, float height) {

    public ArenaRect {
        if (width <= 0.0F || height <= 0.0F) {
            throw new IllegalArgumentException("Arena obstacles must have positive dimensions.");
        }
    }

    public boolean containsCircle(float x, float y, float radius) {
        final float closestX = Math.max(left(), Math.min(x, right()));
        final float closestY = Math.max(top(), Math.min(y, bottom()));
        final float deltaX = x - closestX;
        final float deltaY = y - closestY;
        return deltaX * deltaX + deltaY * deltaY < radius * radius;
    }

    /**
     * Finds the first exact contact between a moving circle and this rectangle's rounded expansion.
     * Face and corner candidates are tested separately so diagonal near misses are not falsely blocked.
     */
    public Optional<CircleImpact> findFirstCircleImpact(
            float startX,
            float startY,
            float endX,
            float endY,
            float radius) {
        if (radius <= 0.0F) throw new IllegalArgumentException("Collision radius must be positive.");
        final float movementX = endX - startX;
        final float movementY = endY - startY;
        if (containsCircle(startX, startY, radius)) {
            return Optional.of(projectBlockedPoint(startX, startY, movementX, movementY));
        }

        CircleImpact earliest = null;
        earliest = earlier(earliest, faceImpact(
                startX, startY, movementX, movementY, left() - radius, true, left(), -1.0F));
        earliest = earlier(earliest, faceImpact(
                startX, startY, movementX, movementY, right() + radius, true, right(), 1.0F));
        earliest = earlier(earliest, faceImpact(
                startX, startY, movementX, movementY, top() - radius, false, top(), -1.0F));
        earliest = earlier(earliest, faceImpact(
                startX, startY, movementX, movementY, bottom() + radius, false, bottom(), 1.0F));

        earliest = earlier(earliest, cornerImpact(
                startX, startY, movementX, movementY, radius, left(), top(), -1, -1));
        earliest = earlier(earliest, cornerImpact(
                startX, startY, movementX, movementY, radius, right(), top(), 1, -1));
        earliest = earlier(earliest, cornerImpact(
                startX, startY, movementX, movementY, radius, left(), bottom(), -1, 1));
        earliest = earlier(earliest, cornerImpact(
                startX, startY, movementX, movementY, radius, right(), bottom(), 1, 1));
        return Optional.ofNullable(earliest);
    }

    /** Tests one expanded face while restricting its contact point to the real rectangle edge. */
    private CircleImpact faceImpact(
            float startX,
            float startY,
            float movementX,
            float movementY,
            float expandedCoordinate,
            boolean verticalFace,
            float surfaceCoordinate,
            float normalSign) {
        final float movement = verticalFace ? movementX : movementY;
        if (Math.abs(movement) <= 1.0E-6F) return null;
        if (movement * normalSign >= 0.0F) return null;

        final float start = verticalFace ? startX : startY;
        final float timeRatio = (expandedCoordinate - start) / movement;
        if (timeRatio < 0.0F || timeRatio > 1.0F) return null;

        final float otherCoordinate = verticalFace
                ? startY + movementY * timeRatio
                : startX + movementX * timeRatio;
        final float minimum = verticalFace ? top() : left();
        final float maximum = verticalFace ? bottom() : right();
        if (otherCoordinate < minimum || otherCoordinate > maximum) return null;

        return verticalFace
                ? new CircleImpact(timeRatio, surfaceCoordinate, otherCoordinate, normalSign, 0.0F)
                : new CircleImpact(timeRatio, otherCoordinate, surfaceCoordinate, 0.0F, normalSign);
    }

    /** Solves the segment-circle equation for one rounded rectangle corner. */
    private static CircleImpact cornerImpact(
            float startX,
            float startY,
            float movementX,
            float movementY,
            float radius,
            float cornerX,
            float cornerY,
            int horizontalSide,
            int verticalSide) {
        final float movementSquared = movementX * movementX + movementY * movementY;
        if (movementSquared <= 1.0E-6F || radius <= 0.0F) return null;

        final float offsetX = startX - cornerX;
        final float offsetY = startY - cornerY;
        final float projection = offsetX * movementX + offsetY * movementY;
        final float distanceOffset = offsetX * offsetX + offsetY * offsetY - radius * radius;
        final float discriminant = projection * projection - movementSquared * distanceOffset;
        if (discriminant < 0.0F) return null;

        final float timeRatio = (-projection - (float) Math.sqrt(discriminant)) / movementSquared;
        if (timeRatio < 0.0F || timeRatio > 1.0F) return null;
        final float centerX = startX + movementX * timeRatio;
        final float centerY = startY + movementY * timeRatio;
        if ((centerX - cornerX) * horizontalSide < -1.0E-4F
                || (centerY - cornerY) * verticalSide < -1.0E-4F) {
            return null;
        }

        final float normalX = (centerX - cornerX) / radius;
        final float normalY = (centerY - cornerY) / radius;
        if (movementX * normalX + movementY * normalY >= 0.0F) return null;
        return new CircleImpact(timeRatio, cornerX, cornerY, normalX, normalY);
    }

    /** Projects a circle that begins overlapped onto the most credible entry face. */
    private CircleImpact projectBlockedPoint(
            float centerX,
            float centerY,
            float movementX,
            float movementY) {
        final float closestX = Math.max(left(), Math.min(centerX, right()));
        final float closestY = Math.max(top(), Math.min(centerY, bottom()));
        final float deltaX = centerX - closestX;
        final float deltaY = centerY - closestY;
        final float distanceSquared = deltaX * deltaX + deltaY * deltaY;
        if (distanceSquared > 1.0E-6F) {
            final float inverseDistance = 1.0F / (float) Math.sqrt(distanceSquared);
            return new CircleImpact(0.0F, closestX, closestY,
                    deltaX * inverseDistance, deltaY * inverseDistance);
        }
        if (Math.abs(movementX) >= Math.abs(movementY) && movementX != 0.0F) {
            return movementX > 0.0F
                    ? new CircleImpact(0.0F, left(), centerY, -1.0F, 0.0F)
                    : new CircleImpact(0.0F, right(), centerY, 1.0F, 0.0F);
        }
        if (movementY != 0.0F) {
            return movementY > 0.0F
                    ? new CircleImpact(0.0F, centerX, top(), 0.0F, -1.0F)
                    : new CircleImpact(0.0F, centerX, bottom(), 0.0F, 1.0F);
        }

        final float leftDistance = centerX - left();
        final float rightDistance = right() - centerX;
        final float topDistance = centerY - top();
        final float bottomDistance = bottom() - centerY;
        final float minimumDistance = Math.min(
                Math.min(leftDistance, rightDistance), Math.min(topDistance, bottomDistance));
        if (minimumDistance == leftDistance) return new CircleImpact(0.0F, left(), centerY, -1.0F, 0.0F);
        if (minimumDistance == rightDistance) return new CircleImpact(0.0F, right(), centerY, 1.0F, 0.0F);
        if (minimumDistance == topDistance) return new CircleImpact(0.0F, centerX, top(), 0.0F, -1.0F);
        return new CircleImpact(0.0F, centerX, bottom(), 0.0F, 1.0F);
    }

    private static CircleImpact earlier(CircleImpact current, CircleImpact candidate) {
        if (candidate == null) return current;
        return current == null || candidate.timeRatio() < current.timeRatio() ? candidate : current;
    }

    public float left() {
        return centerX - width * 0.5F;
    }

    public float right() {
        return centerX + width * 0.5F;
    }

    public float top() {
        return centerY - height * 0.5F;
    }

    public float bottom() {
        return centerY + height * 0.5F;
    }

    /** Describes one first-contact point on the rectangle and its outward unit normal. */
    public record CircleImpact(float timeRatio, float x, float y, float normalX, float normalY) {
    }
}
