package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LethalHitSnapshotTest {

    @Test
    void keepsTheActualLaunchImpactAndTargetGeometry() {
        LethalHitSnapshot snapshot = new LethalHitSnapshot(
                PlayerSide.ONE, 100.0F, 600.0F, 420.0F, 240.0F, 432.0F, 250.0F, 0);

        assertEquals(PlayerSide.ONE, snapshot.attacker());
        assertEquals(100.0F, snapshot.launchX());
        assertEquals(420.0F, snapshot.impactX());
        assertEquals(432.0F, snapshot.targetX());
    }

    @Test
    void rejectsMissingSidesAndNonFiniteGeometry() {
        assertThrows(IllegalArgumentException.class, () -> new LethalHitSnapshot(
                null, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0));
        assertThrows(IllegalArgumentException.class, () -> new LethalHitSnapshot(
                PlayerSide.TWO, Float.NaN, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 255));
    }
}
