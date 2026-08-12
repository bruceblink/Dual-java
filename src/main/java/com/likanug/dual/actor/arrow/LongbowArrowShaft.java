package com.likanug.dual.actor.arrow;

import com.likanug.dual.App;

public class LongbowArrowShaft extends LongbowArrowComponent {
    public LongbowArrowShaft(App app) {
        super(app);
    }

    public void display() {
        app.strokeWeight(5);
        app.stroke(0);
        app.fill(0);
        app.pushMatrix();
        app.translate(xPosition, yPosition);
        app.rotate(rotationAngle);
        app.line(-halfLength, 0, halfLength, 0);
        app.popMatrix();
        app.strokeWeight(1);
    }

    @Override
    public boolean isPrimaryProjectileComponent() {
        return false;
    }
}
