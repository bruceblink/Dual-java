package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.actor.player.AbstractPlayerActor;
import com.likanug.dual.actor.player.NullPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.MatchScore;
import com.likanug.dual.game.LethalHitSnapshot;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.CENTER;

public class PlayGameState extends GameSystemState {

    static final float SHORTBOW_HUD_X = 72.0F;
    static final float SHORTBOW_HUD_Y = INTERNAL_CANVAS_HEIGHT - 42.0F;
    static final float PRESSURE_HUD_Y = SHORTBOW_HUD_Y - 54.0F;
    static final float LONGBOW_HUD_Y = PRESSURE_HUD_Y - 42.0F;
    static final float OPPONENT_SHORTBOW_HUD_X = INTERNAL_CANVAS_WIDTH - SHORTBOW_HUD_X;
    static final float OPPONENT_SHORTBOW_HUD_Y = 94.0F;
    static final float OPPONENT_PRESSURE_HUD_Y = 40.0F;
    static final float OPPONENT_LONGBOW_HUD_Y = OPPONENT_SHORTBOW_HUD_Y + 48.0F;
    static final float MATCH_SCORE_HUD_Y = 24.0F;
    static final float TACTICAL_FEEDBACK_Y = 64.0F;
    static final int HUD_PRIMARY_COLOR = 240;
    static final int HUD_MUTED_COLOR = 224;
    static final int HUD_DARK_COLOR = 16;
    static final int HUD_PANEL_ALPHA = 176;
    static final float MATCH_SCORE_PANEL_WIDTH = 384.0F;
    static final float MATCH_SCORE_PANEL_HEIGHT = 32.0F;
    static final float SHORTBOW_HUD_PANEL_WIDTH = 128.0F;
    static final float SHORTBOW_HUD_PANEL_HEIGHT = 66.0F;
    static final float ACTIVE_STATUS_PANEL_WIDTH = 128.0F;
    static final float ACTIVE_STATUS_PANEL_HEIGHT = 34.0F;
    private static final float SHORTBOW_HUD_DOT_SIZE = 16.0F;
    private static final float SHORTBOW_HUD_DOT_GAP = 24.0F;
    private static final float SHORTBOW_RECOVERY_BAR_WIDTH = 72.0F;
    private static final float SHORTBOW_RECOVERY_BAR_HEIGHT = 4.0F;
    private static final float LONGBOW_CHARGE_BAR_WIDTH = 104.0F;
    private static final float LONGBOW_CHARGE_BAR_HEIGHT = 6.0F;
    private static final int TACTICAL_FEEDBACK_DURATION_FRAMES = (int) (FPS * 0.75F);
    private TacticalEvent tacticalFeedbackEvent;
    private int tacticalFeedbackStartFrame;
    private LethalHitSnapshot pendingLethalHit;

    public PlayGameState(App app) {
        super(app);
    }

    public void runSystem(GameSystem system) {
        if (!beginActiveCombatFrame(system)) {
            displayFrozenCombatFrame(system);
            return;
        }

        system.getMyGroup().update();
        system.getOtherGroup().update();
        system.resolveArenaCollisions();
        system.getMyGroup().actPlayer();
        system.getOtherGroup().actPlayer();
        resolveArrowInterceptions(system);
        system.getMyGroup().actArrows();
        system.getOtherGroup().actArrows();
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
        system.getMyGroup().displayArrows();
        system.getOtherGroup().displayArrows();

        resolvePlayerHits(system);

        system.getCommonParticleSet().update();
        system.getCommonParticleSet().display();
    }

    /** Consumes hit-stop before advancing the deterministic clock, returning whether simulation may run. */
    static boolean beginActiveCombatFrame(GameSystem system) {
        if (system.consumeCombatPauseFrame()) return false;
        system.advanceCombatFrame();
        return true;
    }

    /** Keeps the collision scene and its particles visible while simulation waits for the hit-stop window. */
    private void displayFrozenCombatFrame(GameSystem system) {
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();
        system.getMyGroup().displayArrows();
        system.getOtherGroup().displayArrows();
        system.getCommonParticleSet().display();
    }

    public void displayMessage(GameSystem system) {
        displayMatchScore(system);
        displayPressureStatus(system);
        displayLongbowCharge(system);
        displayShortbowAmmo(system);
        displayLocalOpponentHud(system);
        displayTacticalFeedback(system);

        int messageDurationFrameCount = FPS;
        if (properFrameCount >= messageDurationFrameCount) return;
        app.fill(
                HUD_PRIMARY_COLOR,
                (float) (255.0 * (1.0 - (float) properFrameCount / messageDurationFrameCount)));
        app.text("Go", INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F);
    }

    /** Shows the local player's active pressure refresh count so repeated shortbow hits remain legible. */
    private void displayPressureStatus(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) return;
        displayPressureStatus(
                (PlayerActor) system.getMyGroup().getPlayer(),
                SHORTBOW_HUD_X,
                PRESSURE_HUD_Y,
                null);
    }

    /** Shows the second local player's bounded pressure count in a stable top-of-arena HUD. */
    private void displayLocalOpponentHud(GameSystem system) {
        if (!system.isLocalTwoPlayer() || system.getOtherGroup().getPlayer().isNull()) return;
        final PlayerActor player = (PlayerActor) system.getOtherGroup().getPlayer();
        displayPressureStatus(player, OPPONENT_SHORTBOW_HUD_X, OPPONENT_PRESSURE_HUD_Y, "P2");
        displayLongbowChargeHud(
                system,
                player,
                OPPONENT_SHORTBOW_HUD_X,
                OPPONENT_LONGBOW_HUD_Y,
                "P2 Longbow");
        displayShortbowAmmoHud(player, OPPONENT_SHORTBOW_HUD_X, OPPONENT_SHORTBOW_HUD_Y, "P2 Shortbow");
    }

    /** Shows the local player's longbow commitment in the stable HUD layer while a charge is active. */
    private void displayLongbowCharge(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) return;
        displayLongbowChargeHud(
                system,
                (PlayerActor) system.getMyGroup().getPlayer(),
                SHORTBOW_HUD_X,
                LONGBOW_HUD_Y,
                "Longbow");
    }

    /** Draws one compact charge bar without changing weapon timing or consuming tactical events. */
    private void displayLongbowChargeHud(
            GameSystem system,
            PlayerActor player,
            float x,
            float y,
            String ownerLabel) {
        if (!player.getState().isDrawingLongBow()) return;

        final int requiredFrames = Math.round(GameConstants.LONGBOW_CHARGE_SEC * FPS);
        final float progressRatio = DrawLongbowPlayerActorState.calculateChargeProgress(
                player.getChargedFrameCount(), requiredFrames);
        final boolean ready = player.getState().hasCompletedLongBowCharge(player);
        final boolean tacticalOpening = system.hasTacticalOpening(player);
        final int activeColor = ready
                ? app.color(192, 64, 64)
                : tacticalOpening ? app.color(232, 192, 96) : app.color(HUD_PRIMARY_COLOR, 224);

        app.pushStyle();
        displayHudBackdrop(x, y - 4.0F, ACTIVE_STATUS_PANEL_WIDTH, ACTIVE_STATUS_PANEL_HEIGHT);
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 14);
        app.fill(activeColor);
        app.text(longbowChargeDisplayLabel(ownerLabel, progressRatio, ready), x, y - 13.0F);
        app.noStroke();
        app.fill(HUD_DARK_COLOR, 112);
        app.rect(x, y + 4.0F, LONGBOW_CHARGE_BAR_WIDTH, LONGBOW_CHARGE_BAR_HEIGHT);
        app.fill(activeColor);
        app.rect(
                x - LONGBOW_CHARGE_BAR_WIDTH * 0.5F
                        + LONGBOW_CHARGE_BAR_WIDTH * progressRatio * 0.5F,
                y + 4.0F,
                LONGBOW_CHARGE_BAR_WIDTH * progressRatio,
                LONGBOW_CHARGE_BAR_HEIGHT);
        app.popStyle();
    }

    /** Formats a clamped percentage until the fully charged state becomes ready to release. */
    static String longbowChargeDisplayLabel(String ownerLabel, float progressRatio, boolean ready) {
        if (ready) return ownerLabel + " READY";
        final int percentage = Math.round(Math.max(0.0F, Math.min(1.0F, progressRatio)) * 100.0F);
        return ownerLabel + " " + percentage + "%";
    }

    private void displayPressureStatus(PlayerActor player, float x, float y, String ownerLabel) {
        final int pressureCount = player.getShortbowPressure().getConsecutiveRefreshes();
        if (player.getDamageRemainingFrameCount() <= 0 || pressureCount <= 0) return;

        app.pushStyle();
        displayHudBackdrop(x, y, ACTIVE_STATUS_PANEL_WIDTH, ACTIVE_STATUS_PANEL_HEIGHT);
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 14);
        app.fill(HUD_PRIMARY_COLOR, 224);
        app.text(
                ownerLabel == null
                        ? pressureStatusLabel(pressureCount, player.getShortbowPressure().getMaximumConsecutiveRefreshes())
                        : pressureStatusLabel(ownerLabel, pressureCount, player.getShortbowPressure().getMaximumConsecutiveRefreshes()),
                x,
                y);
        app.popStyle();
    }

    /** Formats the bounded pressure count for the same compact HUD surface as shortbow ammo. */
    static String pressureStatusLabel(int pressureCount, int maximumPressureCount) {
        return "Under pressure " + pressureCount + " / " + maximumPressureCount;
    }

    static String pressureStatusLabel(String ownerLabel, int pressureCount, int maximumPressureCount) {
        return ownerLabel + " pressure " + pressureCount + " / " + maximumPressureCount;
    }

    /** Draws the current round and first-to target in the stable HUD layer outside screen shake. */
    private void displayMatchScore(GameSystem system) {
        app.pushStyle();
        displayHudBackdrop(
                INTERNAL_CANVAS_WIDTH * 0.5F,
                MATCH_SCORE_HUD_Y,
                MATCH_SCORE_PANEL_WIDTH,
                MATCH_SCORE_PANEL_HEIGHT);
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 18);
        app.fill(HUD_PRIMARY_COLOR, 224);
        app.text(matchScoreDisplayLabel(system.getMatchScore()), INTERNAL_CANVAS_WIDTH * 0.5F, MATCH_SCORE_HUD_Y);
        app.popStyle();
    }

    /** Formats one compact scoreboard label shared by rendering and UI tests. */
    static String matchScoreDisplayLabel(MatchScore score) {
        int roundNumber = score.getPlayerOneWins() + score.getPlayerTwoWins() + 1;
        return "Round " + roundNumber + " | YOU " + score.getPlayerOneWins()
                + " - " + score.getPlayerTwoWins() + " RIVAL | First to " + score.getRoundsToWin();
    }

    /** Draws the local player's current shortbow reserve outside the shake transform for reliable combat feedback. */
    private void displayShortbowAmmo(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) return;
        displayShortbowAmmoHud((PlayerActor) system.getMyGroup().getPlayer(), SHORTBOW_HUD_X, SHORTBOW_HUD_Y, "Shortbow");
    }

    private void displayShortbowAmmoHud(PlayerActor player, float x, float y, String label) {
        final int availableAmmo = player.getShortbowAmmo().getAvailableAmmo();
        final int maximumAmmo = player.getShortbowAmmo().getMaximumAmmo();

        app.pushStyle();
        displayHudBackdrop(x, y - 4.0F, SHORTBOW_HUD_PANEL_WIDTH, SHORTBOW_HUD_PANEL_HEIGHT);
        app.textAlign(CENTER, CENTER);
        app.textFont(App.smallFont, 16);
        app.fill(HUD_PRIMARY_COLOR, 224);
        app.text(shortbowAmmoDisplayLabel(label, availableAmmo, maximumAmmo), x, y - 22.0F);
        app.stroke(HUD_MUTED_COLOR, 224);
        for (int index = 0; index < maximumAmmo; index++) {
            if (index < availableAmmo) app.fill(255);
            else app.fill(HUD_DARK_COLOR, 112);
            app.ellipse(
                    x + (index - (maximumAmmo - 1) * 0.5F) * SHORTBOW_HUD_DOT_GAP,
                    y,
                    SHORTBOW_HUD_DOT_SIZE,
                    SHORTBOW_HUD_DOT_SIZE);
        }
        displayShortbowRecoveryBar(x, y, player.getShortbowAmmo().getRecoveryProgressRatio());
        app.popStyle();
    }

    /** Places a compact dark field behind small combat text without moving or covering the playfield center. */
    private void displayHudBackdrop(float x, float y, float width, float height) {
        app.noStroke();
        app.fill(HUD_DARK_COLOR, HUD_PANEL_ALPHA);
        app.rectMode(CENTER);
        app.rect(x, y, width, height);
    }

    /** Shows the next-arrow recovery fraction directly below the reserve dots without covering the arena. */
    private void displayShortbowRecoveryBar(float x, float y, float progressRatio) {
        if (progressRatio <= 0.0F) return;
        final float clampedProgress = Math.max(0.0F, Math.min(1.0F, progressRatio));
        final float barY = y + 16.0F;
        app.noStroke();
        app.fill(HUD_DARK_COLOR, 112);
        app.rect(x, barY, SHORTBOW_RECOVERY_BAR_WIDTH, SHORTBOW_RECOVERY_BAR_HEIGHT);
        app.fill(255, 224);
        app.rect(
                x - SHORTBOW_RECOVERY_BAR_WIDTH * 0.5F
                        + SHORTBOW_RECOVERY_BAR_WIDTH * clampedProgress * 0.5F,
                barY,
                SHORTBOW_RECOVERY_BAR_WIDTH * clampedProgress,
                SHORTBOW_RECOVERY_BAR_HEIGHT);
    }

    /** Produces the textual reserve value so the same numbers remain available beyond the dot indicator. */
    static String shortbowAmmoDisplayLabel(int availableAmmo, int maximumAmmo) {
        return shortbowAmmoDisplayLabel("Shortbow", availableAmmo, maximumAmmo);
    }

    static String shortbowAmmoDisplayLabel(String ownerLabel, int availableAmmo, int maximumAmmo) {
        return ownerLabel + " " + availableAmmo + " / " + maximumAmmo;
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
            case DISRUPT -> app.fill(232, 192, 96, alpha);
            case FINISH -> app.fill(192, 64, 64, alpha);
            case INTERCEPT -> app.fill(96, 208, 232, alpha);
        }
        app.text(
                tacticalFeedbackLabel(tacticalFeedbackEvent.attacker(), tacticalFeedbackEvent.type()),
                INTERNAL_CANVAS_WIDTH * 0.5F,
                TACTICAL_FEEDBACK_Y);
        app.popStyle();
    }

    /** Formats the compact state label displayed for the two-player tactical sequence. */
    static String tacticalFeedbackLabel(PlayerSide attacker, TacticalEventType type) {
        if (type == TacticalEventType.INTERCEPT) return "ARROWS: INTERCEPT";
        String playerLabel = attacker == PlayerSide.ONE ? "YOU" : "RIVAL";
        if (type == TacticalEventType.DISRUPT) return playerLabel + ": CHARGE BREAK";
        return playerLabel + ": " + type;
    }

    public void checkStateTransition(GameSystem system) {
        if (system.getMyGroup().getPlayer().isNull()) {
            transitionToRoundResult(system, "You lose.", PlayerSide.TWO);
        } else if (system.getOtherGroup().getPlayer().isNull()) {
            transitionToRoundResult(system, "You win.", PlayerSide.ONE);
        }
    }

    /** Chooses the readable lethal freeze for real arrow hits while preserving legacy direct-result tests. */
    private void transitionToRoundResult(GameSystem system, String message, PlayerSide winner) {
        final TacticalEvent finish = getFinishFeedback();
        final MatchScore.RoundResult roundResult = system.recordRoundWin(winner);
        if (pendingLethalHit == null) {
            system.setCurrentState(new GameResultState(app, message, finish, roundResult));
            return;
        }
        system.setCurrentState(new LethalHitState(
                app, message, finish, roundResult, pendingLethalHit));
    }

    /** Passes only the completed tactical sequence into the next state, not transient pressure or opening notices. */
    private TacticalEvent getFinishFeedback() {
        if (tacticalFeedbackEvent == null || tacticalFeedbackEvent.type() != TacticalEventType.FINISH) return null;
        return tacticalFeedbackEvent;
    }

    /** Retains the original state-owned entry point while production calls pass the active system explicitly. */
    public void checkCollision() {
        checkCollision(app.getSystem());
    }

    public void checkCollision(GameSystem system) {
        resolveArrowInterceptions(system);
        resolvePlayerHits(system);
    }

    /** Resolves this frame's arrow pairs from earliest contact to latest, independent of list order. */
    private void resolveArrowInterceptions(GameSystem system) {
        final ActorGroup myGroup = system.getMyGroup();
        final ActorGroup otherGroup = system.getOtherGroup();
        final List<InterceptionCandidate> candidates = new ArrayList<>();

        for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {
            if (myGroup.getRemovingArrowList().contains(eachMyArrow)) continue;
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (otherGroup.getRemovingArrowList().contains(eachEnemyArrow)) continue;
                final Optional<AbstractArrowActor.ArrowCollision> collision =
                        eachMyArrow.findCollision(eachEnemyArrow);
                collision.ifPresent(impact -> candidates.add(
                        new InterceptionCandidate(eachMyArrow, eachEnemyArrow, impact)));
            }
        }

        candidates.sort(Comparator.comparingDouble(candidate -> candidate.impact().timeRatio()));
        for (InterceptionCandidate candidate : candidates) {
            if (myGroup.getRemovingArrowList().contains(candidate.myArrow())
                    || otherGroup.getRemovingArrowList().contains(candidate.enemyArrow())) continue;
            final AbstractArrowActor.ArrowCollision impact = candidate.impact();
            system.recordInterception();
            system.addInterceptParticles(impact.impactX(), impact.impactY());
            system.startCombatPause(GameConstants.INTERCEPT_HIT_STOP_FRAMES);
            breakArrowAt(candidate.myArrow(), myGroup, impact.impactX(), impact.impactY());
            breakArrowAt(candidate.enemyArrow(), otherGroup, impact.impactX(), impact.impactY());
        }
    }

    /** Applies surviving arrows to players after interception visuals have hidden destroyed projectiles. */
    private void resolvePlayerHits(GameSystem system) {
        final ActorGroup myGroup = system.getMyGroup();
        final ActorGroup otherGroup = system.getOtherGroup();

        if (!otherGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachMyArrow : myGroup.getArrowList()) {
                if (myGroup.getRemovingArrowList().contains(eachMyArrow)) continue;

                AbstractPlayerActor enemyPlayer = otherGroup.getPlayer();
                if (eachMyArrow.isNotCollided(enemyPlayer)) continue;

                if (eachMyArrow.isLethal()) {
                    pendingLethalHit = captureLethalHit(system, eachMyArrow, myGroup, (PlayerActor) enemyPlayer);
                    system.recordLongbowHit(myGroup);
                    killPlayer(otherGroup.getPlayer());
                    system.recordLongbowFinish(myGroup);
                } else {
                    resolveShortbowHit(system, eachMyArrow, myGroup, (PlayerActor) enemyPlayer);
                }

                breakArrow(eachMyArrow, myGroup);
            }
        }

        if (!myGroup.getPlayer().isNull()) {
            for (AbstractArrowActor eachEnemyArrow : otherGroup.getArrowList()) {
                if (otherGroup.getRemovingArrowList().contains(eachEnemyArrow)) continue;
                if (eachEnemyArrow.isNotCollided(myGroup.getPlayer())) continue;

                if (eachEnemyArrow.isLethal()) {
                    pendingLethalHit = captureLethalHit(
                            system, eachEnemyArrow, otherGroup, (PlayerActor) myGroup.getPlayer());
                    system.recordLongbowHit(otherGroup);
                    killPlayer(myGroup.getPlayer());
                    system.recordLongbowFinish(otherGroup);
                } else {
                    resolveShortbowHit(
                            system, eachEnemyArrow, otherGroup, (PlayerActor) myGroup.getPlayer());
                }

                breakArrow(eachEnemyArrow, otherGroup);
            }
        }
    }

    private record InterceptionCandidate(
            AbstractArrowActor myArrow,
            AbstractArrowActor enemyArrow,
            AbstractArrowActor.ArrowCollision impact) {
    }

    /** Applies one shortbow hit and adds extra feedback only when it interrupts an active longbow charge. */
    private void resolveShortbowHit(
            GameSystem system,
            AbstractArrowActor arrow,
            ActorGroup attackerGroup,
            PlayerActor target) {
        final boolean chargeInterrupted = target.getState() != null && target.getState().isDrawingLongBow();
        final float targetX = target.getxPosition();
        final float targetY = target.getyPosition();
        thrustPlayerActor(arrow, target);
        system.recordPressure(attackerGroup);
        if (!chargeInterrupted) return;

        system.recordLongbowDisruption(attackerGroup);
        system.addDisruptionParticles(targetX, targetY);
        system.startCombatPause(GameConstants.DISRUPT_HIT_STOP_FRAMES);
    }

    /** Captures launch, collision, and target geometry before the killed actor is replaced by NullPlayerActor. */
    private LethalHitSnapshot captureLethalHit(
            GameSystem system,
            AbstractArrowActor arrow,
            ActorGroup attackerGroup,
            PlayerActor target) {
        final PlayerSide attacker = attackerGroup == system.getMyGroup() ? PlayerSide.ONE : PlayerSide.TWO;
        final float launchX = arrow.hasLaunchPosition()
                ? arrow.getLaunchX() : attackerGroup.getPlayer().getxPosition();
        final float launchY = arrow.hasLaunchPosition()
                ? arrow.getLaunchY() : attackerGroup.getPlayer().getyPosition();
        return new LethalHitSnapshot(
                attacker,
                launchX,
                launchY,
                arrow.getxPosition(),
                arrow.getyPosition(),
                target.getxPosition(),
                target.getyPosition(),
                target.getFillColor());
    }

    LethalHitSnapshot getPendingLethalHit() {
        return pendingLethalHit;
    }

    public void killPlayer(AbstractPlayerActor player) {
        app.getSystem().addSquareParticles(player.getxPosition(), player.getyPosition(),
                GameConstants.KILL_PARTICLE_COUNT, GameConstants.KILL_PARTICLE_SIZE, 2, 10, 4);
        player.getGroup().setPlayer(new NullPlayerActor(app));
        app.getSystem().setScreenShakeValue(GameConstants.SCREEN_SHAKE_ON_KILL);
    }

    public void breakArrow(AbstractArrowActor arrow, ActorGroup group) {
        breakArrowAt(arrow, group, arrow.getxPosition(), arrow.getyPosition());
    }

    /** Removes an intercepted arrow while anchoring all fragments to the swept collision point. */
    private void breakArrowAt(AbstractArrowActor arrow, ActorGroup group, float impactX, float impactY) {
        app.getSystem().addSquareParticles(impactX, impactY,
                GameConstants.ARROW_BREAK_PARTICLE_COUNT, GameConstants.ARROW_BREAK_PARTICLE_SIZE, 1, 5, 1);
        group.getRemovingArrowList().add(arrow);
    }

    /** Uses the incoming arrow's real travel direction so players can predict every shortbow knockback. */
    static float calculateThrustAngle(AbstractArrowActor arrow) {
        final float speedSquared = arrow.getxVelocity() * arrow.getxVelocity()
                + arrow.getyVelocity() * arrow.getyVelocity();
        if (speedSquared <= 1.0E-6F) {
            return arrow.getDirectionAngle();
        }
        return atan2(arrow.getyVelocity(), arrow.getxVelocity());
    }

    /** Overwrites the target's velocity with the fixed knockback impulse and enters the damaged state. */
    public void thrustPlayerActor(AbstractArrowActor referenceArrow, PlayerActor targetPlayerActor) {
        final float thrustAngle = calculateThrustAngle(referenceArrow);
        targetPlayerActor.setxVelocity(cos(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setyVelocity(sin(thrustAngle) * GameConstants.PLAYER_THRUST_SPEED);
        targetPlayerActor.setState(app.getSystem().getDamagedState().entryState(targetPlayerActor));
        app.getSystem().setScreenShakeValue(app.getSystem().getScreenShakeValue() + GameConstants.SCREEN_SHAKE_ON_HIT);
    }
}
