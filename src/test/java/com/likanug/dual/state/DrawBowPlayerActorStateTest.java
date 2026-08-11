package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.player.NullPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.TacticalEventType;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.inputDevice.InputDevice;
import com.likanug.dual.playerEngine.PlayerEngine;
import com.likanug.dual.playerEngine.AiDifficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawBowPlayerActorStateTest {

    @Test
    void shortbowFiresOnlyOnAPressEdgeAndRespectsItsOwnCooldown() {
        assertTrue(DrawShortbowPlayerActorState.canFire(true, 0));
        assertFalse(DrawShortbowPlayerActorState.canFire(false, 0));
        assertFalse(DrawShortbowPlayerActorState.canFire(true, 1));
        assertFalse(DrawShortbowPlayerActorState.canFire(true, 0, false));

    }

    @Test
    void advancedAiInterceptAimIsOptInAndProbabilityBounded() {
        assertTrue(DrawShortbowPlayerActorState.shouldAimAtIncomingArrow(0.10F, AiDifficulty.ADVANCED));
        assertFalse(DrawShortbowPlayerActorState.shouldAimAtIncomingArrow(0.35F, AiDifficulty.ADVANCED));
        assertFalse(DrawShortbowPlayerActorState.shouldAimAtIncomingArrow(0.10F, AiDifficulty.STANDARD));
    }

    @Test
    void holdingTheShortbowButtonDoesNotCreateASecondArrow() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        InputDevice input = (InputDevice) engine.getControllingInputDevice();
        PlayerActor player = new PlayerActor(engine, 255, app);
        PlayerActor target = new PlayerActor(engine, 0, app);
        ActorGroup playerGroup = new ActorGroup();
        ActorGroup targetGroup = new ActorGroup();
        playerGroup.setEnemyGroup(targetGroup);
        targetGroup.setEnemyGroup(playerGroup);
        playerGroup.setPlayer(player);
        targetGroup.setPlayer(target);
        MovePlayerActorState moveState = new MovePlayerActorState(app);
        DrawShortbowPlayerActorState shortbowState = new DrawShortbowPlayerActorState(app);
        moveState.setDrawShortbowState(shortbowState);
        shortbowState.setMoveState(moveState);

        input.operateShotButton(true);
        moveState.act(player);
        assertEquals(1, playerGroup.getArrowList().size());

        input.operateShotButton(true);
        shortbowState.act(player);
        assertEquals(1, playerGroup.getArrowList().size());
    }

    @Test
    void successfulShortbowUsesAFixedFullMovementActionWindow() {
        ShortbowStateFixture fixture = shortbowStateFixture();
        fixture.input.operateMoveButton(1, 0);
        fixture.input.operateShotButton(true);

        fixture.player.act();

        assertEquals(GameConstants.SHORTBOW_ACTION_FRAMES, fixture.player.getShortbowActionFrameCount());
        assertEquals(1, fixture.playerGroup.getArrowList().size());
        assertEquals(1.0F, fixture.shortbowState.getMoveRatio());

        for (int frame = 0; frame < GameConstants.SHORTBOW_ACTION_FRAMES; frame++) {
            fixture.player.update();
            fixture.player.act();
        }

        assertEquals(fixture.moveState, fixture.player.getState());
        assertEquals(1, fixture.playerGroup.getArrowList().size());
        assertTrue(fixture.player.getxVelocity() > 1.0F);
    }

    @Test
    void heldLongbowStartsOnTheFirstFrameAfterShortbowAction() {
        ShortbowStateFixture fixture = shortbowStateFixture();
        fixture.input.operateShotButton(true);
        fixture.player.act();
        fixture.input.operateLongShotButton(true);

        for (int frame = 0; frame < GameConstants.SHORTBOW_ACTION_FRAMES; frame++) {
            fixture.player.update();
            fixture.player.act();
        }
        assertEquals(fixture.moveState, fixture.player.getState());

        fixture.player.update();
        fixture.player.act();

        assertEquals(fixture.longbowState, fixture.player.getState());
    }

    @Test
    void longbowChargeProgressIsContinuousAndClamped() {
        assertEquals(0.0F, DrawLongbowPlayerActorState.calculateChargeProgress(-1, 30));
        assertEquals(0.5F, DrawLongbowPlayerActorState.calculateChargeProgress(15, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(30, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(31, 30));
        assertEquals(1.0F, DrawLongbowPlayerActorState.calculateChargeProgress(0, 0));
    }

    @Test
    void longbowChargeUsesTheApprovedHalfSpeedMovementRatio() {
        DrawLongbowPlayerActorState state = new DrawLongbowPlayerActorState(new App());

        assertEquals(0.5F, GameConstants.LONGBOW_CHARGE_MOVE_RATIO);
        assertEquals(GameConstants.LONGBOW_CHARGE_MOVE_RATIO, state.getMoveRatio());
    }

    @Test
    void longbowReleaseCancelsBeforeMinimumChargeAndFiresAfterIt() {
        assertEquals(
                DrawLongbowPlayerActorState.ReleaseOutcome.KEEP_CHARGING,
                DrawLongbowPlayerActorState.releaseOutcome(true, 0, 30)
        );
        assertEquals(
                DrawLongbowPlayerActorState.ReleaseOutcome.CANCEL,
                DrawLongbowPlayerActorState.releaseOutcome(false, 29, 30)
        );
        assertEquals(
                DrawLongbowPlayerActorState.ReleaseOutcome.FIRE,
                DrawLongbowPlayerActorState.releaseOutcome(false, 30, 30)
        );
    }

    @Test
    void manuallyCancelledLongbowCannotReuseItsOldTacticalOpening() {
        App app = new App();
        app.setCurrentKeyInput(new KeyInput());
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);
        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        DrawLongbowPlayerActorState state = new DrawLongbowPlayerActorState(app);
        state.setMoveState(new MovePlayerActorState(app));
        system.recordPressure(system.getMyGroup());
        player.setState(state.entryState(player));

        state.onButtonReleased(player);
        system.recordLongbowFinish(system.getMyGroup());

        assertFalse(system.getTacticalEventLog().stream()
                .anyMatch(event -> event.type() == TacticalEventType.FINISH));
        player.setState(state.entryState(player));
        system.recordLongbowFinish(system.getMyGroup());
        assertEquals(TacticalEventType.FINISH, system.getTacticalEventLog().getLast().type());
    }

    @Test
    void longbowFireStartsAVisibleAttackRecoveryWindow() {
        App app = new App();
        KeyInput keyInput = new KeyInput();
        app.setCurrentKeyInput(keyInput);
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);
        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        DrawLongbowPlayerActorState state = new DrawLongbowPlayerActorState(app);
        MovePlayerActorState moveState = new MovePlayerActorState(app);
        state.setMoveState(moveState);
        player.setState(state);
        player.setChargedFrameCount((int) (GameConstants.LONGBOW_CHARGE_SEC * App.FPS));
        player.getEngine().getControllingInputDevice().operateLongShotButton(false);

        state.act(player);

        assertEquals(
                Math.round(GameConstants.LONGBOW_RECOVERY_SEC * App.FPS),
                player.getLongbowRecoveryFrameCount());
        assertEquals(moveState, player.getState());
        assertEquals(1.0F, MovePlayerActorState.recoveryProgress(
                player.getLongbowRecoveryFrameCount(),
                Math.round(GameConstants.LONGBOW_RECOVERY_SEC * App.FPS)));
    }

    @Test
    void longbowRecoveryBlocksWeaponsButStillAppliesMovementInput() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        InputDevice input = (InputDevice) engine.getControllingInputDevice();
        PlayerActor player = new PlayerActor(engine, 255, app);
        PlayerActor target = new PlayerActor(engine, 0, app);
        ActorGroup playerGroup = new ActorGroup();
        ActorGroup targetGroup = new ActorGroup();
        playerGroup.setEnemyGroup(targetGroup);
        targetGroup.setEnemyGroup(playerGroup);
        playerGroup.setPlayer(player);
        targetGroup.setPlayer(target);
        MovePlayerActorState moveState = new MovePlayerActorState(app);
        moveState.setDrawShortbowState(new DrawShortbowPlayerActorState(app));
        moveState.setDrawLongbowState(new DrawLongbowPlayerActorState(app));
        player.setState(moveState);
        player.setLongbowRecoveryFrameCount(1);
        input.operateMoveButton(1, 1);
        input.operateShotButton(true);
        input.operateLongShotButton(true);

        moveState.act(player);

        assertEquals(0, playerGroup.getArrowList().size());
        assertEquals(moveState, player.getState());
        assertTrue(player.getxVelocity() > 0.0F);
        assertTrue(player.getyVelocity() > 0.0F);
    }

    @Test
    void bufferedShortbowFiresOnceWhenLongbowRecoveryEnds() {
        ShortbowStateFixture fixture = shortbowStateFixture();
        fixture.player.setLongbowRecoveryFrameCount(3);
        fixture.input.operateShotButton(true);

        fixture.player.act();
        fixture.input.operateShotButton(false);
        assertEquals(0, fixture.playerGroup.getArrowList().size());
        assertTrue(fixture.player.hasBufferedShortbowInput());

        while (fixture.player.getLongbowRecoveryFrameCount() > 0) {
            fixture.player.update();
            fixture.player.act();
        }

        assertEquals(1, fixture.playerGroup.getArrowList().size());
        assertFalse(fixture.player.hasBufferedShortbowInput());
    }

    @Test
    void invalidShortbowPressStaysMobileAndDoesNotBlockHeldLongbow() {
        ShortbowStateFixture fixture = shortbowStateFixture();
        fixture.player.setShortbowCooldownFrameCount(10);
        fixture.input.operateShotButton(true);
        fixture.input.operateLongShotButton(true);

        fixture.player.act();

        assertEquals(fixture.longbowState, fixture.player.getState());
        assertEquals(0, fixture.playerGroup.getArrowList().size());
        assertFalse(fixture.player.hasBufferedShortbowInput());
    }

    @Test
    void shortbowPressDuringLongbowChargeIsNotReleasedAfterCancellation() {
        ShortbowStateFixture fixture = shortbowStateFixture();
        fixture.input.operateLongShotButton(true);
        fixture.player.act();
        assertEquals(fixture.longbowState, fixture.player.getState());

        fixture.input.operateShotButton(true);
        fixture.player.act();

        assertFalse(fixture.player.hasBufferedShortbowInput());
        assertEquals(0, fixture.playerGroup.getArrowList().size());
    }

    @Test
    void longbowChargeAdvancesOnlyWhileHeldAndStopsAtTheFiringThreshold() {
        assertEquals(1, DrawLongbowPlayerActorState.advanceChargeFrameCount(true, 0, 30));
        assertEquals(15, DrawLongbowPlayerActorState.advanceChargeFrameCount(false, 15, 30));
        assertEquals(30, DrawLongbowPlayerActorState.advanceChargeFrameCount(true, 29, 30));
        assertEquals(30, DrawLongbowPlayerActorState.advanceChargeFrameCount(true, 30, 30));
        assertEquals(1, DrawLongbowPlayerActorState.advanceChargeFrameCount(true, -2, 30));
    }

    @Test
    void longbowReadyFeedbackIsEmittedOnlyOnceAtTheChargeBoundary() {
        App app = new App();
        KeyInput keyInput = new KeyInput();
        app.setCurrentKeyInput(keyInput);
        GameSystem system = new GameSystem(false, false, app);
        app.setSystem(system);

        PlayerActor player = (PlayerActor) system.getMyGroup().getPlayer();
        DrawLongbowPlayerActorState state = new DrawLongbowPlayerActorState(app);
        MovePlayerActorState moveState = new MovePlayerActorState(app);
        state.setMoveState(moveState);
        player.getEngine().getControllingInputDevice().operateLongShotButton(true);
        player.setState(state.entryState(player));
        player.setChargedFrameCount((int) (GameConstants.LONGBOW_CHARGE_SEC * App.FPS));

        state.act(player);
        int particleCountAfterReady = system.getCommonParticleSet().getParticleList().size();
        state.act(player);

        assertTrue(player.isChargeReadyFeedbackShown());
        assertEquals(particleCountAfterReady, system.getCommonParticleSet().getParticleList().size());
    }

    @Test
    void longbowAutoAimIncludesTheRangeBoundaryAndRejectsDistantTargets() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        PlayerActor player = new PlayerActor(engine, 255, app);
        PlayerActor target = new PlayerActor(engine, 0, app);
        player.setxPosition(100.0F);
        player.setyPosition(100.0F);
        target.setxPosition(100.0F + GameConstants.LONGBOW_AUTO_AIM_RANGE);
        target.setyPosition(100.0F);

        assertTrue(DrawLongbowPlayerActorState.isAutoAimTargetAvailable(
                player, target, GameConstants.LONGBOW_AUTO_AIM_RANGE));

        target.setxPosition(101.0F + GameConstants.LONGBOW_AUTO_AIM_RANGE);
        assertFalse(DrawLongbowPlayerActorState.isAutoAimTargetAvailable(
                player, target, GameConstants.LONGBOW_AUTO_AIM_RANGE));
        assertFalse(DrawLongbowPlayerActorState.isAutoAimTargetAvailable(
                player, new NullPlayerActor(app), GameConstants.LONGBOW_AUTO_AIM_RANGE));
    }

    @Test
    void longbowAimTracksNearbyEnemyAndFallsBackToMouseOutsideRange() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        PlayerActor player = new PlayerActor(engine, 255, app);
        PlayerActor target = new PlayerActor(engine, 0, app);
        ActorGroup playerGroup = new ActorGroup();
        ActorGroup targetGroup = new ActorGroup();
        playerGroup.setEnemyGroup(targetGroup);
        targetGroup.setEnemyGroup(playerGroup);
        playerGroup.setPlayer(player);
        targetGroup.setPlayer(target);
        player.setxPosition(100.0F);
        player.setyPosition(100.0F);
        target.setxPosition(100.0F);
        target.setyPosition(300.0F);
        engine.getControllingInputDevice().operateAim(0.25F);
        DrawLongbowPlayerActorState state = new DrawLongbowPlayerActorState(app);

        state.aim(player, engine.getControllingInputDevice());
        assertEquals((float) (Math.PI * 0.5), player.getAimAngle(), 1e-6);

        target.setyPosition(100.0F + GameConstants.LONGBOW_AUTO_AIM_RANGE + 1.0F);
        state.aim(player, engine.getControllingInputDevice());
        assertEquals(0.25F, player.getAimAngle(), 1e-6);
    }

    /** Builds the shared state graph used by timing tests without depending on Processing rendering. */
    private static ShortbowStateFixture shortbowStateFixture() {
        App app = new App();
        PlayerEngine engine = new PlayerEngine() {
            @Override
            public void run(PlayerActor player) {
            }
        };
        InputDevice input = (InputDevice) engine.getControllingInputDevice();
        PlayerActor player = new PlayerActor(engine, 255, app);
        PlayerActor target = new PlayerActor(engine, 0, app);
        ActorGroup playerGroup = new ActorGroup();
        ActorGroup targetGroup = new ActorGroup();
        playerGroup.setEnemyGroup(targetGroup);
        targetGroup.setEnemyGroup(playerGroup);
        playerGroup.setPlayer(player);
        targetGroup.setPlayer(target);
        MovePlayerActorState moveState = new MovePlayerActorState(app);
        DrawShortbowPlayerActorState shortbowState = new DrawShortbowPlayerActorState(app);
        DrawLongbowPlayerActorState longbowState = new DrawLongbowPlayerActorState(app);
        moveState.setDrawShortbowState(shortbowState);
        moveState.setDrawLongbowState(longbowState);
        shortbowState.setMoveState(moveState);
        longbowState.setMoveState(moveState);
        player.setState(moveState);
        return new ShortbowStateFixture(input, player, playerGroup, moveState, shortbowState, longbowState);
    }

    private record ShortbowStateFixture(
            InputDevice input,
            PlayerActor player,
            ActorGroup playerGroup,
            MovePlayerActorState moveState,
            DrawShortbowPlayerActorState shortbowState,
            DrawLongbowPlayerActorState longbowState) {
    }
}
