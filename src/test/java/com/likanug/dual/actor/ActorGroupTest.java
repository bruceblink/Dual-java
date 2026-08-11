package com.likanug.dual.actor;

import com.likanug.dual.App;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActorGroupTest {

    @Test
    void pendingRemovalArrowIsHiddenBeforeAHitStopFrameDisplays() {
        ActorGroup group = new ActorGroup();
        CountingArrow arrow = new CountingArrow();
        group.addArrow(arrow);
        group.getRemovingArrowList().add(arrow);

        group.displayArrows();

        assertEquals(0, arrow.displayCount);
    }

    private static final class CountingArrow extends AbstractArrowActor {
        private int displayCount;

        private CountingArrow() {
            super(1.0F, 1.0F, new App());
        }

        @Override
        protected void act() {
        }

        @Override
        public void display() {
            displayCount++;
        }

        @Override
        public boolean isLethal() {
            return false;
        }
    }
}
