package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.arrow.ShortbowArrow;
import com.likanug.dual.actor.arrow.LongbowArrowHead;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.MatchScore;
import com.likanug.dual.game.PlayerSide;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static processing.core.PConstants.HALF_PI;
import static processing.core.PConstants.PI;

class PlayGameStateTest {

    @Test
    void shortbowAmmoHudKeepsTheAvailableAndMaximumValuesReadable() {
        assertEquals("Shortbow 3 / 3", PlayGameState.shortbowAmmoDisplayLabel(3, 3));
        assertEquals("Shortbow 0 / 3", PlayGameState.shortbowAmmoDisplayLabel(0, 3));
    }

    @Test
    void pressureHudNamesTheBoundedRefreshCount() {
        assertEquals("Under pressure 1 / 2", PlayGameState.pressureStatusLabel(1, 2));
        assertEquals("P2 pressure 2 / 2", PlayGameState.pressureStatusLabel("P2", 2, 2));
    }

    @Test
    void localOpponentAmmoHudKeepsTheOwnerLabelVisible() {
        assertEquals("P2 Shortbow 1 / 3", PlayGameState.shortbowAmmoDisplayLabel("P2 Shortbow", 1, 3));
    }

    @Test
    void longbowHudClampsChargeProgressAndNamesTheReadyState() {
        assertEquals("Longbow 0%", PlayGameState.longbowChargeDisplayLabel("Longbow", -0.2F, false));
        assertEquals("Longbow 50%", PlayGameState.longbowChargeDisplayLabel("Longbow", 0.5F, false));
        assertEquals("P2 Longbow 100%", PlayGameState.longbowChargeDisplayLabel("P2 Longbow", 1.4F, false));
        assertEquals("Longbow READY", PlayGameState.longbowChargeDisplayLabel("Longbow", 1.0F, true));
    }

    @Test
    void matchScoreHudNamesRoundScoreAndTarget() {
        MatchScore score = new MatchScore(3);
        score.recordRoundWin(PlayerSide.ONE);
        score.recordRoundWin(PlayerSide.TWO);

        assertEquals("Round 3 | YOU 1 - 1 RIVAL | First to 3", PlayGameState.matchScoreDisplayLabel(score));
    }

    @Test
    void primaryAndMutedHudTextMeetNormalTextContrastOnTheArena() {
        assertTrue(grayscaleContrastRatio(
                PlayGameState.HUD_PRIMARY_COLOR, GameConstants.ARENA_BACKGROUND_COLOR) >= 4.5);
        assertTrue(grayscaleContrastRatio(
                PlayGameState.HUD_MUTED_COLOR, GameConstants.ARENA_BACKGROUND_COLOR) >= 4.5);
    }

    @Test
    void compactHudBackdropsKeepStableDimensionsAndOpacity() {
        assertEquals(176, PlayGameState.HUD_PANEL_ALPHA);
        assertEquals(384.0F, PlayGameState.MATCH_SCORE_PANEL_WIDTH);
        assertEquals(32.0F, PlayGameState.MATCH_SCORE_PANEL_HEIGHT);
        assertEquals(128.0F, PlayGameState.SHORTBOW_HUD_PANEL_WIDTH);
        assertEquals(66.0F, PlayGameState.SHORTBOW_HUD_PANEL_HEIGHT);
    }

    @Test
    void calculateThrustAngleUsesCenteredRandomOffset() {
        float base = 1.25f;

        assertEquals(base - HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 0.0f), 1e-6);
        assertEquals(base, PlayGameState.calculateThrustAngle(base, 0.5f), 1e-6);
        assertEquals(base + HALF_PI * 0.5f, PlayGameState.calculateThrustAngle(base, 1.0f), 1e-6);
    }

    @Test
    void shortbowCollisionRecordsPressureForItsOwningPlayer() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);

        state.checkCollision(system);

        assertEquals(
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.PRESSURE, 0),
                system.getTacticalEventLog().getFirst());
        assertEquals(1, system.getRoundCombatStats().playerOne().shortbowHits());
    }

    @Test
    void interceptedArrowCannotAlsoRecordPressureAgainstAPlayer() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);
        system.getMyGroup().getRemovingArrowList().add(arrow);

        state.checkCollision(system);

        assertEquals(0, system.getTacticalEventLog().size());
    }

    @Test
    void tacticalFeedbackLabelsIdentifyBothThePlayerAndEventType() {
        assertEquals("YOU: OPENING", PlayGameState.tacticalFeedbackLabel(PlayerSide.ONE, TacticalEventType.OPENING));
        assertEquals("YOU: CHARGE BREAK", PlayGameState.tacticalFeedbackLabel(PlayerSide.ONE, TacticalEventType.DISRUPT));
        assertEquals("RIVAL: FINISH", PlayGameState.tacticalFeedbackLabel(PlayerSide.TWO, TacticalEventType.FINISH));
        assertEquals("ARROWS: INTERCEPT", PlayGameState.tacticalFeedbackLabel(null, TacticalEventType.INTERCEPT));
    }

    @Test
    void arrowInterceptionUsesAMidpointAndDedicatedFeedbackParticles() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        ShortbowArrow myArrow = new ShortbowArrow(app);
        ShortbowArrow otherArrow = new ShortbowArrow(app);
        myArrow.setxPosition(200.0F);
        myArrow.setyPosition(100.0F);
        otherArrow.setxPosition(208.0F);
        otherArrow.setyPosition(100.0F);
        system.getMyGroup().addArrow(myArrow);
        system.getOtherGroup().addArrow(otherArrow);

        state.checkCollision(system);

        assertEquals(
                GameConstants.ARROW_BREAK_PARTICLE_COUNT * 2 + GameConstants.INTERCEPT_PARTICLE_COUNT + 1,
                system.getCommonParticleSet().getParticleList().size());
        assertEquals(GameConstants.INTERCEPT_HIT_STOP_FRAMES, system.getCombatPauseFrameCount());
        assertEquals(
                new TacticalEvent(null, TacticalEventType.INTERCEPT, 0),
                system.getTacticalEventLog().getFirst());
        assertEquals(1, system.getRoundCombatStats().interceptionCount());
    }

    @Test
    void oneArrowCanResolveOnlyOneInterceptionPerFrame() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        ShortbowArrow myArrow = new ShortbowArrow(app);
        ShortbowArrow firstEnemyArrow = new ShortbowArrow(app);
        ShortbowArrow secondEnemyArrow = new ShortbowArrow(app);
        for (ShortbowArrow arrow : List.of(myArrow, firstEnemyArrow, secondEnemyArrow)) {
            arrow.setxPosition(240.0F);
            arrow.setyPosition(160.0F);
        }
        system.getMyGroup().addArrow(myArrow);
        system.getOtherGroup().addArrow(firstEnemyArrow);
        system.getOtherGroup().addArrow(secondEnemyArrow);

        state.checkCollision(system);

        assertEquals(1, system.getRoundCombatStats().interceptionCount());
        assertEquals(1, system.getTacticalEventLog().size());
        assertEquals(1, system.getMyGroup().getRemovingArrowList().size());
        assertEquals(1, system.getOtherGroup().getRemovingArrowList().size());
    }

    @Test
    void sweptInterceptionUsesTheCrossingPointForEveryFragment() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        ShortbowArrow myArrow = new ShortbowArrow(app);
        ShortbowArrow enemyArrow = new ShortbowArrow(app);
        myArrow.setxPosition(100.0F);
        myArrow.setyPosition(100.0F);
        myArrow.setVelocity(0.0F, 24.0F);
        enemyArrow.setxPosition(124.0F);
        enemyArrow.setyPosition(100.0F);
        enemyArrow.setVelocity(PI, 24.0F);
        myArrow.update();
        enemyArrow.update();
        system.getMyGroup().addArrow(myArrow);
        system.getOtherGroup().addArrow(enemyArrow);

        state.checkCollision(system);

        assertEquals(1, system.getRoundCombatStats().interceptionCount());
        assertEquals(1, system.getTacticalEventLog().size());
        assertEquals(1, system.getMyGroup().getRemovingArrowList().size());
        assertEquals(1, system.getOtherGroup().getRemovingArrowList().size());
        assertEquals(GameConstants.INTERCEPT_HIT_STOP_FRAMES, system.getCombatPauseFrameCount());
        system.getCommonParticleSet().getParticleList().forEach(particle -> {
            assertEquals(112.0F, particle.getxPosition(), 1.0e-4F);
            assertEquals(100.0F, particle.getyPosition(), 1.0e-4F);
        });
    }

    @Test
    void earliestSweptContactWinsEvenWhenTheLaterArrowWasInsertedFirst() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        ShortbowArrow myArrow = movingArrow(app, 100.0F, 24.0F);
        ShortbowArrow laterEnemyArrow = stationaryArrow(app, 136.0F);
        ShortbowArrow earlierEnemyArrow = stationaryArrow(app, 120.0F);
        system.getMyGroup().addArrow(myArrow);
        system.getOtherGroup().addArrow(laterEnemyArrow);
        system.getOtherGroup().addArrow(earlierEnemyArrow);

        state.checkCollision(system);

        assertEquals(1, system.getRoundCombatStats().interceptionCount());
        assertTrue(system.getOtherGroup().getRemovingArrowList().contains(earlierEnemyArrow));
        assertFalse(system.getOtherGroup().getRemovingArrowList().contains(laterEnemyArrow));
        system.getCommonParticleSet().getParticleList().forEach(particle -> {
            assertEquals(112.0F, particle.getxPosition(), 1.0e-4F);
            assertEquals(100.0F, particle.getyPosition(), 1.0e-4F);
        });
    }

    @Test
    void interceptionPauseConsumesExactlyItsConfiguredFrames() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);

        system.startCombatPause(GameConstants.INTERCEPT_HIT_STOP_FRAMES);

        for (int frame = 0; frame < GameConstants.INTERCEPT_HIT_STOP_FRAMES; frame++) {
            assertTrue(system.consumeCombatPauseFrame());
        }
        assertFalse(system.consumeCombatPauseFrame());
        assertEquals(0, system.getCombatPauseFrameCount());
    }

    @Test
    void activeCombatClockExcludesHitStopFrames() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        system.startCombatPause(2);

        assertFalse(PlayGameState.beginActiveCombatFrame(system));
        assertFalse(PlayGameState.beginActiveCombatFrame(system));
        assertEquals(0, system.getRoundCombatStats().activeFrameCount());

        assertTrue(PlayGameState.beginActiveCombatFrame(system));
        assertEquals(1, system.getRoundCombatStats().activeFrameCount());
    }

    @Test
    void shortbowHitBreaksAChargeAndInvalidatesItsOldOpening() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        DrawLongbowPlayerActorState longbowState = new DrawLongbowPlayerActorState(app);
        system.recordPressure(system.getOtherGroup());
        target.setState(longbowState.entryState(target));
        system.drainTacticalEvents();
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);

        state.checkCollision(system);
        system.recordLongbowFinish(system.getOtherGroup());

        assertEquals(List.of(
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.PRESSURE, 0),
                new TacticalEvent(PlayerSide.ONE, TacticalEventType.DISRUPT, 0)
        ), system.getTacticalEventLog());
        assertEquals(0, target.getChargedFrameCount());
        assertEquals(1, system.getRoundCombatStats().playerOne().chargeBreaks());
        assertEquals(GameConstants.DISRUPT_HIT_STOP_FRAMES, system.getCombatPauseFrameCount());
        assertEquals(
                GameConstants.ARROW_BREAK_PARTICLE_COUNT + GameConstants.DISRUPT_PARTICLE_COUNT + 1,
                system.getCommonParticleSet().getParticleList().size());
    }

    @Test
    void lethalArrowCapturesItsLaunchPathBeforeTheResultFreeze() {
        App app = new App();
        GameSystem system = new GameSystem(true, false, app);
        app.setSystem(system);
        PlayGameState state = new PlayGameState(app);
        PlayerActor target = (PlayerActor) system.getOtherGroup().getPlayer();
        LongbowArrowHead arrow = new LongbowArrowHead(app);
        arrow.setLaunchPosition(320.0F, 640.0F);
        arrow.setxPosition(target.getxPosition());
        arrow.setyPosition(target.getyPosition());
        system.getMyGroup().addArrow(arrow);

        state.checkCollision(system);
        state.checkStateTransition(system);

        assertEquals(PlayerSide.ONE, state.getPendingLethalHit().attacker());
        assertEquals(320.0F, state.getPendingLethalHit().launchX());
        assertEquals(target.getxPosition(), state.getPendingLethalHit().targetX());
        assertInstanceOf(LethalHitState.class, system.getCurrentState());
        assertEquals(1, system.getMatchScore().getPlayerOneWins());
        assertEquals(1, system.getRoundCombatStats().playerOne().longbowHits());
    }

    private static double grayscaleContrastRatio(int first, int second) {
        final double lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
        final double darker = Math.min(relativeLuminance(first), relativeLuminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int grayscale) {
        final double channel = Math.max(0, Math.min(255, grayscale)) / 255.0;
        return channel <= 0.04045
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    private static ShortbowArrow movingArrow(App app, float startX, float speed) {
        ShortbowArrow arrow = new ShortbowArrow(app);
        arrow.setxPosition(startX);
        arrow.setyPosition(100.0F);
        arrow.setVelocity(0.0F, speed);
        arrow.update();
        return arrow;
    }

    private static ShortbowArrow stationaryArrow(App app, float x) {
        return movingArrow(app, x, 0.0F);
    }
}
