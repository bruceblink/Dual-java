package com.likanug.dual.playerEngine;

import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.network.GameNetwork;

/**
 * 远端玩家引擎：从网络接收到的对方 KeyInput 来驱动远端角色，
 * 行为与 {@link HumanPlayerEngine} 完全一致，只是输入源来自网络而非本地键盘。
 * <p>
 * 双方客户端都会以“自己在下方”的视角渲染，因此远端方向输入需要做 180 度镜像。
 */
public class NetworkPlayerEngine extends PlayerEngine {

    private final GameNetwork network;

    public NetworkPlayerEngine(GameNetwork network) {
        this.network = network;
    }

    @Override
    public void run(PlayerActor player) {
        KeyInput ki = network.getRemoteInput();
        final int horizontal = axisValue(ki.isLeftPressed, ki.isRightPressed);
        final int vertical = axisValue(ki.isUpPressed, ki.isDownPressed);
        controllingInputDevice.operateMoveButton(-horizontal, -vertical);
        controllingInputDevice.operateShotButton(ki.isZPressed);
        controllingInputDevice.operateLongShotButton(ki.isXPressed);
    }

    private static int axisValue(boolean negativePressed, boolean positivePressed) {
        final int negative = negativePressed ? -1 : 0;
        final int positive = positivePressed ? 1 : 0;
        return negative + positive;
    }
}
