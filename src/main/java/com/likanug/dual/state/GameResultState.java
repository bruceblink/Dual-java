package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static com.likanug.dual.App.largeFont;
import static com.likanug.dual.App.smallFont;
import static processing.core.PConstants.CENTER;

public class GameResultState extends GameSystemState {

    private final String resultMessage;
    private final int durationFrameCount = FPS;
    static final float RESULT_MESSAGE_X = INTERNAL_CANVAS_WIDTH * 0.5F;
    static final float RESULT_MESSAGE_Y = INTERNAL_CANVAS_HEIGHT * 0.5F - 40.0F;
    static final float RESET_PROMPT_X = RESULT_MESSAGE_X;
    static final float RESET_PROMPT_Y = INTERNAL_CANVAS_HEIGHT * 0.5F + 40.0F;

    public GameResultState(App app, String msg) {
        super(app);
        resultMessage = msg;
    }

    public void runSystem(GameSystem system) {
        system.getMyGroup().update();
        system.getOtherGroup().update();
        system.getMyGroup().displayPlayer();
        system.getOtherGroup().displayPlayer();

        system.getCommonParticleSet().update();
        system.getCommonParticleSet().display();
    }

    /** 在固定画布中心绘制结算层；演示对局也展示结果，但不显示人工重置提示。 */
    public void displayMessage(GameSystem system) {
        app.pushStyle();
        app.textAlign(CENTER, CENTER);
        app.noStroke();
        app.fill(0, 176);
        app.rect(
                INTERNAL_CANVAS_WIDTH * 0.5F,
                INTERNAL_CANVAS_HEIGHT * 0.5F,
                INTERNAL_CANVAS_WIDTH,
                INTERNAL_CANVAS_HEIGHT
        );

        app.fill(255);
        app.textFont(largeFont, 72);
        app.text(resultMessage, RESULT_MESSAGE_X, RESULT_MESSAGE_Y);
        if (!system.isDemoPlay() && shouldShowResetPrompt()) {
            app.textFont(smallFont, 20);
            app.fill(224);
            app.text("Press X key to reset.", RESET_PROMPT_X, RESET_PROMPT_Y);
        }
        app.popStyle();
    }

    public void checkStateTransition(GameSystem system) {
        if (system.isDemoPlay()) {
            if (properFrameCount > durationFrameCount * 3) {
                app.newGame(true, system.isShowsInstructionWindow());
            }
        } else {
            if (properFrameCount > durationFrameCount && app.getCurrentKeyInput().isXPressed) {
                app.newGame(true, true);  // back to demoplay with instruction window
            }
        }
    }

    /** 重置提示延迟一秒出现，避免玩家误按跳过结算画面。 */
    boolean shouldShowResetPrompt() {
        return properFrameCount > durationFrameCount;
    }

    /** 返回标题和提示的组合中心，用于确保完整结算内容垂直居中。 */
    static float resultGroupCenterY() {
        return (RESULT_MESSAGE_Y + RESET_PROMPT_Y) * 0.5F;
    }

}
