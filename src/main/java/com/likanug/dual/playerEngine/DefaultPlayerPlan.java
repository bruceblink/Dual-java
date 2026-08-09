package com.likanug.dual.playerEngine;

import com.likanug.dual.App;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.AbstractInputDevice;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.QUARTER_PI;

public class DefaultPlayerPlan extends PlayerPlan {

    private PlayerPlan movePlan, jabPlan, escapePlan, killPlan;
    private int horizontalMove, verticalMove;
    private boolean shoot;

    public DefaultPlayerPlan(App app) {
        this.app = app;
    }

    public PlayerPlan getMovePlan() {
        return movePlan;
    }

    public void setMovePlan(PlayerPlan movePlan) {
        this.movePlan = movePlan;
    }

    public PlayerPlan getJabPlan() {
        return jabPlan;
    }

    public void setJabPlan(PlayerPlan jabPlan) {
        this.jabPlan = jabPlan;
    }

    public PlayerPlan getEscapePlan() {
        return escapePlan;
    }

    public void setEscapePlan(PlayerPlan escapePlan) {
        this.escapePlan = escapePlan;
    }

    public PlayerPlan getKillPlan() {
        return killPlan;
    }

    public void setKillPlan(PlayerPlan killPlan) {
        this.killPlan = killPlan;
    }

    public int getHorizontalMove() {
        return horizontalMove;
    }

    public void setHorizontalMove(int horizontalMove) {
        this.horizontalMove = horizontalMove;
    }

    public int getVerticalMove() {
        return verticalMove;
    }

    public void setVerticalMove(int verticalMove) {
        this.verticalMove = verticalMove;
    }

    public boolean isShoot() {
        return shoot;
    }

    public void setShoot(boolean shoot) {
        this.shoot = shoot;
    }

    public void execute(PlayerActor player, AbstractInputDevice input) {
        input.operateMoveButton(horizontalMove, verticalMove);
        input.operateLongShotButton(false);
    }

    public PlayerPlan nextPlan(PlayerActor player) {
        final AbstractPlayerActor enemy = player.getGroup().getEnemyGroup().getPlayer();
        final AiDifficulty difficulty = getDifficulty(player);

        // Draw longbow if enemy is damaged
        if (enemy.getState().isDamaged()) {
            if (app.random(1.0F) < difficulty.getKillAttemptProbability()) return killPlan;
        }
        if (shouldStartFakeCharge(enemy.getState().isDamaged(), app.random(1.0F), difficulty)) return killPlan;

        // Avoid the nearest arrow
        AbstractArrowActor nearestArrow = null;
        float tmpMinDistancePow2 = 999999999.0F;
        for (AbstractArrowActor eachArrow : enemy.getGroup().getArrowList()) {
            final float distancePow2 = player.getDistancePow2(eachArrow);
            if (distancePow2 < tmpMinDistancePow2) {
                nearestArrow = eachArrow;
                tmpMinDistancePow2 = distancePow2;
            }
        }
        if (tmpMinDistancePow2 < 40000.0) {
            final float playerAngleInArrowFrame = nearestArrow.getAngle(player);
            float escapeAngle = nearestArrow.getDirectionAngle();
            if (playerAngleInArrowFrame - nearestArrow.getDirectionAngle() > 0.0)
                escapeAngle += QUARTER_PI + app.random(QUARTER_PI);
            else escapeAngle -= QUARTER_PI + app.random(QUARTER_PI);
            final float escapeTargetX = player.getxPosition() + 100 * cos(escapeAngle);
            final float escapeTargetY = player.getyPosition() + 100 * sin(escapeAngle);
            setMoveDirection(player, escapeTargetX, escapeTargetY, 0);
            if (app.random(1.0F) < difficulty.getEvadeProbability()) return movePlan;
            else return jabPlan;
        }

        // Away from enemy
        setMoveDirection(player, enemy);
        if (player.getDistancePow2(enemy) < 100000.0) {
            if (app.random(1.0F) < difficulty.getEvadeProbability()) return movePlan;
            else return jabPlan;
        }

        // If there is nothing special
        if (app.random(1.0F) < difficulty.getIdleMoveProbability()) return movePlan;
        else return jabPlan;
    }

    /** Reads only the AI profile attached to this player; human and network players keep standard defaults. */
    private AiDifficulty getDifficulty(PlayerActor player) {
        if (player.getEngine() instanceof ComputerPlayerEngine computer) return computer.getDifficulty();
        return AiDifficulty.STANDARD;
    }

    /** Allows only profiles that opted in to begin a readable longbow feint against an undamaged target. */
    static boolean shouldStartFakeCharge(boolean enemyDamaged, float randomValue, AiDifficulty difficulty) {
        return !enemyDamaged && randomValue < difficulty.getFakeChargeProbability();
    }

    public void setMoveDirection(PlayerActor player, AbstractPlayerActor enemy) {
        float targetX, targetY;
        if (enemy.getxPosition() > INTERNAL_CANVAS_WIDTH * 0.5F)
            targetX = app.random(0, INTERNAL_CANVAS_WIDTH * 0.5F);
        else targetX = app.random(INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_WIDTH);
        if (enemy.getyPosition() > INTERNAL_CANVAS_HEIGHT * 0.5F)
            targetY = app.random(0, INTERNAL_CANVAS_HEIGHT * 0.5F);
        else targetY = app.random(INTERNAL_CANVAS_HEIGHT * 0.5F, INTERNAL_CANVAS_HEIGHT);
        setMoveDirection(player, targetX, targetY, 100);
    }

    public void setMoveDirection(PlayerActor player, float targetX, float targetY, float allowance) {
        if (targetX > player.getxPosition() + allowance) horizontalMove = 1;
        else if (targetX < player.getxPosition() - allowance) horizontalMove = -1;
        else horizontalMove = 0;

        if (targetY > player.getyPosition() + allowance) verticalMove = 1;
        else if (targetY < player.getyPosition() - allowance) verticalMove = -1;
        else verticalMove = 0;
    }

}
