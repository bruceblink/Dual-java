package com.likanug.dual.game;

/** Immutable combat fact emitted by the tactical recorder for UI feedback and future replay data. */
public record TacticalEvent(PlayerSide attacker, TacticalEventType type, int frame) {

    /** Keeps player-owned sequence facts distinct from the one neutral interception fact. */
    public TacticalEvent {
        if (frame < 0) throw new IllegalArgumentException("Frame must not be negative.");
        if (type == null) throw new IllegalArgumentException("Event type is required.");
        if ((type == TacticalEventType.INTERCEPT) != (attacker == null)) {
            throw new IllegalArgumentException("Only interception events may omit the attacker.");
        }
    }

    /** Creates the neutral fact used when two arrows destroy each other without an attacker. */
    public static TacticalEvent intercept(int frame) {
        return new TacticalEvent(null, TacticalEventType.INTERCEPT, frame);
    }
}
