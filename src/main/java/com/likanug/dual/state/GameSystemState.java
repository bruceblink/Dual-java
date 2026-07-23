package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;

import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;

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

    /** 在震动矩阵之外，以竞技场中心为原点绘制固定界面。 */
    public void displayInterface(GameSystem system) {
        app.pushMatrix();
        app.translate(INTERNAL_CANVAS_WIDTH * 0.5F, INTERNAL_CANVAS_HEIGHT * 0.5F);
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
