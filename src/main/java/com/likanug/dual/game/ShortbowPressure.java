package com.likanug.dual.game;

/**
 * Limits how many consecutive shortbow hits can refresh one pressure window.
 * It changes only the vulnerable-window refresh, never the hit or knockback itself.
 */
public final class ShortbowPressure {

    private final int maximumConsecutiveRefreshes;
    private int consecutiveRefreshes;

    public ShortbowPressure(int maximumConsecutiveRefreshes) {
        if (maximumConsecutiveRefreshes <= 0) {
            throw new IllegalArgumentException("Maximum pressure refreshes must be positive.");
        }
        this.maximumConsecutiveRefreshes = maximumConsecutiveRefreshes;
    }

    /** Records one hit and reports whether it may refresh the current vulnerable window. */
    public boolean recordHit() {
        if (consecutiveRefreshes >= maximumConsecutiveRefreshes) return false;
        consecutiveRefreshes++;
        return true;
    }

    public int getMaximumConsecutiveRefreshes() {
        return maximumConsecutiveRefreshes;
    }

    public int getConsecutiveRefreshes() {
        return consecutiveRefreshes;
    }

    /** Clears the cap when a pressure window ends or a round resets. */
    public void reset() {
        consecutiveRefreshes = 0;
    }
}
