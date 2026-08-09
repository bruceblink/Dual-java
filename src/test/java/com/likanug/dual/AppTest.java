package com.likanug.dual;

import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.ArenaLayout;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.playerEngine.AiDifficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static processing.core.PConstants.ESC;

public class AppTest {

    private App app;

    @BeforeEach
    void setUp() {
        app = new App();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testAppSize() {
        assertEquals(1920, App.DEFAULT_WINDOW_WIDTH);
        assertEquals(1080, App.DEFAULT_WINDOW_HEIGHT);
        assertEquals(1280, App.INTERNAL_CANVAS_WIDTH);
        assertEquals(720, App.INTERNAL_CANVAS_HEIGHT);
        assertEquals(60, App.FPS);
    }

    @Test
    void focusLossBeforeSetupDoesNotCrashTheWindow() {
        assertDoesNotThrow(app::focusLost);
    }

    @Test
    void fullHdWindowCentersAndScalesThePlayfield() {
        app.width = App.DEFAULT_WINDOW_WIDTH;
        app.height = App.DEFAULT_WINDOW_HEIGHT;

        assertEquals(1.5F, app.canvasScale());
        assertEquals(0.0F, app.canvasOffsetX());
        assertEquals(0.0F, app.canvasOffsetY());
        assertTrue(app.isInsideCanvas(0.0F, 0.0F));
        assertTrue(app.isInsideCanvas(1919.9F, 1079.9F));
        assertFalse(app.isInsideCanvas(1920.0F, 540.0F));

        App.CanvasPoint center = app.toCanvasPoint(960.0F, 540.0F);
        assertEquals(640.0F, center.x());
        assertEquals(360.0F, center.y());
    }

    @Test
    void canvasScalePreservesTheSquarePlayfield() {
        app.width = 1280;
        app.height = 720;

        assertEquals(1.0F, app.canvasScale());
        assertEquals(0.0F, app.canvasOffsetX());
        assertEquals(0.0F, app.canvasOffsetY());
    }

    @Test
    void canvasHitTestingIgnoresLetterboxMargins() {
        app.width = 1280;
        app.height = 720;

        assertTrue(app.isInsideCanvas(0.0F, 0.0F));
        assertTrue(app.isInsideCanvas(1279.9F, 719.9F));
        assertFalse(app.isInsideCanvas(1280.0F, 360.0F));
    }

    @Test
    void screenCoordinatesConvertToTheInternalCanvas() {
        app.width = 1280;
        app.height = 720;

        App.CanvasPoint topLeft = app.toCanvasPoint(0.0F, 0.0F);
        App.CanvasPoint center = app.toCanvasPoint(640.0F, 360.0F);

        assertEquals(0.0F, topLeft.x());
        assertEquals(0.0F, topLeft.y());
        assertEquals(640.0F, center.x());
        assertEquals(360.0F, center.y());
    }

    @Test
    void parsePortAcceptsValidBounds() {
        assertEquals(App.MIN_PORT, App.parsePort("1"));
        assertEquals(App.DEFAULT_PORT, App.parsePort("7777"));
        assertEquals(App.MAX_PORT, App.parsePort("65535"));
    }

    @Test
    void parsePortRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> App.parsePort(""));
        assertThrows(IllegalArgumentException.class, () -> App.parsePort("0"));
        assertThrows(IllegalArgumentException.class, () -> App.parsePort("65536"));
        assertThrows(IllegalArgumentException.class, () -> App.parsePort("abc"));
    }

    @Test
    void localModeMenuStartsLocalTwoPlayerFromTheVisibleSecondOption() {
        app.setCurrentKeyInput(new KeyInput());
        app.setSecondKeyInput(new KeyInput());
        app.openLocalModeMenu();

        app.key = '4';
        app.keyPressed();

        assertFalse(app.isLocalModeMenuVisible());
        assertTrue(app.getSystem().isLocalTwoPlayer());
    }

    @Test
    void localModeMenuStartsTheSelectedBasicAiProfile() {
        app.setCurrentKeyInput(new KeyInput());
        app.setSecondKeyInput(new KeyInput());
        app.openLocalModeMenu();

        app.key = '1';
        app.keyPressed();

        assertFalse(app.isLocalModeMenuVisible());
        assertFalse(app.getSystem().isLocalTwoPlayer());
        assertEquals(AiDifficulty.BASIC, app.getSystem().getAiDifficulty());
    }

    @Test
    void localModeMenuStartsTheOptionalCentralCoverArena() {
        app.setCurrentKeyInput(new KeyInput());
        app.setSecondKeyInput(new KeyInput());
        app.openLocalModeMenu();

        app.key = '5';
        app.keyPressed();

        assertEquals(ArenaLayout.centralCover().getDisplayName(), app.getSystem().getArenaLayout().getDisplayName());
    }

    @Test
    void localModeMenuCanBeCancelledBackToTheDemo() {
        app.setCurrentKeyInput(new KeyInput());
        app.setSecondKeyInput(new KeyInput());
        app.setSystem(new GameSystem(true, false, app));
        app.openLocalModeMenu();

        app.key = ESC;
        app.keyPressed();

        assertFalse(app.isLocalModeMenuVisible());
        assertTrue(app.getSystem().isDemoPlay());
    }

    @Test
    void settingsMenuChangesVolumeAndCanBeClosed() {
        app.setCurrentKeyInput(new KeyInput());
        app.setSecondKeyInput(new KeyInput());
        app.openSettingsMenu();

        assertTrue(app.isSettingsMenuVisible());
        app.key = '-';
        app.keyPressed();
        assertEquals("50%", app.getAudioSettings().displayLabel());

        app.key = 'm';
        app.keyPressed();
        assertEquals("Muted", app.getAudioSettings().displayLabel());

        app.key = ESC;
        app.keyPressed();
        assertFalse(app.isSettingsMenuVisible());
    }

    @Test
    void demoSettingsShortcutOpensMenuWithoutLeakingIntoCombatInput() {
        KeyInput playerOne = new KeyInput();
        KeyInput playerTwo = new KeyInput();
        app.setCurrentKeyInput(playerOne);
        app.setSecondKeyInput(playerTwo);

        app.key = 'o';
        app.keyPressed();
        assertTrue(app.isSettingsMenuVisible());

        app.key = 'w';
        app.keyPressed();
        assertFalse(playerOne.isWPressed);
        assertFalse(playerTwo.isWPressed);
    }

    @Test
    void focusLossClearsBothConfiguredLocalInputSnapshots() {
        KeyInput playerOne = new KeyInput();
        KeyInput playerTwo = new KeyInput();
        playerOne.isWPressed = true;
        playerOne.isXPressed = true;
        playerTwo.isAPressed = true;
        playerTwo.isAimRightPressed = true;
        app.setCurrentKeyInput(playerOne);
        app.setSecondKeyInput(playerTwo);

        app.focusLost();

        assertFalse(playerOne.isWPressed);
        assertFalse(playerOne.isXPressed);
        assertFalse(playerTwo.isAPressed);
        assertFalse(playerTwo.isAimRightPressed);
    }
}
