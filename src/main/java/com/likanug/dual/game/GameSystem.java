package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.network.GameNetwork;
import com.likanug.dual.particle.Particle;
import com.likanug.dual.particle.ParticleBuilder;
import com.likanug.dual.particle.ParticleSet;
import com.likanug.dual.playerEngine.ComputerPlayerEngine;
import com.likanug.dual.playerEngine.HumanPlayerEngine;
import com.likanug.dual.playerEngine.NetworkPlayerEngine;
import com.likanug.dual.playerEngine.PlayerEngine;
import com.likanug.dual.state.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PConstants.*;

public class GameSystem {
    private final App app;
    private final ActorGroup myGroup;
    private final ActorGroup otherGroup;
    private final ParticleSet commonParticleSet;
    private GameSystemState currentState;
    private float screenShakeValue;
    private final DamagedPlayerActorState damagedState;
    private final MovePlayerActorState moveState;
    private final PlayerEngine myEngine;
    private final PlayerEngine otherEngine;
    private final GameBackground currentBackground;
    private final boolean demoPlay;
    private boolean showsInstructionWindow;
    private final TacticalEventRecorder tacticalEventRecorder =
            new TacticalEventRecorder(GameConstants.TACTICAL_OPENING_WINDOW_FRAMES);
    private final List<TacticalEvent> tacticalEventLog = new ArrayList<>();
    private int combatFrameCount;
    /** 用于游戏物理运算的可确定性随机数生成器（联机时双方使用相同种子保证一致） */
    private final Random gameRandom;
    private final MatchScore matchScore = new MatchScore(GameConstants.MATCH_ROUNDS_TO_WIN);

    public GameSystem(boolean demo, boolean instruction, App app) {
        this.app = app;
        // prepare ActorGroup
        this.myGroup = new ActorGroup();
        this.otherGroup = new ActorGroup();
        this.myGroup.setEnemyGroup(otherGroup);
        this.otherGroup.setEnemyGroup(myGroup);

        // prepare PlayerActorState
        this.moveState = new MovePlayerActorState(app);
        final DrawShortbowPlayerActorState drawShortbowState = new DrawShortbowPlayerActorState(app);
        final DrawBowPlayerActorState drawLongbowState = new DrawLongbowPlayerActorState(app);
        this.damagedState = new DamagedPlayerActorState(app);
        moveState.setDrawShortbowState(drawShortbowState);
        moveState.setDrawLongbowState(drawLongbowState);
        drawShortbowState.setMoveState(moveState);
        drawLongbowState.setMoveState(moveState);
        this.damagedState.setMoveState(moveState);

        // prepare PlayerActor
        if (demo) this.myEngine = new ComputerPlayerEngine(app);
        else this.myEngine = new HumanPlayerEngine(app.getCurrentKeyInput());
        PlayerActor myPlayer = createPlayer(myEngine, 255, INTERNAL_CANVAS_HEIGHT - 100);
        this.myGroup.setPlayer(myPlayer);
        this.otherEngine = new ComputerPlayerEngine(app);
        PlayerActor otherPlayer = createPlayer(otherEngine, 0, 100);
        this.otherGroup.setPlayer(otherPlayer);

        // other
        this.commonParticleSet = new ParticleSet(2048, app);
        this.currentState = new StartGameState(app);
        this.currentBackground = new GameBackground(
                GameConstants.ARENA_GRID_COLOR,
                GameConstants.ARENA_GRID_MAX_ACCELERATION,
                app);
        this.demoPlay = demo;
        this.showsInstructionWindow = instruction;
        this.gameRandom = new Random();
    }

    /**
     * 联机对战构造方法。
     * myGroup（本地玩家，位于画面下方）使用键盘输入；
     * otherGroup（远端玩家，位于画面上方）使用网络输入。
     * gameRandom 用共享种子初始化，保证双端物理运算一致。
     */
    public GameSystem(GameNetwork network, App app) {
        this.app = app;
        this.myGroup = new ActorGroup();
        this.otherGroup = new ActorGroup();
        this.myGroup.setEnemyGroup(otherGroup);
        this.otherGroup.setEnemyGroup(myGroup);

        this.moveState = new MovePlayerActorState(app);
        final DrawShortbowPlayerActorState drawShortbowState = new DrawShortbowPlayerActorState(app);
        final DrawBowPlayerActorState drawLongbowState = new DrawLongbowPlayerActorState(app);
        this.damagedState = new DamagedPlayerActorState(app);
        moveState.setDrawShortbowState(drawShortbowState);
        moveState.setDrawLongbowState(drawLongbowState);
        drawShortbowState.setMoveState(moveState);
        drawLongbowState.setMoveState(moveState);
        this.damagedState.setMoveState(moveState);

        // 本地玩家（下方，白色）
        this.myEngine = new HumanPlayerEngine(app.getCurrentKeyInput());
        PlayerActor myPlayer = createPlayer(myEngine, 255, INTERNAL_CANVAS_HEIGHT - 100);
        this.myGroup.setPlayer(myPlayer);

        // 远端玩家（上方，黑色）
        this.otherEngine = new NetworkPlayerEngine(network);
        PlayerActor otherPlayer = createPlayer(otherEngine, 0, 100);
        this.otherGroup.setPlayer(otherPlayer);

        this.commonParticleSet = new ParticleSet(2048, app);
        this.currentState = new StartGameState(app);
        this.currentBackground = new GameBackground(
                GameConstants.ARENA_GRID_COLOR,
                GameConstants.ARENA_GRID_MAX_ACCELERATION,
                app);
        this.demoPlay = false;
        this.showsInstructionWindow = false;
        this.gameRandom = new Random(network.getSharedSeed());
    }

    GameSystem(App app) {
        this(false, false, app);
    }

    private PlayerActor createPlayer(PlayerEngine engine, int fillColor, float spawnY) {
        PlayerActor player = new PlayerActor(engine, fillColor, app);
        player.resetForRound(INTERNAL_CANVAS_WIDTH * 0.5F, spawnY, moveState);
        return player;
    }

    public ActorGroup getMyGroup() {
        return myGroup;
    }

    public ActorGroup getOtherGroup() {
        return otherGroup;
    }

    public ParticleSet getCommonParticleSet() {
        return commonParticleSet;
    }

    public GameSystemState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(GameSystemState currentState) {
        this.currentState = currentState;
    }

    public MatchScore getMatchScore() {
        return matchScore;
    }

    /** Records one round winner while keeping the match score available for the next round. */
    public MatchScore.RoundResult recordRoundWin(PlayerSide roundWinner) {
        return matchScore.recordRoundWin(roundWinner);
    }

    /**
     * Rebuilds only round-scoped state: players, arrows, particles, tactical facts, and countdown.
     * Engines, match score, mode, and network-backed input devices remain attached to this system.
     */
    public void resetRound() {
        if (matchScore.isMatchComplete()) {
            throw new IllegalStateException("A completed match cannot start another round.");
        }

        myGroup.clearArrows();
        otherGroup.clearArrows();
        commonParticleSet.clearForRound();
        myGroup.setPlayer(createPlayer(myEngine, 255, INTERNAL_CANVAS_HEIGHT - 100));
        otherGroup.setPlayer(createPlayer(otherEngine, 0, 100));
        screenShakeValue = 0;
        combatFrameCount = 0;
        resetTacticalEvents();
        currentState = new StartGameState(app);
    }

    public float getScreenShakeValue() {
        return screenShakeValue;
    }

    public void setScreenShakeValue(float screenShakeValue) {
        this.screenShakeValue = screenShakeValue;
    }

    public DamagedPlayerActorState getDamagedState() {
        return damagedState;
    }

    public GameBackground getCurrentBackground() {
        return currentBackground;
    }

    public boolean isDemoPlay() {
        return demoPlay;
    }

    public boolean isShowsInstructionWindow() {
        return showsInstructionWindow;
    }

    public void setShowsInstructionWindow(boolean showsInstructionWindow) {
        this.showsInstructionWindow = showsInstructionWindow;
    }

    /** 返回用于游戏物理运算的可确定性随机数生成器 */
    public Random getGameRandom() {
        return gameRandom;
    }

    public int getCombatFrameCount() {
        return combatFrameCount;
    }

    /** Advances the deterministic combat clock once for each active play-state simulation frame. */
    public void advanceCombatFrame() {
        combatFrameCount++;
    }

    /** Returns immutable tactical facts for upcoming UI feedback or replay consumers. */
    public List<TacticalEvent> getTacticalEventLog() {
        return List.copyOf(tacticalEventLog);
    }

    /** Delivers each new tactical fact once so interface feedback does not replay old combat events. */
    public List<TacticalEvent> drainTacticalEvents() {
        List<TacticalEvent> events = List.copyOf(tacticalEventLog);
        tacticalEventLog.clear();
        return events;
    }

    /** Records the confirmed shortbow hit that begins one player's tactical opportunity. */
    public void recordPressure(ActorGroup attackerGroup) {
        tacticalEventLog.add(tacticalEventRecorder.recordPressure(resolvePlayerSide(attackerGroup), combatFrameCount));
    }

    /** Records a longbow charge only when it follows the same player's recent shortbow pressure. */
    public void recordLongbowChargeStarted(PlayerActor attacker) {
        tacticalEventRecorder.recordLongbowChargeStarted(resolvePlayerSide(attacker.getGroup()), combatFrameCount)
                .ifPresent(tacticalEventLog::add);
    }

    /** Records a lethal longbow payoff only when it completes the same player's tactical sequence. */
    public void recordLongbowFinish(ActorGroup attackerGroup) {
        tacticalEventRecorder.recordLongbowFinish(resolvePlayerSide(attackerGroup), combatFrameCount)
                .ifPresent(tacticalEventLog::add);
    }

    /** Returns whether a charging player is still converting a recent shortbow hit into an opening. */
    public boolean hasTacticalOpening(PlayerActor attacker) {
        return tacticalEventRecorder.hasActiveOpening(
                resolvePlayerSide(attacker.getGroup()), combatFrameCount);
    }

    /** Clears both recorded facts and incomplete windows when a round or match is reset. */
    public void resetTacticalEvents() {
        tacticalEventRecorder.reset();
        tacticalEventLog.clear();
    }

    private PlayerSide resolvePlayerSide(ActorGroup group) {
        if (group == myGroup) return PlayerSide.ONE;
        if (group == otherGroup) return PlayerSide.TWO;
        throw new IllegalArgumentException("Actor group does not belong to this game system.");
    }

    public void run() {
        //演示模式
        if (demoPlay) {
            if (app.getCurrentKeyInput().isZPressed) {
                // Z starts the match but is also the shortbow compatibility key; do not carry it into combat.
                app.getCurrentKeyInput().clear();
                app.setSystem(new GameSystem(app));  // stop demo and start game
                return;
            }
        }

        app.pushMatrix();

        if (screenShakeValue > 0) {
            app.translate(app.random(-screenShakeValue, screenShakeValue), app.random(-screenShakeValue, screenShakeValue));
            screenShakeValue -= 50F / FPS;
        }
        currentBackground.update();
        currentBackground.display();
        currentState.runWorld(this);

        app.popMatrix();
        currentState.displayInterface(this);
        currentState.finishFrame(this);
        if (demoPlay && showsInstructionWindow)
            displayDemo();
    }

    public void displayDemo() {
        app.pushStyle();

        final float panelCenterX = INTERNAL_CANVAS_WIDTH * 0.5F;
        final float panelCenterY = INTERNAL_CANVAS_HEIGHT * 0.5F;
        final float contentOffsetX = panelCenterX - 320.0F;

        app.stroke(0);
        app.strokeWeight(2);
        app.fill(255, 240);
        app.rect(
                panelCenterX,
                panelCenterY,
                448.0F,
                432.0F
        );

        app.textFont(App.smallFont, 20);
        app.textLeading(26);
        app.textAlign(RIGHT, BASELINE);
        app.fill(0);
        app.text("WASD / Arrow:", contentOffsetX + 280, 160);
        app.text("Mouse:", contentOffsetX + 280, 225);
        app.text("Left / Z:", contentOffsetX + 280, 290);
        app.text("Right / X:", contentOffsetX + 280, 355);
        app.textAlign(LEFT);
        app.text("Move", contentOffsetX + 300, 160);
        app.text("Aim outside lock range", contentOffsetX + 300, 225);
        app.text("Shortbow", contentOffsetX + 300, 290);
        app.text("Hold to charge longbow", contentOffsetX + 300, 355);
        app.textAlign(CENTER);
        app.text("- Press Z key to start -", panelCenterX, 455);
        app.text("(Click to hide this window)", panelCenterX, 490);
        app.popStyle();

        app.strokeWeight(1);
    }

    public void addSquareParticles(float x, float y, int particleCount, int particleSize, float minSpeed, float maxSpeed, int lifespanSecondValue) {
        final ParticleBuilder builder = commonParticleSet.getBuilder()
                .type(1)  // Square
                .position(x, y)
                .particleSize(particleSize)
                .particleColor(app.color(0))
                .lifespanSecond(lifespanSecondValue);
        for (int i = 0; i < particleCount; i++) {
            final Particle newParticle = builder
                    .polarVelocity(app.random(TWO_PI), app.random(minSpeed, maxSpeed))
                    .build();
            commonParticleSet.getParticleList().add(newParticle);
        }
    }

    /** Creates a cyan burst and ring at an arrow interception so it reads differently from a player hit. */
    public void addInterceptParticles(float x, float y) {
        final int interceptColor = app.color(96, 208, 232);
        final ParticleBuilder builder = commonParticleSet.getBuilder()
                .initialize()
                .position(x, y)
                .particleColor(interceptColor);
        final Particle ring = builder
                .type(3)
                .particleSize(GameConstants.INTERCEPT_RING_SIZE)
                .weight(GameConstants.INTERCEPT_RING_STROKE)
                .lifespanSecond(0.35F)
                .build();
        commonParticleSet.getParticleList().add(ring);

        for (int index = 0; index < GameConstants.INTERCEPT_PARTICLE_COUNT; index++) {
            Particle shard = builder
                    .type(1)
                    .polarVelocity(app.random(TWO_PI), app.random(2.0F, 6.0F))
                    .particleSize(GameConstants.INTERCEPT_PARTICLE_SIZE)
                    .lifespanSecond(0.3F)
                    .build();
            commonParticleSet.getParticleList().add(shard);
        }
    }
}
