package com.likanug.dual.inputDevice;

import processing.core.PConstants;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stores immutable keyboard bindings and rejects ambiguous mappings between local players. */
public final class KeyBindings {

    private final EnumMap<InputAction, List<KeyStroke>> bindings;

    private KeyBindings(EnumMap<InputAction, List<KeyStroke>> bindings) {
        this.bindings = bindings;
    }

    /** Keeps the existing WASD, arrow, Z, and X controls as player one's default mapping. */
    public static KeyBindings playerOne() {
        return builder()
                .bindCharacter(InputAction.UP, 'w')
                .bindCharacter(InputAction.DOWN, 's')
                .bindCharacter(InputAction.LEFT, 'a')
                .bindCharacter(InputAction.RIGHT, 'd')
                .bindCode(InputAction.UP, PConstants.UP)
                .bindCode(InputAction.DOWN, PConstants.DOWN)
                .bindCode(InputAction.LEFT, PConstants.LEFT)
                .bindCode(InputAction.RIGHT, PConstants.RIGHT)
                .bindCharacter(InputAction.SHORTBOW, 'z')
                .bindCharacter(InputAction.LONGBOW, 'x')
                .build();
    }

    /** Uses an independent keyboard cluster for a future local second player. */
    public static KeyBindings playerTwo() {
        return builder()
                .bindCharacter(InputAction.UP, 'i')
                .bindCharacter(InputAction.DOWN, 'k')
                .bindCharacter(InputAction.LEFT, 'j')
                .bindCharacter(InputAction.RIGHT, 'l')
                .bindCharacter(InputAction.SHORTBOW, 'b')
                .bindCharacter(InputAction.LONGBOW, 'v')
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean matches(InputAction action, char character, int keyCode) {
        return bindings.get(action).stream().anyMatch(stroke -> stroke.matches(character, keyCode));
    }

    public List<KeyStroke> strokes(InputAction action) {
        return bindings.get(action);
    }

    public Set<KeyStroke> conflictsWith(KeyBindings other) {
        final Set<KeyStroke> conflicts = new LinkedHashSet<>();
        for (List<KeyStroke> ownStrokes : bindings.values()) {
            for (KeyStroke ownStroke : ownStrokes) {
                if (other.containsExactStroke(ownStroke)) conflicts.add(ownStroke);
            }
        }
        return Set.copyOf(conflicts);
    }

    private boolean containsExactStroke(KeyStroke stroke) {
        return bindings.values().stream().anyMatch(strokes -> strokes.contains(stroke));
    }

    public static final class Builder {
        private final EnumMap<InputAction, List<KeyStroke>> bindings = new EnumMap<>(InputAction.class);

        private Builder() {
            for (InputAction action : InputAction.values()) bindings.put(action, new ArrayList<>());
        }

        public Builder bindCharacter(InputAction action, char character) {
            bindings.get(action).add(KeyStroke.character(character));
            return this;
        }

        public Builder bindCode(InputAction action, int keyCode) {
            bindings.get(action).add(KeyStroke.code(keyCode));
            return this;
        }

        public KeyBindings build() {
            final EnumMap<InputAction, List<KeyStroke>> immutableBindings = new EnumMap<>(InputAction.class);
            for (Map.Entry<InputAction, List<KeyStroke>> entry : bindings.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    throw new IllegalStateException("Every input action needs at least one key binding.");
                }
                immutableBindings.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new KeyBindings(immutableBindings);
        }
    }
}
