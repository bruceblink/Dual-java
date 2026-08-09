package com.likanug.dual.state;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.MatchScore;
import com.likanug.dual.game.PlayerSide;
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
    static final float RESULT_MESSAGE_Y = INTERNAL_CANVAS_HEIGHT * 0.5F - 80.0F;
    static final float RESET_PROMPT_X = RESULT_MESSAGE_X;
    static final float RESET_PROMPT_Y = INTERNAL_CANVAS_HEIGHT * 0.5F + 80.0F;
    static final float TACTICAL_RESULT_MESSAGE_Y = RESULT_MESSAGE_Y;
    static final float TACTICAL_FINISH_Y = INTERNAL_CANVAS_HEIGHT * 0.5F - 28.0F;
    static final float ROUND_SCORE_Y = INTERNAL_CANVAS_HEIGHT * 0.5F + 28.0F;
    static final float TACTICAL_RESET_PROMPT_Y = RESET_PROMPT_Y;
    private final TacticalEvent finishFeedback;
    private final MatchScore.RoundResult roundResult;
    private boolean networkResetRequested;
    private boolean networkResetTimedOut;
    private boolean networkScoreMismatch;
    private boolean xPressedLastFrame;
    private int networkResetWaitFrameCount;

    public GameResultState(App app, String msg) {
        this(app, msg, null);
    }

    /** Keeps a confirmed tactical finish visible after the play state hands control to the result overlay. */
    public GameResultState(App app, String msg, TacticalEvent finishFeedback) {
        this(app, msg, finishFeedback, null);
    }

    /** Keeps the round outcome alongside existing tactical result feedback for the next-round action. */
    public GameResultState(App app, String msg, TacticalEvent finishFeedback, MatchScore.RoundResult roundResult) {
        super(app);
        if (finishFeedback != null && finishFeedback.type() != TacticalEventType.FINISH) {
            throw new IllegalArgumentException("Result feedback must be a FINISH event.");
        }
        resultMessage = msg;
        this.finishFeedback = finishFeedback;
        this.roundResult = roundResult;
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
        if (roundResult != null) {
            app.textFont(smallFont, 22);
            app.fill(224);
            app.text(roundScoreDisplayLabel(roundResult), RESULT_MESSAGE_X, ROUND_SCORE_Y);
        }
        if (!system.isDemoPlay() && shouldShowResetPrompt()) {
            app.textFont(smallFont, 20);
            app.fill(224);
            app.text(resetPromptLabel(), RESET_PROMPT_X, resetPromptY());
        }
        app.popStyle();
    }

    public void checkStateTransition(GameSystem system) {
        if (system.isDemoPlay()) {
            if (properFrameCount > durationFrameCount * 3) {
                app.newGame(true, system.isShowsInstructionWindow());
            }
        } else {
            if (properFrameCount > durationFrameCount) {
                boolean xPressed = app.getCurrentKeyInput().isXPressed;
                networkScoreMismatch = system.hasNetworkRoundResultMismatch(roundResult);
                if (!xPressed) {
                    xPressedLastFrame = false;
                    if (networkResetTimedOut) {
                        networkResetTimedOut = false;
                        networkResetRequested = false;
                        networkResetWaitFrameCount = 0;
                    }
                } else if (!xPressedLastFrame && roundResult != null) {
                    system.requestNetworkRematch(roundResult);
                    networkResetRequested = true;
                    networkResetWaitFrameCount = 0;
                }
                xPressedLastFrame = xPressed;

                if (xPressed && roundResult != null && networkResetRequested) {
                    if (!system.isNetworkRematchReady(roundResult)) {
                        networkResetWaitFrameCount++;
                        if (hasNetworkRematchTimedOut(networkResetWaitFrameCount)) {
                            networkResetTimedOut = true;
                            networkResetRequested = false;
                        }
                        return;
                    }
                    if (roundResult.matchWinner().isPresent()) {
                        system.resetMatch();
                    } else {
                        system.resetRound();
                    }
                } else if (xPressed && roundResult == null) {
                    app.newGame(true, true);  // legacy result without score
                } else if (roundResult != null && roundResult.matchWinner().isPresent()
                        && app.getCurrentKeyInput().isZPressed) {
                    app.newGame(true, true);  // return to the demo without replaying
                }
            }
        }
    }

    /** 重置提示延迟一秒出现，避免玩家误按跳过结算画面。 */
    boolean shouldShowResetPrompt() {
        return properFrameCount > durationFrameCount;
    }

    /** Names the distinct actions available after a completed match or an intermediate round. */
    String resetPromptLabel() {
        if (networkScoreMismatch) return "Score mismatch. Waiting for sync...";
        if (networkResetTimedOut) return "No response. Release X and try again.";
        if (networkResetRequested) return "Waiting for rival...";
        if (roundResult != null && roundResult.matchWinner().isPresent()) {
            return "Press X to replay, Z for demo.";
        }
        return "Press X key for next round.";
    }

    /** Keeps a lost peer from leaving the result overlay waiting forever. */
    static boolean hasNetworkRematchTimedOut(int waitFrames) {
        return waitFrames >= GameConstants.NETWORK_REMATCH_TIMEOUT_FRAMES;
    }

    /** Formats a result-layer label that keeps round winner and match winner semantics distinct. */
    static String roundScoreDisplayLabel(MatchScore.RoundResult result) {
        String roundWinner = result.roundWinner() == PlayerSide.ONE ? "YOU" : "RIVAL";
        String outcome = result.matchWinner().isPresent()
                ? "MATCH COMPLETE: " + roundWinner
                : "ROUND WINNER: " + roundWinner;
        return outcome + " | Score YOU " + result.playerOneWins()
                + " - " + result.playerTwoWins();
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
