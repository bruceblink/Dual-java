package com.likanug.dual.game;

/** Immutable combat fact emitted by the tactical recorder for UI feedback and future replay data. */
public record TacticalEvent(PlayerSide attacker, TacticalEventType type, int frame) {
}
