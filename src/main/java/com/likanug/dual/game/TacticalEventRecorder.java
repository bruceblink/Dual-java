package com.likanug.dual.game;

import java.util.EnumMap;
import java.util.Optional;

/**
 * Records the shortbow-pressure, longbow-opening, and finishing-hit sequence without changing combat rules.
 * Each player owns one pending sequence, which expires after a fixed number of simulation frames.
 */
public final class TacticalEventRecorder {

    private final int openingWindowFrameCount;
    private final EnumMap<PlayerSide, PendingSequence> pendingSequences = new EnumMap<>(PlayerSide.class);

    public TacticalEventRecorder(int openingWindowFrameCount) {
        if (openingWindowFrameCount < 0) {
            throw new IllegalArgumentException("Opening window must not be negative.");
        }
        this.openingWindowFrameCount = openingWindowFrameCount;
    }

    /** Starts or replaces one player's pending sequence after a shortbow hit has applied knockback. */
    public TacticalEvent recordPressure(PlayerSide attacker, int frame) {
        validateFrame(frame);
        pendingSequences.put(attacker, new PendingSequence(frame, false));
        return new TacticalEvent(attacker, TacticalEventType.PRESSURE, frame);
    }

    /** Emits OPENING only when the same player begins a longbow charge inside their active pressure window. */
    public Optional<TacticalEvent> recordLongbowChargeStarted(PlayerSide attacker, int frame) {
        validateFrame(frame);
        PendingSequence sequence = pendingSequences.get(attacker);
        if (sequence == null || sequence.openingRecorded || !isInWindow(sequence.pressureFrame, frame)) {
            return Optional.empty();
        }

        pendingSequences.put(attacker, new PendingSequence(sequence.pressureFrame, true));
        return Optional.of(new TacticalEvent(attacker, TacticalEventType.OPENING, frame));
    }

    /** Emits FINISH only when the same player lands a lethal longbow hit after a valid opening. */
    public Optional<TacticalEvent> recordLongbowFinish(PlayerSide attacker, int frame) {
        validateFrame(frame);
        PendingSequence sequence = pendingSequences.get(attacker);
        if (sequence == null || !sequence.openingRecorded || !isInWindow(sequence.pressureFrame, frame)) {
            return Optional.empty();
        }

        pendingSequences.remove(attacker);
        return Optional.of(new TacticalEvent(attacker, TacticalEventType.FINISH, frame));
    }

    /** Reports whether the player is still in the valid opening created by their earlier shortbow pressure. */
    public boolean hasActiveOpening(PlayerSide attacker, int frame) {
        validateFrame(frame);
        PendingSequence sequence = pendingSequences.get(attacker);
        return sequence != null && sequence.openingRecorded && isInWindow(sequence.pressureFrame, frame);
    }

    /** Clears incomplete tactical sequences when a round or match is reset. */
    public void reset() {
        pendingSequences.clear();
    }

    private boolean isInWindow(int pressureFrame, int currentFrame) {
        return currentFrame >= pressureFrame
                && currentFrame - pressureFrame <= openingWindowFrameCount;
    }

    private void validateFrame(int frame) {
        if (frame < 0) throw new IllegalArgumentException("Frame must not be negative.");
    }

    private record PendingSequence(int pressureFrame, boolean openingRecorded) {
    }
}
