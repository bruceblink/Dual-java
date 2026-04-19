package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.Actor;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.NullPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;

import static com.likanug.dual.App.FPS;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.HALF_PI;

public class PlayGameState extends GameSystemState {

    public PlayGameState(App app) {
        super(app);
    }

    public void runSystem(GameSystem system) {
        system.getMyGroup().update();
        system.getMyGroup().act();
        system.getOtherGroup().update();
        system.getOtherGroup().act();
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
        system.getMyGroup().displayArrows();
        system.getOtherGroup().displayArrows();

        checkCollision();

        system.getCommonParticleSet().update();
        system.getCommonParticleSet().display();
    }

    public void displayMessage(GameSystem system) {
        int messageDurationFrameCount = FPS;
        if (properFrameCount >= messageDurationFrameCount) return;
        app.fill(0, (float) (255.0 * (1.0 - properFrameCount / messageDurationFrameCount)));
        app.text("Go", 0.0F, 0.0F);
    }

    public void checkStateTransition(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) {
            system.setCurrentState(new GameResultState(app, "You lose."));
        } else if (system.getOtherGroup().getPlayer().isNull()) {
            system.setCurrentState(new GameResultState(app, "You win."));
        }
    }

    public void checkCollision() {
        final ActorGroup myGroup = app.getSystem().getMyGroup();
        final ActorGroup otherGroup = app.getSystem().getOtherGroup();

        for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (eachMyArrow.isNotCollided(eachEnemyArrow)) continue;
                breakArrow(eachMyArrow, myGroup);
                breakArrow(eachEnemyArrow, otherGroup);
            }
        }

        if (!otherGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {

                AbstractPlayerActor enemyPlayer = otherGroup.getPlayer();
                if (eachMyArrow.isNotCollided(enemyPlayer)) continue;

                if (eachMyArrow.isLethal()) killPlayer(otherGroup.getPlayer());
                else thrustPlayerActor(eachMyArrow, (PlayerActor) enemyPlayer);

                breakArrow(eachMyArrow, myGroup);
            }
        }

        if (!myGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (eachEnemyArrow.isNotCollided(myGroup.getPlayer())) continue;

                if (eachEnemyArrow.isLethal()) killPlayer(myGroup.getPlayer());
                else thrustPlayerActor(eachEnemyArrow, (PlayerActor) myGroup.getPlayer());

                breakArrow(eachEnemyArrow, otherGroup);
            }
        }
    }

    public void killPlayer(AbstractPlayerActor player) {
        app.getSystem().addSquareParticles(player.getxPosition(), player.getyPosition(),
                GameConstants.KILL_PARTICLE_COUNT, GameConstants.KILL_PARTICLE_SIZE, 2, 10, 4);
        player.getGroup().setPlayer(new NullPlayerActor(app));
        app.getSystem().setScreenShakeValue(GameConstants.SCREEN_SHAKE_ON_KILL);
    }

    public void breakArrow(AbstractArrowActor arrow, ActorGroup group) {
        app.getSystem().addSquareParticles(arrow.getxPosition(), arrow.getyPosition(),
                GameConstants.ARROW_BREAK_PARTICLE_COUNT, GameConstants.ARROW_BREAK_PARTICLE_SIZE, 1, 5, 1);
        group.getRemovingArrowList().add(arrow);
    }

    public void thrustPlayerActor(Actor referenceActor, PlayerActor targetPlayerActor) {
        final float relativeAngle = atan2(targetPlayerActor.getyPosition() - referenceActor.getyPosition(), targetPlayerActor.getxPosition() - referenceActor.getxPosition());
        final float thrustAngle = relativeAngle + app.random((float) (-0.5 * HALF_PI), (float) (0.5 * HALF_PI));
        targetPlayerActor.setxVelocity(cos(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setyVelocity(sin(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setState(app.getSystem().getDamagedState().entryState(targetPlayerActor));
        app.getSystem().setScreenShakeValue(app.getSystem().getScreenShakeValue() + GameConstants.SCREEN_SHAKE_ON_HIT);
    }
}
