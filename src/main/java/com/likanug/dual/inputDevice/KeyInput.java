package com.likanug.dual.inputDevice;

public class KeyInput {

    public boolean isUpPressed = false;
    public boolean isDownPressed = false;
    public boolean isLeftPressed = false;
    public boolean isRightPressed = false;
    public boolean isZPressed = false;
    public boolean isXPressed = false;
    public boolean isWPressed = false;
    public boolean isAPressed = false;
    public boolean isSPressed = false;
    public boolean isDPressed = false;
    private boolean mouseShotPressed = false;
    private boolean mouseLongShotPressed = false;
    private boolean hasMouseAim = false;
    private float mouseAimX;
    private float mouseAimY;

    public boolean isMovingUp() {
        return isUpPressed || isWPressed;
    }

    public boolean isMovingDown() {
        return isDownPressed || isSPressed;
    }

    public boolean isMovingLeft() {
        return isLeftPressed || isAPressed;
    }

    public boolean isMovingRight() {
        return isRightPressed || isDPressed;
    }

    public boolean isShotPressed() {
        return isZPressed || mouseShotPressed;
    }

    public boolean isLongShotPressed() {
        return isXPressed || mouseLongShotPressed;
    }

    /**
     * 保存最近一次位于竞技场内的鼠标目标，供玩家引擎换算瞄准角。
     * 画布外移动不会调用此方法，因此不会覆盖最后一个有效目标。
     */
    public void updateMouseAim(float canvasX, float canvasY) {
        mouseAimX = canvasX;
        mouseAimY = canvasY;
        hasMouseAim = true;
    }

    public boolean hasMouseAim() {
        return hasMouseAim;
    }

    public float getMouseAimX() {
        return mouseAimX;
    }

    public float getMouseAimY() {
        return mouseAimY;
    }

    public void setMouseShotPressed(boolean pressed) {
        mouseShotPressed = pressed;
    }

    public void setMouseLongShotPressed(boolean pressed) {
        mouseLongShotPressed = pressed;
    }

    public void releaseMouseButtons() {
        mouseShotPressed = false;
        mouseLongShotPressed = false;
    }

    public void clear() {
        isUpPressed = false;
        isDownPressed = false;
        isLeftPressed = false;
        isRightPressed = false;
        isZPressed = false;
        isXPressed = false;
        isWPressed = false;
        isAPressed = false;
        isSPressed = false;
        isDPressed = false;
        releaseMouseButtons();
    }

}
