package com.likanug.dual.inputDevice;

/** Represents either a character key or a Processing key-code binding. */
public record KeyStroke(char character, int keyCode) {

    private static final int CHARACTER_KEY_CODE = -1;

    public static KeyStroke character(char character) {
        return new KeyStroke(Character.toLowerCase(character), CHARACTER_KEY_CODE);
    }

    public static KeyStroke code(int keyCode) {
        return new KeyStroke('\0', keyCode);
    }

    public boolean matches(char receivedCharacter, int receivedKeyCode) {
        if (keyCode != CHARACTER_KEY_CODE) return keyCode == receivedKeyCode;
        return character == Character.toLowerCase(receivedCharacter);
    }
}
