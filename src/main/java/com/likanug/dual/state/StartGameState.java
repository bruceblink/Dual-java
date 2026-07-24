package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.particle.Particle;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PConstants.HALF_PI;
import static processing.core.PConstants.TWO_PI;

public class StartGameState extends GameSystemState {

    private final int frameCountPerNumber = FPS;
    private final float ringSize = 200;
    private int displayNumber = 4;

    public StartGameState(App app) {
        super(app);
    }

    public void runSystem(GameSystem system) {
        system.getMyGroup().update();
        system.getOtherGroup().update();
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
    }

    public void displayMessage(GameSystem system) {
        final int currentNumberFrameCount = properFrameCount % frameCountPerNumber;
        if (currentNumberFrameCount == 0) displayNumber--;
        if (displayNumber <= 0) return;

        float ringColor = 0;
        app.fill(ringColor);
        app.text(displayNumber, INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F);

        app.strokeWeight(3);
        app.stroke(ringColor);
        app.noFill();
        app.arc(
                INTERNAL_CANVAS_WIDTH * 0.5F,
                INTERNAL_CANVAS_HEIGHT * 0.5F,
                ringSize,
                ringSize,
                -HALF_PI,
                -HALF_PI + TWO_PI * (properFrameCount % frameCountPerNumber) / frameCountPerNumber
        );
        app.strokeWeight(1);
    }

    public void checkStateTransition(GameSystem system) {
        if (properFrameCount >= frameCountPerNumber * 3) {
            float ringStrokeWeight = 5;
            final Particle newParticle = system.getCommonParticleSet().getBuilder()
                    .type(3)  // Ring
                    .position(INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F)
                    .polarVelocity(0, 0)
                    .particleSize((int) ringSize)
                    .particleColor(0)
                    .weight(ringStrokeWeight)
                    .lifespanSecond(1)
                    .build();
            system.getCommonParticleSet().getParticleList().add(newParticle);

            system.setCurrentState(new PlayGameState(app));
        }
    }

}
