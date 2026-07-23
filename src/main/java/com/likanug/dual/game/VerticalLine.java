package com.likanug.dual.game;

import com.likanug.dual.App;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;

public class VerticalLine extends BackgroundLine {

    public VerticalLine(App app) {
        super(app, app.random(INTERNAL_CANVAS_WIDTH));
    }

    public void display() {
        app.line(position, 0.0F, position, INTERNAL_CANVAS_HEIGHT);
    }

    public float getMaxPosition() {
        return INTERNAL_CANVAS_WIDTH;
    }
}
