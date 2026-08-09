package com.likanug.dual.inputDevice;

/** Fans each local keyboard event into two independent input snapshots for future local multiplayer. */
public final class LocalInputRouter {

    private final KeyBindings playerOneBindings;
    private final KeyBindings playerTwoBindings;
    private final KeyInput playerOneInput = new KeyInput();
    private final KeyInput playerTwoInput = new KeyInput();

    public LocalInputRouter(KeyBindings playerOneBindings, KeyBindings playerTwoBindings) {
        if (!playerOneBindings.conflictsWith(playerTwoBindings).isEmpty()) {
            throw new IllegalArgumentException("Local player key bindings cannot overlap.");
        }
        this.playerOneBindings = playerOneBindings;
        this.playerTwoBindings = playerTwoBindings;
    }

    public KeyInput getPlayerOneInput() {
        return playerOneInput;
    }

    public KeyInput getPlayerTwoInput() {
        return playerTwoInput;
    }

    /** Applies one Processing key event to both players without sharing mutable action state. */
    public void handleKey(char character, int keyCode, boolean pressed) {
        playerOneInput.applyKey(playerOneBindings, character, keyCode, pressed);
        playerTwoInput.applyKey(playerTwoBindings, character, keyCode, pressed);
    }

    public void clear() {
        playerOneInput.clear();
        playerTwoInput.clear();
    }
}
