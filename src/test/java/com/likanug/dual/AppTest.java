package com.likanug.dual;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(640, App.INTERNAL_CANVAS_SIDE_WIDTH);
        assertEquals(60, App.FPS);
    }

    @Test
    void canvasScalePreservesTheSquarePlayfield() {
        app.width = 1280;
        app.height = 720;

        assertEquals(1.125F, app.canvasScale());
        assertEquals(280.0F, app.canvasOffsetX());
        assertEquals(0.0F, app.canvasOffsetY());
    }

    @Test
    void canvasHitTestingIgnoresLetterboxMargins() {
        app.width = 1280;
        app.height = 720;

        assertFalse(app.isInsideCanvas(279.9F, 360.0F));
        assertTrue(app.isInsideCanvas(280.0F, 360.0F));
        assertTrue(app.isInsideCanvas(999.9F, 719.9F));
        assertFalse(app.isInsideCanvas(1000.0F, 360.0F));
    }

    @Test
    void screenCoordinatesConvertToTheInternalCanvas() {
        app.width = 1280;
        app.height = 720;

        App.CanvasPoint topLeft = app.toCanvasPoint(280.0F, 0.0F);
        App.CanvasPoint center = app.toCanvasPoint(640.0F, 360.0F);

        assertEquals(0.0F, topLeft.x());
        assertEquals(0.0F, topLeft.y());
        assertEquals(320.0F, center.x());
        assertEquals(320.0F, center.y());
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
}
