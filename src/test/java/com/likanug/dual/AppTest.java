package com.likanug.dual;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
