package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.GameConstants;
import com.likanug.dual.actor.ActorGroup;
import com.likanug.dual.actor.player.PlayerActor;
import com.likanug.dual.actor.arrow.AbstractArrowActor;
import com.likanug.dual.network.GameNetwork;
import com.likanug.dual.network.NetworkRoundResult;
import com.likanug.dual.network.NetworkRematchRequest;
import com.likanug.dual.particle.Particle;
import com.likanug.dual.particle.ParticleBuilder;
import com.likanug.dual.particle.ParticleSet;
import com.likanug.dual.playerEngine.ComputerPlayerEngine;
import com.likanug.dual.playerEngine.AiDifficulty;
import com.likanug.dual.playerEngine.HumanPlayerEngine;
import com.likanug.dual.playerEngine.NetworkPlayerEngine;
import com.likanug.dual.playerEngine.PlayerEngine;
import com.likanug.dual.state.*;
import com.likanug.dual.inputDevice.KeyInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_HEIGHT;
import static com.likanug.dual.App.INTERNAL_CANVAS_WIDTH;
import static processing.core.PConstants.*;

public class GameSystem {
    static final float DEMO_GUIDE_WIDTH = 352.0F;
    static final float DEMO_GUIDE_LEFT_X = INTERNAL_CANVAS_WIDTH - DEMO_GUIDE_WIDTH;
    private static final float DEMO_GUIDE_CONTENT_X = DEMO_GUIDE_LEFT_X + 32.0F;
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
    private final ArenaLayout arenaLayout;
    private final GameNetwork network;
    private final boolean demoPlay;
    private final boolean localTwoPlayer;
    private boolean showsInstructionWindow;
    private final TacticalEventRecorder tacticalEventRecorder =
            new TacticalEventRecorder(GameConstants.TACTICAL_OPENING_WINDOW_FRAMES);
    private final List<TacticalEvent> tacticalEventLog = new ArrayList<>();
    private int combatFrameCount;
    private int combatPauseFrameCount;
    /** 用于游戏物理运算的可确定性随机数生成器（联机时双方使用相同种子保证一致） */
    private final Random gameRandom;
    private final MatchScore matchScore = new MatchScore(GameConstants.MATCH_ROUNDS_TO_WIN);

    public GameSystem(boolean demo, boolean instruction, App app) {
        this(demo, instruction, app, false, AiDifficulty.STANDARD, ArenaLayout.open());
    }

    /** Builds demo, human-versus-AI, or local two-player combat while sharing one rule pipeline. */
    public GameSystem(boolean demo, boolean instruction, App app, boolean localTwoPlayer) {
        this(demo, instruction, app, localTwoPlayer, AiDifficulty.STANDARD, ArenaLayout.open());
    }

    /** Builds one local mode with an explicit fair AI profile when the opponent is computer-controlled. */
    public GameSystem(boolean demo, boolean instruction, App app, boolean localTwoPlayer, AiDifficulty aiDifficulty) {
        this(demo, instruction, app, localTwoPlayer, aiDifficulty, ArenaLayout.open());
    }

    /** Builds a local game with explicit AI and arena configuration while keeping rules shared. */
    public GameSystem(
            boolean demo,
            boolean instruction,
            App app,
            boolean localTwoPlayer,
            AiDifficulty aiDifficulty,
            ArenaLayout arenaLayout) {
        this.app = app;
        this.network = null;
        this.arenaLayout = arenaLayout;
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
        if (demo || !localTwoPlayer) this.otherEngine = new ComputerPlayerEngine(app, aiDifficulty);
        else this.otherEngine = new HumanPlayerEngine(localInputOrFallback(app));
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
        this.localTwoPlayer = localTwoPlayer;
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
        this.network = network;
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
        this.localTwoPlayer = false;
        this.arenaLayout = ArenaLayout.open();
        this.showsInstructionWindow = false;
        this.gameRandom = new Random(network.getSharedSeed());
    }

    GameSystem(App app) {
        this(false, false, app);
    }

    private static KeyInput localInputOrFallback(App app) {
        return app.getSecondKeyInput() != null ? app.getSecondKeyInput() : new KeyInput();
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
        MatchScore.RoundResult result = matchScore.recordRoundWin(roundWinner);
        if (network != null) {
            int roundNumber = result.playerOneWins() + result.playerTwoWins();
            network.sendRoundResult(new NetworkRoundResult(
                    roundNumber,
                    roundWinner == PlayerSide.ONE ? NetworkRoundResult.SIDE_ONE : NetworkRoundResult.SIDE_TWO,
                    result.playerOneWins(),
                    result.playerTwoWins(),
                    result.matchWinner().isPresent()));
        }
        return result;
    }

    /** Sends the local result-overlay action so online peers reset the same completed round together. */
    public void requestNetworkRematch(MatchScore.RoundResult result) {
        if (network == null || result == null) return;
        int roundNumber = result.playerOneWins() + result.playerTwoWins();
        network.sendRematchRequest(new NetworkRematchRequest(roundNumber, result.matchWinner().isPresent()));
    }

    /** Allows a local result to advance only after the peer confirms the same round transition. */
    public boolean isNetworkRematchReady(MatchScore.RoundResult result) {
        if (network == null) return true;
        if (result == null) return false;
        if (!isNetworkRoundResultConsistent(result)) return false;
        NetworkRematchRequest request = network.getRemoteRematchRequest();
        return request != null
                && request.roundNumber() == result.playerOneWins() + result.playerTwoWins()
                && request.matchReset() == result.matchWinner().isPresent();
    }

    /** Compares the remote sender's perspective with this client's score and winner before a reset. */
    public boolean isNetworkRoundResultConsistent(MatchScore.RoundResult result) {
        if (network == null) return true;
        if (result == null) return false;
        NetworkRoundResult remoteResult = network.getRemoteRoundResult();
        if (remoteResult == null) return false;
        NetworkRoundResult expectedLocalPerspective = new NetworkRoundResult(
                result.playerOneWins() + result.playerTwoWins(),
                result.roundWinner() == PlayerSide.ONE ? NetworkRoundResult.SIDE_ONE : NetworkRoundResult.SIDE_TWO,
                result.playerOneWins(),
                result.playerTwoWins(),
                result.matchWinner().isPresent());
        return remoteResult.mirrored().equals(expectedLocalPerspective);
    }

    /** Distinguishes a delayed result frame from an actual peer score disagreement. */
    public boolean hasNetworkRoundResultMismatch(MatchScore.RoundResult result) {
        if (network == null || result == null) return false;
        NetworkRoundResult remoteResult = network.getRemoteRoundResult();
        return remoteResult != null && !isNetworkRoundResultConsistent(result);
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
        // The reset key is also a weapon key; release both local snapshots before combat resumes.
        app.clearLocalInputs();
        myGroup.setPlayer(createPlayer(myEngine, 255, INTERNAL_CANVAS_HEIGHT - 100));
        otherGroup.setPlayer(createPlayer(otherEngine, 0, 100));
        screenShakeValue = 0;
        combatFrameCount = 0;
        combatPauseFrameCount = 0;
        resetTacticalEvents();
        currentState = new StartGameState(app);
    }

    /** Starts a short deterministic hit-stop window used only for readable arrow interceptions. */
    public void startCombatPause(int frameCount) {
        combatPauseFrameCount = Math.max(combatPauseFrameCount, frameCount);
    }

    public int getCombatPauseFrameCount() {
        return combatPauseFrameCount;
    }

    /** Consumes one frozen simulation frame while leaving rendering and feedback visible. */
    public boolean consumeCombatPauseFrame() {
        if (combatPauseFrameCount <= 0) return false;
        combatPauseFrameCount--;
        return true;
    }

    /** Starts a fresh match in the current mode, retaining the input engines and any network link. */
    public void resetMatch() {
        if (network != null) network.resetRemoteMatchState();
        matchScore.reset();
        resetRound();
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

    public ArenaLayout getArenaLayout() {
        return arenaLayout;
    }

    /** Resolves cover collisions after positions advance and before weapon state acts. */
    public void resolveArenaCollisions() {
        arenaLayout.resolvePlayer((PlayerActor) myGroup.getPlayer());
        arenaLayout.resolvePlayer((PlayerActor) otherGroup.getPlayer());
        removeArrowsInsideCover(myGroup);
        removeArrowsInsideCover(otherGroup);
    }

    private void removeArrowsInsideCover(ActorGroup group) {
        for (AbstractArrowActor arrow : group.getArrowList()) {
            if (arenaLayout.blocksCircle(arrow.getxPosition(), arrow.getyPosition(), arrow.getCollisionRadius())) {
                group.getRemovingArrowList().add(arrow);
            }
        }
    }

    public boolean isDemoPlay() {
        return demoPlay;
    }

    public boolean isLocalTwoPlayer() {
        return localTwoPlayer;
    }

    public AiDifficulty getAiDifficulty() {
        return myEngine instanceof ComputerPlayerEngine computer
                ? computer.getDifficulty()
                : otherEngine instanceof ComputerPlayerEngine computer
                        ? computer.getDifficulty()
                        : null;
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

    /** Invalidates a released or interrupted charge without discarding its still-active pressure window. */
    public void cancelLongbowCharge(PlayerActor attacker) {
        tacticalEventRecorder.recordLongbowChargeCancelled(
                resolvePlayerSide(attacker.getGroup()), combatFrameCount);
    }

    /** Records the standalone counterplay fact when a shortbow interrupts an active longbow charge. */
    public void recordLongbowDisruption(ActorGroup attackerGroup) {
        tacticalEventLog.add(new TacticalEvent(
                resolvePlayerSide(attackerGroup), TacticalEventType.DISRUPT, combatFrameCount));
    }

    /** Records a lethal longbow payoff only when it completes the same player's tactical sequence. */
    public void recordLongbowFinish(ActorGroup attackerGroup) {
        tacticalEventRecorder.recordLongbowFinish(resolvePlayerSide(attackerGroup), combatFrameCount)
                .ifPresent(tacticalEventLog::add);
    }

    /** Records one neutral arrow-interception fact for the fixed HUD and replay feedback layer. */
    public void recordInterception() {
        tacticalEventLog.add(TacticalEvent.intercept(combatFrameCount));
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
                // Z opens the mode chooser; it is also a weapon key, so clear both local snapshots first.
                app.clearLocalInputs();
                app.openLocalModeMenu();
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
        arenaLayout.display(app);
        currentState.runWorld(this);

        app.popMatrix();
        currentState.displayInterface(this);
        currentState.finishFrame(this);
        if (demoPlay && showsInstructionWindow)
            displayDemo();
    }

    public void displayDemo() {
        app.pushStyle();
        app.rectMode(CORNER);
        app.noStroke();
        app.fill(248, 244);
        app.rect(DEMO_GUIDE_LEFT_X, 0.0F, DEMO_GUIDE_WIDTH, INTERNAL_CANVAS_HEIGHT);
        app.fill(96, 208, 232);
        app.rect(DEMO_GUIDE_LEFT_X, 0.0F, 5.0F, INTERNAL_CANVAS_HEIGHT);

        app.textAlign(LEFT, CENTER);
        app.fill(0);
        app.textFont(App.largeFont, 52);
        app.text("DUAL", DEMO_GUIDE_CONTENT_X, 58.0F);
        app.textFont(App.smallFont, 13);
        app.fill(0, 136);
        app.text("TACTICAL ARCHERY", DEMO_GUIDE_CONTENT_X, 101.0F);

        drawDemoGuideRow("MOVE", "WASD / ARROWS", 151.0F);
        drawDemoGuideRow("MANUAL AIM", "MOUSE BEYOND LOCK RANGE", 226.0F);
        drawDemoGuideRow("SHORTBOW", "LEFT CLICK / Z", 301.0F);
        drawDemoGuideRow("LONGBOW", "HOLD RIGHT CLICK / X", 376.0F);

        app.stroke(0, 40);
        app.line(DEMO_GUIDE_CONTENT_X, 441.0F, INTERNAL_CANVAS_WIDTH - 32.0F, 441.0F);
        app.noStroke();
        drawDemoGuideRow("P2 KEYBOARD", "IJKL + TFGH + B / V", 468.0F);

        app.fill(96, 208, 232);
        app.rect(DEMO_GUIDE_CONTENT_X, 554.0F, 10.0F, 10.0F);
        app.fill(0, 224);
        app.textFont(App.smallFont, 17);
        app.text("Z   CHOOSE MODE", DEMO_GUIDE_CONTENT_X + 22.0F, 559.0F);
        app.text("O   SETTINGS     P   PAUSE", DEMO_GUIDE_CONTENT_X, 605.0F);
        app.fill(0, 128);
        app.textFont(App.smallFont, 14);
        app.text("CLICK   HIDE GUIDE", DEMO_GUIDE_CONTENT_X, 670.0F);
        app.popStyle();
    }

    /** Draws one heading and control pair inside the fixed demo rail without covering the duel center. */
    private void drawDemoGuideRow(String heading, String controls, float y) {
        app.textFont(App.smallFont, 12);
        app.fill(0, 128);
        app.text(heading, DEMO_GUIDE_CONTENT_X, y);
        app.textFont(App.smallFont, 18);
        app.fill(0, 224);
        app.text(controls, DEMO_GUIDE_CONTENT_X, y + 25.0F);
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

    /** Creates an amber break ring and shards at a player whose active longbow charge was interrupted. */
    public void addDisruptionParticles(float x, float y) {
        final int disruptionColor = app.color(232, 192, 96);
        final ParticleBuilder builder = commonParticleSet.getBuilder()
                .initialize()
                .position(x, y)
                .particleColor(disruptionColor);
        commonParticleSet.getParticleList().add(builder
                .type(3)
                .particleSize(GameConstants.DISRUPT_RING_SIZE)
                .weight(GameConstants.DISRUPT_RING_STROKE)
                .lifespanSecond(0.35F)
                .build());

        for (int index = 0; index < GameConstants.DISRUPT_PARTICLE_COUNT; index++) {
            commonParticleSet.getParticleList().add(builder
                    .type(1)
                    .polarVelocity(app.random(TWO_PI), app.random(1.5F, 4.5F))
                    .particleSize(GameConstants.DISRUPT_PARTICLE_SIZE)
                    .lifespanSecond(0.3F)
                    .build());
        }
    }
}
