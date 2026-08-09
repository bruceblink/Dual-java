package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.PlayerActor;

import java.util.Objects;

public class ComputerPlayerEngine extends PlayerEngine {

    private final App app;
    private final AiDifficulty difficulty;
    private final int planUpdateFrameCount;
    private PlayerPlan currentPlan;

    public ComputerPlayerEngine(App app) {
        this(app, AiDifficulty.STANDARD);
    }

    public ComputerPlayerEngine(App app, AiDifficulty difficulty) {
        this.app = app;
        this.difficulty = Objects.requireNonNull(difficulty);
        this.planUpdateFrameCount = difficulty.getPlanUpdateFrameCount();
        final MovePlayerPlan move = new MovePlayerPlan(app);
        final JabPlayerPlan jab = new JabPlayerPlan(app);
        final KillPlayerPlan kill = new KillPlayerPlan(app);
        // Plan transition graph: move ↔ jab ↔ kill → move
        move.setMovePlan(move);
        move.setJabPlan(jab);
        move.setKillPlan(kill);
        jab.setMovePlan(move);
        jab.setJabPlan(jab);
        jab.setKillPlan(kill);
        kill.setMovePlan(move);

        currentPlan = move;
    }

    public App getApp() {
        return app;
    }

    public int getPlanUpdateFrameCount() {
        return planUpdateFrameCount;
    }

    public AiDifficulty getDifficulty() {
        return difficulty;
    }

    public PlayerPlan getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(PlayerPlan currentPlan) {
        this.currentPlan = currentPlan;
    }

    public void run(PlayerActor player) {
        currentPlan.execute(player, controllingInputDevice);

        if (app.frameCount % planUpdateFrameCount == 0) currentPlan = currentPlan.nextPlan(player);
    }

}
