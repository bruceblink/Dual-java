package com.likanug.dual.inputDevice;

public abstract class AbstractInputDevice {

    protected int horizontalMoveButton;
    protected int verticalMoveButton;
    protected boolean shotButtonPressed;
    protected boolean shotButtonJustPressed;
    protected boolean longShotButtonPressed;
    protected boolean longShotButtonJustPressed;
    protected boolean longShotButtonJustReleased;
    protected boolean hasAimAngle;
    protected float aimAngle;

    public int getHorizontalMoveButton() {
        return horizontalMoveButton;
    }

    public void setHorizontalMoveButton(int horizontalMoveButton) {
        this.horizontalMoveButton = horizontalMoveButton;
    }

    public int getVerticalMoveButton() {
        return verticalMoveButton;
    }

    public void setVerticalMoveButton(int verticalMoveButton) {
        this.verticalMoveButton = verticalMoveButton;
    }

    public boolean isShotButtonPressed() {
        return shotButtonPressed;
    }

    public void setShotButtonPressed(boolean shotButtonPressed) {
        shotButtonJustPressed = shotButtonPressed && !this.shotButtonPressed;
        this.shotButtonPressed = shotButtonPressed;
    }

    public boolean isLongShotButtonPressed() {
        return longShotButtonPressed;
    }

    public void setLongShotButtonPressed(boolean longShotButtonPressed) {
        this.longShotButtonPressed = longShotButtonPressed;
    }

    public void operateMoveButton(int horizontal, int vertical) {
        horizontalMoveButton = horizontal;
        verticalMoveButton = vertical;
    }

    public void operateShotButton(boolean pressed) {
        shotButtonJustPressed = pressed && !shotButtonPressed;
        shotButtonPressed = pressed;
    }

    public boolean isShotButtonJustPressed() {
        return shotButtonJustPressed;
    }

    public void operateLongShotButton(boolean pressed) {
        longShotButtonJustPressed = pressed && !longShotButtonPressed;
        longShotButtonJustReleased = !pressed && longShotButtonPressed;
        longShotButtonPressed = pressed;
    }

    public boolean isLongShotButtonJustPressed() {
        return longShotButtonJustPressed;
    }

    public boolean isLongShotButtonJustReleased() {
        return longShotButtonJustReleased;
    }

    public boolean hasAimAngle() {
        return hasAimAngle;
    }

    public float getAimAngle() {
        return aimAngle;
    }

    public void operateAim(float angle) {
        aimAngle = angle;
        hasAimAngle = true;
    }

}
