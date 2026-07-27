package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.TacticalEvent;
import com.likanug.dual.game.TacticalEventType;

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
    static final float TACTICAL_RESULT_MESSAGE_Y = INTERNAL_CANVAS_HEIGHT * 0.5F - 80.0F;
    static final float TACTICAL_FINISH_Y = INTERNAL_CANVAS_HEIGHT * 0.5F;
    static final float TACTICAL_RESET_PROMPT_Y = INTERNAL_CANVAS_HEIGHT * 0.5F + 80.0F;
    private final TacticalEvent finishFeedback;

    public GameResultState(App app, String msg) {
        this(app, msg, null);
    }

    /** Keeps a confirmed tactical finish visible after the play state hands control to the result overlay. */
    public GameResultState(App app, String msg, TacticalEvent finishFeedback) {
        super(app);
        if (finishFeedback != null && finishFeedback.type() != TacticalEventType.FINISH) {
            throw new IllegalArgumentException("Result feedback must be a FINISH event.");
        }
        resultMessage = msg;
        this.finishFeedback = finishFeedback;
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
        app.text(resultMessage, RESULT_MESSAGE_X, resultMessageY());
        if (finishFeedback != null) {
            app.textFont(smallFont, 28);
            app.fill(192, 64, 64);
            app.text(PlayGameState.tacticalFeedbackLabel(finishFeedback.attacker(), finishFeedback.type()),
                    RESULT_MESSAGE_X, TACTICAL_FINISH_Y);
        }
        if (!system.isDemoPlay() && shouldShowResetPrompt()) {
            app.textFont(smallFont, 20);
            app.fill(224);
            app.text("Press X key to reset.", RESET_PROMPT_X, resetPromptY());
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

    TacticalEvent getFinishFeedback() {
        return finishFeedback;
    }

    private float resultMessageY() {
        return finishFeedback == null ? RESULT_MESSAGE_Y : TACTICAL_RESULT_MESSAGE_Y;
    }

    private float resetPromptY() {
        return finishFeedback == null ? RESET_PROMPT_Y : TACTICAL_RESET_PROMPT_Y;
    }

    /** 返回标题和提示的组合中心，用于确保完整结算内容垂直居中。 */
    static float resultGroupCenterY() {
        return (RESULT_MESSAGE_Y + RESET_PROMPT_Y) * 0.5F;
    }

    /** Confirms that tactical and normal result layouts both keep their complete content centered. */
    static float tacticalResultGroupCenterY() {
        return (TACTICAL_RESULT_MESSAGE_Y + TACTICAL_RESET_PROMPT_Y) * 0.5F;
    }

}
