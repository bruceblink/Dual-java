package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

import static processing.core.PApplet.abs;
import static processing.core.PApplet.radians;
import static processing.core.PConstants.*;

public class KillPlayerPlan extends PlayerPlan {

    private PlayerPlan movePlan, escapePlan;

    public PlayerPlan getMovePlan() {
        return movePlan;
    }

    public void setMovePlan(PlayerPlan movePlan) {
        this.movePlan = movePlan;
    }

    public PlayerPlan getEscapePlan() {
        return escapePlan;
    }

    public void setEscapePlan(PlayerPlan escapePlan) {
        this.escapePlan = escapePlan;
    }

    public KillPlayerPlan(App app) {
        this.app = app;
    }

    public void execute(PlayerActor player, AbstractInputDevice input) {
        int horizontalMove;
        final float relativeAngle = player.getAngle(player.getGroup().getEnemyGroup().getPlayer()) - player.getAimAngle();
        if (abs(relativeAngle) < radians(1)) horizontalMove = 0;
        else {
            if ((relativeAngle + TWO_PI) % TWO_PI > PI) horizontalMove = -1;
            else horizontalMove = +1;
        }
        input.operateMoveButton(horizontalMove, 0);

        input.operateShotButton(false);

        final AiDifficulty difficulty = getDifficulty(player);
        final boolean chargeComplete = player.getState().hasCompletedLongBowCharge(player);
        input.operateLongShotButton(!shouldReleaseLongbow(chargeComplete, app.random(1.0F), difficulty));
    }

    public PlayerPlan nextPlan(PlayerActor player) {
        final AbstractPlayerActor enemy = player.getGroup().getEnemyGroup().getPlayer();

        if (abs(player.getAngle(player.getGroup().getEnemyGroup().getPlayer()) - player.getAimAngle()) > QUARTER_PI)
            return movePlan;
        if (player.getDistance(enemy) < 400.0) return movePlan;
        if (!player.getEngine().controllingInputDevice.isLongShotButtonPressed()) return movePlan;

        return this;
    }

    /** Releases only after a complete charge and the selected fair profile's reaction roll succeeds. */
    static boolean shouldReleaseLongbow(boolean chargeComplete, float randomValue, AiDifficulty difficulty) {
        return chargeComplete && randomValue < difficulty.getLongbowReleaseProbability();
    }

    private AiDifficulty getDifficulty(PlayerActor player) {
        if (player.getEngine() instanceof ComputerPlayerEngine computer) return computer.getDifficulty();
        return AiDifficulty.STANDARD;
    }

}
