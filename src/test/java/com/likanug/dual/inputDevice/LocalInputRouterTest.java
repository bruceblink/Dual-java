package com.likanug.dual.inputDevice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static processing.core.PConstants.UP;

class LocalInputRouterTest {

    @Test
    void defaultBindingsKeepPlayersIndependent() {
        LocalInputRouter router = new LocalInputRouter(KeyBindings.playerOne(), KeyBindings.playerTwo());

        router.handleKey('w', 0, true);
        assertTrue(router.getPlayerOneInput().isWPressed);
        assertFalse(router.getPlayerTwoInput().isWPressed);

        router.handleKey('i', 0, true);
        assertTrue(router.getPlayerTwoInput().isWPressed);
        assertTrue(router.getPlayerOneInput().isWPressed);
    }

    @Test
    void releasingCharacterBindingDoesNotReleaseIndependentArrowBinding() {
        LocalInputRouter router = new LocalInputRouter(KeyBindings.playerOne(), KeyBindings.playerTwo());

        router.handleKey('w', 0, true);
        router.handleKey((char) 0xffff, UP, true);
        router.handleKey('w', 0, false);

        assertFalse(router.getPlayerOneInput().isWPressed);
        assertTrue(router.getPlayerOneInput().isUpPressed);

        router.handleKey((char) 0xffff, UP, false);
        assertFalse(router.getPlayerOneInput().isUpPressed);
    }

    @Test
    void overlappingBindingsAreRejectedBeforeLocalPlayStarts() {
        KeyBindings conflicting = bindingsWithShortbow('z');

        assertEquals(1, KeyBindings.playerOne().conflictsWith(conflicting).size());
        assertThrows(
                IllegalArgumentException.class,
                () -> new LocalInputRouter(KeyBindings.playerOne(), conflicting));
    }

    @Test
    void clearReleasesBothPlayerSnapshots() {
        LocalInputRouter router = new LocalInputRouter(KeyBindings.playerOne(), KeyBindings.playerTwo());
        router.handleKey('z', 0, true);
        router.handleKey('b', 0, true);

        router.clear();

        assertFalse(router.getPlayerOneInput().isZPressed);
        assertFalse(router.getPlayerTwoInput().isZPressed);
    }

    private static KeyBindings bindingsWithShortbow(char key) {
        return KeyBindings.builder()
                .bindCharacter(InputAction.UP, '1')
                .bindCharacter(InputAction.DOWN, '2')
                .bindCharacter(InputAction.LEFT, '3')
                .bindCharacter(InputAction.RIGHT, '4')
                .bindCharacter(InputAction.SHORTBOW, key)
                .bindCharacter(InputAction.LONGBOW, '5')
                .build();
    }
}
