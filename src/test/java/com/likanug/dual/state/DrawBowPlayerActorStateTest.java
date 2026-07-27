package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.player.NullPlayerActor;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.InputDevice;
import com.likanug.dual.playerEngine.PlayerEngine;
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
}
