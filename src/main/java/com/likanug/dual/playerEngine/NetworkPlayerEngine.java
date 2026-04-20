package com.likanug.dual.playerEngine;

import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.network.GameNetwork;

/**
 * 远端玩家引擎：从网络接收到的对方 KeyInput 来驱动远端角色，
 * 行为与 {@link HumanPlayerEngine} 完全一致，只是输入源来自网络而非本地键盘。
 */
public class NetworkPlayerEngine extends PlayerEngine {

    private final GameNetwork network;

    public NetworkPlayerEngine(GameNetwork network) {
        this.network = network;
    }

    @Override
    public void run(PlayerActor player) {
        KeyInput ki = network.getRemoteInput();
        final int intUp    = ki.isUpPressed    ? -1 : 0;
        final int intDown  = ki.isDownPressed  ?  1 : 0;
        final int intLeft  = ki.isLeftPressed  ? -1 : 0;
        final int intRight = ki.isRightPressed ?  1 : 0;
        controllingInputDevice.operateMoveButton(intLeft + intRight, intUp + intDown);
        controllingInputDevice.operateShotButton(ki.isZPressed);
        controllingInputDevice.operateLongShotButton(ki.isXPressed);
    }
}
