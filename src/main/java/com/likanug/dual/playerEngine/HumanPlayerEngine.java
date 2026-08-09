package com.likanug.dual.playerEngine;

import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.inputDevice.KeyInput;

import static processing.core.PApplet.atan2;

public class HumanPlayerEngine extends PlayerEngine {

    private final KeyInput currentKeyInput;

    public HumanPlayerEngine(KeyInput _keyInput) {
        currentKeyInput = _keyInput;
    }


    public KeyInput getCurrentKeyInput() {
        return currentKeyInput;
    }

    /**
     * 将键盘和鼠标的原始状态合并为本帧玩家意图；输入设备只保存意图，状态机仍负责移动和开火规则。
     */
    public void run(PlayerActor player) {
        final int intUp = currentKeyInput.isMovingUp() ? -1 : 0;
        final int intDown = currentKeyInput.isMovingDown() ? 1 : 0;
        final int intLeft = currentKeyInput.isMovingLeft() ? -1 : 0;
        final int intRight = currentKeyInput.isMovingRight() ? 1 : 0;

        controllingInputDevice.operateMoveButton(intLeft + intRight, intUp + intDown);
        controllingInputDevice.operateShotButton(currentKeyInput.isShotPressed());
        controllingInputDevice.operateLongShotButton(currentKeyInput.isLongShotPressed());
        if (currentKeyInput.hasMouseAim()) {
            controllingInputDevice.operateAim(atan2(
                    currentKeyInput.getMouseAimY() - player.getyPosition(),
                    currentKeyInput.getMouseAimX() - player.getxPosition()));
        } else if (currentKeyInput.hasKeyboardAim()) {
            controllingInputDevice.operateAim(currentKeyInput.getKeyboardAimAngle());
        }
    }

}
