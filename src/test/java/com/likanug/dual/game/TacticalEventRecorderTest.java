package com.likanug.dual.game;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalEventRecorderTest {

    @Test
    void recordsACompletePressureOpeningFinishSequence() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);

        assertEquals(
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.PRESSURE, 10),
                recorder.recordPressure(PlayerSide.ONE, 10));
        assertEquals(
                Optional.of(new TacticalEvent(PlayerSide.ONE, TacticalEventType.OPENING, 30)),
                recorder.recordLongbowChargeStarted(PlayerSide.ONE, 30));
        assertEquals(
                Optional.of(new TacticalEvent(PlayerSide.ONE, TacticalEventType.FINISH, 100)),
                recorder.recordLongbowFinish(PlayerSide.ONE, 100));
    }

    @Test
    void rejectsOpeningAndFinishAfterThePressureWindowExpires() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);
        recorder.recordPressure(PlayerSide.ONE, 10);

        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 101).isEmpty());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 101).isEmpty());
    }

    @Test
    void keepsBothPlayersSequencesIndependent() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);
        recorder.recordPressure(PlayerSide.ONE, 10);

        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.TWO, 20).isEmpty());
        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 20).isPresent());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.TWO, 30).isEmpty());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 30).isPresent());
    }

    @Test
    void rejectsOutOfOrderAndDuplicateEvents() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);

        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 10).isEmpty());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 10).isEmpty());

        recorder.recordPressure(PlayerSide.ONE, 10);
        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 20).isPresent());
        assertTrue(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 21).isEmpty());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 30).isPresent());
        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 31).isEmpty());
    }

    @Test
    void resetRemovesIncompleteSequencesAndNegativeFramesAreInvalid() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);
        recorder.recordPressure(PlayerSide.ONE, 10);
        recorder.reset();

        assertFalse(recorder.recordLongbowChargeStarted(PlayerSide.ONE, 20).isPresent());
        assertThrows(IllegalArgumentException.class, () -> recorder.recordPressure(PlayerSide.ONE, -1));
    }

    @Test
    void openingRemainsActiveOnlyUntilTheOriginalPressureWindowExpires() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);
        recorder.recordPressure(PlayerSide.ONE, 10);
        recorder.recordLongbowChargeStarted(PlayerSide.ONE, 20);

        assertTrue(recorder.hasActiveOpening(PlayerSide.ONE, 100));
        assertFalse(recorder.hasActiveOpening(PlayerSide.ONE, 101));
    }

    @Test
    void cancelledChargeRequiresAFreshOpeningBeforeItCanFinish() {
        TacticalEventRecorder recorder = new TacticalEventRecorder(90);
        recorder.recordPressure(PlayerSide.ONE, 10);
        recorder.recordLongbowChargeStarted(PlayerSide.ONE, 20);

        recorder.recordLongbowChargeCancelled(PlayerSide.ONE, 30);

        assertTrue(recorder.recordLongbowFinish(PlayerSide.ONE, 31).isEmpty());
        assertEquals(
                Optional.of(new TacticalEvent(PlayerSide.ONE, TacticalEventType.OPENING, 32)),
                recorder.recordLongbowChargeStarted(PlayerSide.ONE, 32));
        assertEquals(
                Optional.of(new TacticalEvent(PlayerSide.ONE, TacticalEventType.FINISH, 40)),
                recorder.recordLongbowFinish(PlayerSide.ONE, 40));
    }

    @Test
    void interceptionIsTheOnlyNeutralTacticalFact() {
        assertEquals(
                new TacticalEvent(null, TacticalEventType.INTERCEPT, 5),
                TacticalEvent.intercept(5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TacticalEvent(null, TacticalEventType.PRESSURE, 5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TacticalEvent(PlayerSide.ONE, TacticalEventType.INTERCEPT, 5));
        assertThrows(IllegalArgumentException.class, () -> TacticalEvent.intercept(-1));
    }
}
