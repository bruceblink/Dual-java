package com.likanug.dual.game;

/** Immutable geometry captured at a lethal collision for the freeze and result presentation states. */
public record LethalHitSnapshot(
        PlayerSide attacker,
        float launchX,
        float launchY,
        float impactX,
        float impactY,
        float targetX,
        float targetY,
        int targetColor) {

    public LethalHitSnapshot {
        if (attacker == null) throw new IllegalArgumentException("Attacker is required.");
        if (!Float.isFinite(launchX) || !Float.isFinite(launchY)
                || !Float.isFinite(impactX) || !Float.isFinite(impactY)
                || !Float.isFinite(targetX) || !Float.isFinite(targetY)) {
            throw new IllegalArgumentException("Lethal hit geometry must be finite.");
        }
    }
}
