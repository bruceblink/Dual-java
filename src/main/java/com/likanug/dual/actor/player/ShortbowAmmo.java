package com.likanug.dual.actor.player;

/**
 * Tracks the shortbow's limited reserve and one-arrow-at-a-time recovery timer.
 * A fired arrow restarts recovery, while a full reserve never accumulates hidden progress.
 */
public final class ShortbowAmmo {

    private final int maximumAmmo;
    private final int recoveryFrameCount;
    private int availableAmmo;
    private int recoveryProgressFrameCount;

    public ShortbowAmmo(int maximumAmmo, int recoveryFrameCount) {
        if (maximumAmmo <= 0 || recoveryFrameCount <= 0) {
            throw new IllegalArgumentException("Shortbow ammo settings must be positive.");
        }
        this.maximumAmmo = maximumAmmo;
        this.recoveryFrameCount = recoveryFrameCount;
        availableAmmo = maximumAmmo;
    }

    public int getAvailableAmmo() {
        return availableAmmo;
    }

    public boolean canFire() {
        return availableAmmo > 0;
    }

    /** Consumes one arrow and starts a fresh recovery period when the reserve is no longer full. */
    public boolean consume() {
        if (!canFire()) return false;
        availableAmmo--;
        recoveryProgressFrameCount = 0;
        return true;
    }

    /** Advances one game frame and restores at most one arrow after the configured interval. */
    public void tickRecovery() {
        if (availableAmmo >= maximumAmmo) {
            recoveryProgressFrameCount = 0;
            return;
        }

        recoveryProgressFrameCount++;
        if (recoveryProgressFrameCount < recoveryFrameCount) return;

        availableAmmo++;
        recoveryProgressFrameCount = 0;
    }
}
