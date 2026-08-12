package com.likanug.dual.actor.arrow;

import com.likanug.dual.App;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static processing.core.PConstants.PI;

class AbstractArrowActorTest {

    @Test
    void oppositeShortbowsCollideWhenTheyExchangePositionsInOneFrame() {
        ShortbowArrow first = shortbowAt(100.0F, 100.0F, 0.0F, 24.0F);
        ShortbowArrow second = shortbowAt(124.0F, 100.0F, PI, 24.0F);

        first.update();
        second.update();

        AbstractArrowActor.ArrowCollision collision = first.findCollision(second).orElseThrow();
        assertEquals(1.0F / 6.0F, collision.timeRatio(), 1.0e-5F);
        assertEquals(112.0F, collision.impactX(), 1.0e-4F);
        assertEquals(100.0F, collision.impactY(), 1.0e-4F);
    }

    @Test
    void crossingArrowsWithMoreThanTheirCombinedRadiusRemainANearMiss() {
        ShortbowArrow first = shortbowAt(100.0F, 100.0F, 0.0F, 24.0F);
        ShortbowArrow second = shortbowAt(124.0F, 117.0F, PI, 24.0F);

        first.update();
        second.update();

        assertTrue(first.findCollision(second).isEmpty());
    }

    @Test
    void overlappingEndpointsStillCollideWithoutMovementHistory() {
        ShortbowArrow first = shortbowAt(100.0F, 100.0F, 0.0F, 0.0F);
        ShortbowArrow second = shortbowAt(108.0F, 100.0F, 0.0F, 0.0F);

        AbstractArrowActor.ArrowCollision collision = first.findCollision(second).orElseThrow();

        assertEquals(1.0F, collision.timeRatio());
        assertEquals(104.0F, collision.impactX());
        assertEquals(100.0F, collision.impactY());
    }

    @Test
    void parallelArrowsDoNotCollideWhenTheirSeparationNeverChanges() {
        ShortbowArrow first = shortbowAt(100.0F, 100.0F, 0.0F, 24.0F);
        ShortbowArrow second = shortbowAt(124.0F, 100.0F, 0.0F, 24.0F);

        first.update();
        second.update();

        assertTrue(first.findCollision(second).isEmpty());
    }

    @Test
    void fastLongbowAndShortbowCollideAcrossAFrame() {
        LongbowArrowHead longbow = new LongbowArrowHead(new App());
        longbow.setxPosition(100.0F);
        longbow.setyPosition(100.0F);
        longbow.setVelocity(0.0F, 64.0F);
        ShortbowArrow shortbow = shortbowAt(164.0F, 100.0F, PI, 24.0F);

        longbow.update();
        shortbow.update();

        AbstractArrowActor.ArrowCollision collision = longbow.findCollision(shortbow).orElseThrow();
        assertEquals(0.45454547F, collision.timeRatio(), 1.0e-5F);
        assertEquals(141.09091F, collision.impactX(), 1.0e-4F);
        assertEquals(100.0F, collision.impactY(), 1.0e-4F);
    }

    private static ShortbowArrow shortbowAt(float x, float y, float direction, float speed) {
        ShortbowArrow arrow = new ShortbowArrow(new App());
        arrow.setxPosition(x);
        arrow.setyPosition(y);
        arrow.setVelocity(direction, speed);
        return arrow;
    }
}
