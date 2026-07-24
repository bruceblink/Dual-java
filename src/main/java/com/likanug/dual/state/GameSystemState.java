package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;

public abstract class GameSystemState {

    protected final App app;
    protected int properFrameCount;

    public GameSystemState(App app) {
        this.app = app;
    }

    /** 更新并绘制会随屏幕震动移动的战斗世界。 */
    public void runWorld(GameSystem system) {
        runSystem(system);
    }

    /** 在震动矩阵之外绘制固定界面；各状态使用明确的画布坐标定位。 */
    public void displayInterface(GameSystem system) {
        app.pushMatrix();
        displayMessage(system);
        app.popMatrix();
    }

    /** 完成本帧状态转换和计时；必须在世界及界面绘制完成后调用一次。 */
    public void finishFrame(GameSystem system) {
        checkStateTransition(system);
        properFrameCount++;
    }

    public abstract void runSystem(GameSystem system);

    public abstract void displayMessage(GameSystem system);

    public abstract void checkStateTransition(GameSystem system);

}
