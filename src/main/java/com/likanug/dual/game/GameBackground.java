package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;

import java.util.ArrayList;

public class GameBackground {

    private final App app;
    private final ArrayList<BackgroundLine> lineList = new ArrayList<>();
    private final float maxAccelerationMagnitude;
    private final float lineColor;

    GameBackground(float col, float maxAcc, App app) {
        this.app = app;
        lineColor = col;
        maxAccelerationMagnitude = maxAcc;
        for (int i = 0; i < GameConstants.ARENA_GRID_LINES_PER_AXIS; i++) {
            lineList.add(new HorizontalLine(app));
        }
        for (int i = 0; i < GameConstants.ARENA_GRID_LINES_PER_AXIS; i++) {
            lineList.add(new VerticalLine(app));
        }
    }

    void update() {
        for (BackgroundLine eachLine : lineList) {
            eachLine.update(app.random(-maxAccelerationMagnitude, maxAccelerationMagnitude));
        }
    }

    void display() {
        app.stroke(lineColor);
        for (BackgroundLine eachLine : lineList) {
            eachLine.display();
        }
    }
}
