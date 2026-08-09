package com.likanug.dual.game;

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
}
