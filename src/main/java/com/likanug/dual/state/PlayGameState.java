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
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;

import java.util.List;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.HALF_PI;
import static processing.core.PConstants.CENTER;

public class PlayGameState extends GameSystemState {

    static final float SHORTBOW_HUD_X = 72.0F;
    static final float SHORTBOW_HUD_Y = INTERNAL_CANVAS_HEIGHT - 42.0F;
    private static final float SHORTBOW_HUD_DOT_SIZE = 16.0F;
    private static final float SHORTBOW_HUD_DOT_GAP = 24.0F;
    private static final int TACTICAL_FEEDBACK_DURATION_FRAMES = (int) (FPS * 0.75F);
    private TacticalEvent tacticalFeedbackEvent;
    private int tacticalFeedbackStartFrame;

    public PlayGameState(App app) {
        super(app);
    }

    public void runSystem(GameSystem system) {
        system.advanceCombatFrame();
        system.getMyGroup().update();
        system.getMyGroup().act();
        system.getOtherGroup().update();
        system.getOtherGroup().act();
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
        system.getMyGroup().displayArrows();
        system.getOtherGroup().displayArrows();

        checkCollision(system);

        system.getCommonParticleSet().update();
        system.getCommonParticleSet().display();
    }

    public void displayMessage(GameSystem system) {
        displayShortbowAmmo(system);
        displayTacticalFeedback(system);

        int messageDurationFrameCount = FPS;
        if (properFrameCount >= messageDurationFrameCount) return;
        app.fill(0, (float) (255.0 * (1.0 - (float) properFrameCount / messageDurationFrameCount)));
        app.text("Go", INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F);
    }

    /** Draws the local player's current shortbow reserve outside the shake transform for reliable combat feedback. */
    private void displayShortbowAmmo(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) return;

        final PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        final int availableAmmo = player.getShortbowAmmo().getAvailableAmmo();
        final int maximumAmmo = player.getShortbowAmmo().getMaximumAmmo();

        app.pushStyle();
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 16);
        app.fill(0, 176);
        app.text(shortbowAmmoDisplayLabel(availableAmmo, maximumAmmo), SHORTBOW_HUD_X, SHORTBOW_HUD_Y - 22.0F);
        app.stroke(0, 176);
        for (int index = 0; index < maximumAmmo; index++) {
            if (index < availableAmmo) app.fill(255);
            else app.fill(0, 48);
            app.ellipse(
                    SHORTBOW_HUD_X + (index - (maximumAmmo - 1) * 0.5F) * SHORTBOW_HUD_DOT_GAP,
                    SHORTBOW_HUD_Y,
                    SHORTBOW_HUD_DOT_SIZE,
                    SHORTBOW_HUD_DOT_SIZE);
        }
        app.popStyle();
    }

    /** Produces the textual reserve value so the same numbers remain available beyond the dot indicator. */
    static String shortbowAmmoDisplayLabel(int availableAmmo, int maximumAmmo) {
        return "Shortbow " + availableAmmo + " / " + maximumAmmo;
    }

    /** Consumes new combat facts and shows the most recent tactical state without moving with screen shake. */
    private void displayTacticalFeedback(GameSystem system) {
        List<TacticalEvent> newEvents = system.drainTacticalEvents();
        if (!newEvents.isEmpty()) {
            tacticalFeedbackEvent = newEvents.getLast();
            tacticalFeedbackStartFrame = system.getCombatFrameCount();
        }
        if (tacticalFeedbackEvent == null) return;

        int elapsedFrames = system.getCombatFrameCount() - tacticalFeedbackStartFrame;
        if (elapsedFrames >= TACTICAL_FEEDBACK_DURATION_FRAMES) {
            tacticalFeedbackEvent = null;
            return;
        }

        app.pushStyle();
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 24);
        int alpha = (int) (255.0F * (1.0F - (float) elapsedFrames / TACTICAL_FEEDBACK_DURATION_FRAMES));
        switch (tacticalFeedbackEvent.type()) {
            case PRESSURE -> app.fill(232, 192, 96, alpha);
            case OPENING -> app.fill(64, 176, 128, alpha);
            case FINISH -> app.fill(192, 64, 64, alpha);
        }
        app.text(
                tacticalFeedbackLabel(tacticalFeedbackEvent.attacker(), tacticalFeedbackEvent.type()),
                INTERNAL_CANVAS_WIDTH * 0.5F,
                52.0F);
        app.popStyle();
    }

    /** Formats the compact state label displayed for the two-player tactical sequence. */
    static String tacticalFeedbackLabel(PlayerSide attacker, TacticalEventType type) {
        String playerLabel = attacker == PlayerSide.ONE ? "P1" : "P2";
        return playerLabel + " " + type;
    }

    public void checkStateTransition(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) {
            system.setCurrentState(new GameResultState(app, "You lose."));
        } else if (system.getOtherGroup().getPlayer().isNull()) {
            system.setCurrentState(new GameResultState(app, "You win."));
        }
    }

    /** Retains the original state-owned entry point while production calls pass the active system explicitly. */
    public void checkCollision() {
        checkCollision(app.getSystem());
    }

    public void checkCollision(GameSystem system) {
        final ActorGroup myGroup = system.getMyGroup();
        final ActorGroup otherGroup = system.getOtherGroup();

        for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (eachMyArrow.isNotCollided(eachEnemyArrow)) continue;
                system.addInterceptParticles(
                        collisionMidpoint(eachMyArrow.getxPosition(), eachEnemyArrow.getxPosition()),
                        collisionMidpoint(eachMyArrow.getyPosition(), eachEnemyArrow.getyPosition()));
                breakArrow(eachMyArrow, myGroup);
                breakArrow(eachEnemyArrow, otherGroup);
            }
        }

        if (!otherGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {
                if (myGroup.getRemovingArrowList().contains(eachMyArrow)) continue;

                AbstractPlayerActor enemyPlayer = otherGroup.getPlayer();
                if (eachMyArrow.isNotCollided(enemyPlayer)) continue;

                if (eachMyArrow.isLethal()) {
                    killPlayer(otherGroup.getPlayer());
                    system.recordLongbowFinish(myGroup);
                } else {
                    thrustPlayerActor(eachMyArrow, (PlayerActor) enemyPlayer);
                    system.recordPressure(myGroup);
                }

                breakArrow(eachMyArrow, myGroup);
            }
        }

        if (!myGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (otherGroup.getRemovingArrowList().contains(eachEnemyArrow)) continue;
                if (eachEnemyArrow.isNotCollided(myGroup.getPlayer())) continue;

                if (eachEnemyArrow.isLethal()) {
                    killPlayer(myGroup.getPlayer());
                    system.recordLongbowFinish(otherGroup);
                } else {
                    thrustPlayerActor(eachEnemyArrow, (PlayerActor) myGroup.getPlayer());
                    system.recordPressure(otherGroup);
                }

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

    static float calculateThrustAngle(float relativeAngle, float randomUnit) {
        return relativeAngle + (randomUnit - 0.5f) * HALF_PI;
    }

    /** Calculates the visual interception point halfway between the two arrows that destroyed each other. */
    static float collisionMidpoint(float firstPosition, float secondPosition) {
        return (firstPosition + secondPosition) * 0.5F;
    }

    public void thrustPlayerActor(Actor referenceActor, PlayerActor targetPlayerActor) {
        final float relativeAngle = atan2(targetPlayerActor.getyPosition() - referenceActor.getyPosition(), targetPlayerActor.getxPosition() - referenceActor.getxPosition());
        final float thrustAngle = calculateThrustAngle(relativeAngle, app.getSystem().getGameRandom().nextFloat());
        targetPlayerActor.setxVelocity(cos(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setyVelocity(sin(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setState(app.getSystem().getDamagedState().entryState(targetPlayerActor));
        app.getSystem().setScreenShakeValue(app.getSystem().getScreenShakeValue() + GameConstants.SCREEN_SHAKE_ON_HIT);
    }
}
