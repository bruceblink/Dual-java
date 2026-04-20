package com.likanug.dual.game;

import com.likanug.dual.App;
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

import java.util.Random;

import static com.likanug.dual.App.FPS;
import static com.likanug.dual.App.INTERNAL_CANVAS_SIDE_WIDTH;
import static processing.core.PConstants.*;

public class GameSystem {
    private final App app;
    private final ActorGroup myGroup;
    private final ActorGroup otherGroup;
    private final ParticleSet commonParticleSet;
    private GameSystemState currentState;
    private float screenShakeValue;
    private final DamagedPlayerActorState damagedState;
    private final GameBackground currentBackground;
    private final boolean demoPlay;
    private boolean showsInstructionWindow;
    /** 用于游戏物理运算的可确定性随机数生成器（联机时双方使用相同种子保证一致） */
    private final Random gameRandom;

    public GameSystem(boolean demo, boolean instruction, App app) {
        this.app = app;
        // prepare ActorGroup
        this.myGroup = new ActorGroup();
        this.otherGroup = new ActorGroup();
        this.myGroup.setEnemyGroup(otherGroup);
        this.otherGroup.setEnemyGroup(myGroup);

        // prepare PlayerActorState
        final MovePlayerActorState moveState = new MovePlayerActorState(app);
        final DrawBowPlayerActorState drawShortbowState = new DrawShortbowPlayerActorState(app);
        final DrawBowPlayerActorState drawLongbowState = new DrawLongbowPlayerActorState(app);
        this.damagedState = new DamagedPlayerActorState(app);
        moveState.setDrawShortbowState(drawShortbowState);
        moveState.setDrawLongbowState(drawLongbowState);
        drawShortbowState.setMoveState(moveState);
        drawLongbowState.setMoveState(moveState);
        this.damagedState.setMoveState(moveState);

        // prepare PlayerActor
        PlayerEngine myEngine;
        if (demo) myEngine = new ComputerPlayerEngine(app);
        else myEngine = new HumanPlayerEngine(app.getCurrentKeyInput());
        PlayerActor myPlayer = new PlayerActor(myEngine, 255, app);
        myPlayer.setxPosition(INTERNAL_CANVAS_SIDE_WIDTH * 0.5F);
        myPlayer.setyPosition(INTERNAL_CANVAS_SIDE_WIDTH - 100);
        myPlayer.setState(moveState);
        this.myGroup.setPlayer(myPlayer);
        PlayerEngine otherEngine = new ComputerPlayerEngine(app);
        PlayerActor otherPlayer = new PlayerActor(otherEngine, 0, app);
        otherPlayer.setxPosition((float) (INTERNAL_CANVAS_SIDE_WIDTH * 0.5));
        otherPlayer.setyPosition(100);
        otherPlayer.setState(moveState);
        this.otherGroup.setPlayer(otherPlayer);

        // other
        this.commonParticleSet = new ParticleSet(2048, app);
        this.currentState = new StartGameState(app);
        this.currentBackground = new GameBackground(224, 0.1F, app);
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

        final MovePlayerActorState moveState = new MovePlayerActorState(app);
        final DrawBowPlayerActorState drawShortbowState = new DrawShortbowPlayerActorState(app);
        final DrawBowPlayerActorState drawLongbowState = new DrawLongbowPlayerActorState(app);
        this.damagedState = new DamagedPlayerActorState(app);
        moveState.setDrawShortbowState(drawShortbowState);
        moveState.setDrawLongbowState(drawLongbowState);
        drawShortbowState.setMoveState(moveState);
        drawLongbowState.setMoveState(moveState);
        this.damagedState.setMoveState(moveState);

        // 本地玩家（下方，白色）
        PlayerActor myPlayer = new PlayerActor(new HumanPlayerEngine(app.getCurrentKeyInput()), 255, app);
        myPlayer.setxPosition(INTERNAL_CANVAS_SIDE_WIDTH * 0.5F);
        myPlayer.setyPosition(INTERNAL_CANVAS_SIDE_WIDTH - 100);
        myPlayer.setState(moveState);
        this.myGroup.setPlayer(myPlayer);

        // 远端玩家（上方，黑色）
        PlayerActor otherPlayer = new PlayerActor(new NetworkPlayerEngine(network), 0, app);
        otherPlayer.setxPosition(INTERNAL_CANVAS_SIDE_WIDTH * 0.5F);
        otherPlayer.setyPosition(100);
        otherPlayer.setState(moveState);
        this.otherGroup.setPlayer(otherPlayer);

        this.commonParticleSet = new ParticleSet(2048, app);
        this.currentState = new StartGameState(app);
        this.currentBackground = new GameBackground(224, 0.1F, app);
        this.demoPlay = false;
        this.showsInstructionWindow = false;
        this.gameRandom = new Random(network.getSharedSeed());
    }

    GameSystem(App app) {
        this(false, false, app);
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

    public void run() {
        //演示模式
        if (demoPlay) {
            if (app.getCurrentKeyInput().isZPressed) {
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
        currentState.run(this);

        app.popMatrix();
        if (demoPlay && showsInstructionWindow)
            displayDemo();
    }

    public void displayDemo() {
        app.pushStyle();

        app.stroke(0);
        app.strokeWeight(2);
        app.fill(255, 240);
        app.rect(
                INTERNAL_CANVAS_SIDE_WIDTH * 0.5F,
                INTERNAL_CANVAS_SIDE_WIDTH * 0.5F,
                INTERNAL_CANVAS_SIDE_WIDTH * 0.7F,
                INTERNAL_CANVAS_SIDE_WIDTH * 0.6F
        );

        app.textFont(App.smallFont, 20);
        app.textLeading(26);
        app.textAlign(RIGHT, BASELINE);
        app.fill(0);
        app.text("Z key:", 280, 160);
        app.text("X key:", 280, 225);
        app.text("Arrow key:", 280, 310);
        app.text("N key:", 280, 390);
        app.textAlign(LEFT);
        app.text("Weak shot\n (auto aiming)", 300, 160);
        app.text("Lethal shot\n (manual aiming,\n  requires charge)", 300, 225);
        app.text("Move\n (or aim lethal shot)", 300, 310);
        app.text("Online multiplayer\n (open lobby)", 300, 390);
        app.textAlign(CENTER);
        app.text("- Press Z key to start -", INTERNAL_CANVAS_SIDE_WIDTH * 0.5F, 455);
        app.text("(Click to hide this window)", INTERNAL_CANVAS_SIDE_WIDTH * 0.5F, 490);
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
}
