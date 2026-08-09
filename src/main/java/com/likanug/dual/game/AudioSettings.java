package com.likanug.dual.game;

/** Stores the small set of user-facing volume levels without coupling settings to Processing. */
public final class AudioSettings {

    private static final float[] LEVELS = {0.0F, 0.5F, 1.0F};
    private int levelIndex = LEVELS.length - 1;

    public float getVolume() {
        return LEVELS[levelIndex];
    }

    /** Moves one step toward louder feedback and reports the resulting normalized volume. */
    public float increaseVolume() {
        levelIndex = Math.min(LEVELS.length - 1, levelIndex + 1);
        return getVolume();
    }

    /** Moves one step toward silence and reports the resulting normalized volume. */
    public float decreaseVolume() {
        levelIndex = Math.max(0, levelIndex - 1);
        return getVolume();
    }

    /** Toggles between muted and full feedback while preserving the simple three-level model. */
    public float toggleMute() {
        levelIndex = levelIndex == 0 ? LEVELS.length - 1 : 0;
        return getVolume();
    }

    public String displayLabel() {
        return getVolume() == 0.0F ? "Muted" : Math.round(getVolume() * 100.0F) + "%";
    }
}
