package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;

public abstract class GameSystemState {

    protected final App app;
    protected int properFrameCount;

    public GameSystemState(App app) {
        this.app = app;
    }

    public void run(GameSystem system) {
        runSystem(system);

        app.translate(INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F);
        displayMessage(system);

        checkStateTransition(system);

        properFrameCount++;
    }

    public abstract void runSystem(GameSystem system);

    public abstract void displayMessage(GameSystem system);

    public abstract void checkStateTransition(GameSystem system);

}
