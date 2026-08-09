package com.likanug.dual;

import com.likanug.dual.game.GameSystem;
import com.likanug.dual.game.ArenaLayout;
import com.likanug.dual.inputDevice.KeyBindings;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.inputDevice.LocalInputRouter;
import com.likanug.dual.playerEngine.AiDifficulty;
import com.likanug.dual.network.GameNetwork;
import com.likanug.dual.network.NetworkClient;
import com.likanug.dual.network.NetworkServer;
import processing.core.PApplet;
import processing.core.PFont;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class App extends PApplet {

    // ──────────────────────────────────────────────
    // 常量
    // ──────────────────────────────────────────────
    public static final int FPS = 60;
    public static final int DEFAULT_WINDOW_WIDTH = 1920;
    public static final int DEFAULT_WINDOW_HEIGHT = 1080;
    public static final int INTERNAL_CANVAS_WIDTH = 1280;
    public static final int INTERNAL_CANVAS_HEIGHT = 720;
    public static PFont smallFont, largeFont;

    static final int DEFAULT_PORT = 7777;
    static final int MIN_PORT = 1;
    static final int MAX_PORT = 65535;
    private static final int MAX_HOST_LENGTH = 253;

    // ──────────────────────────────────────────────
    // 游戏核心状态
    // ──────────────────────────────────────────────
    private KeyInput currentKeyInput;
    private KeyInput secondKeyInput;
    private LocalInputRouter localInputRouter;
    private GameSystem system;
    private boolean paused;

    // ──────────────────────────────────────────────
    // 联机大厅状态机
    // ──────────────────────────────────────────────
    private enum NetworkMode {
        NONE,        // 演示 / 本地模式（默认）
        LOCAL_MODE_MENU, // 选择人机或本地双人
        LOBBY_MENU,  // 选择"开房"或"加入"
        HOSTING,     // 房主等待对方连接
        JOINING,     // 加入方输入 IP / Port
        CONNECTING,  // 正在连接中…
        ONLINE       // 联机对战进行中
    }

    private NetworkMode networkMode = NetworkMode.NONE;
    private GameNetwork activeNetwork = null;

    // HOSTING 用
    private NetworkServer networkServer = null;

    // JOINING / CONNECTING 用
    private NetworkClient networkClient = null;
    private StringBuilder joinIP       = new StringBuilder("127.0.0.1");
    private StringBuilder joinPortStr  = new StringBuilder(String.valueOf(DEFAULT_PORT));
    private boolean       editingIP    = true;  // true=正在编辑IP, false=编辑Port
    private String        connectError = null;

    // ──────────────────────────────────────────────
    // Getters / Setters
    // ──────────────────────────────────────────────
    public KeyInput getCurrentKeyInput() { return currentKeyInput; }
    public void setCurrentKeyInput(KeyInput k) { this.currentKeyInput = k; }
    public KeyInput getSecondKeyInput() { return secondKeyInput; }
    public void setSecondKeyInput(KeyInput k) { this.secondKeyInput = k; }
    public GameSystem getSystem() { return system; }
    public void setSystem(GameSystem s) { this.system = s; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { this.paused = p; }
    public boolean isLocalModeMenuVisible() { return networkMode == NetworkMode.LOCAL_MODE_MENU; }

    // ──────────────────────────────────────────────
    // Processing 生命周期
    // ──────────────────────────────────────────────
    @Override
    public void settings() {
        size(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
    }

    @Override
    public void setup() {
        surface.setResizable(true);
        frameRate(FPS);
        final String fontFilePath = "Lato-Regular.ttf";
        smallFont = createFont(fontFilePath, 20.0F, true);
        largeFont = createFont(fontFilePath, 96.0F, true);
        textFont(smallFont, 96);
        textAlign(CENTER, CENTER);
        rectMode(CENTER);
        ellipseMode(CENTER);
        localInputRouter = new LocalInputRouter(KeyBindings.playerOne(), KeyBindings.playerTwo());
        currentKeyInput = localInputRouter.getPlayerOneInput();
        secondKeyInput = localInputRouter.getPlayerTwoInput();
        newGame(true, true);
    }

    public void newGame(boolean demo, boolean instruction) {
        newGame(demo, instruction, AiDifficulty.STANDARD);
    }

    public void newGame(boolean demo, boolean instruction, AiDifficulty aiDifficulty) {
        newGame(demo, instruction, aiDifficulty, ArenaLayout.open());
    }

    public void newGame(boolean demo, boolean instruction, AiDifficulty aiDifficulty, ArenaLayout arenaLayout) {
        clearLocalInputs();
        // 如果正在联机，先断线
        if (activeNetwork != null) {
            activeNetwork.disconnect();
            activeNetwork = null;
        }
        networkMode = NetworkMode.NONE;
        system = new GameSystem(demo, instruction, this, false, aiDifficulty, arenaLayout);
    }

    /** Starts a local two-player match while preserving both configured keyboard snapshots. */
    public void newLocalGame(boolean instruction) {
        clearLocalInputs();
        if (activeNetwork != null) {
            activeNetwork.disconnect();
            activeNetwork = null;
        }
        networkMode = NetworkMode.NONE;
        system = new GameSystem(false, instruction, this, true);
    }

    /** Opens the visible local mode chooser while keeping the demo system available for cancel. */
    public void openLocalModeMenu() {
        clearLocalInputs();
        networkMode = NetworkMode.LOCAL_MODE_MENU;
    }

    /** Clears both local snapshots so a menu key cannot leak into the next combat state. */
    public void clearLocalInputs() {
        if (localInputRouter != null) localInputRouter.clear();
        else {
            if (currentKeyInput != null) currentKeyInput.clear();
            if (secondKeyInput != null) secondKeyInput.clear();
        }
    }

    /** 启动联机对战（握手完成后调用） */
    public void startOnlineGame(GameNetwork network) {
        this.activeNetwork = network;
        this.networkMode   = NetworkMode.ONLINE;
        this.system        = new GameSystem(network, this);
    }

    @Override
    public void draw() {
        background(GameConstants.ARENA_BACKGROUND_COLOR);
        pushMatrix();
        translate(canvasOffsetX(), canvasOffsetY());
        scale(canvasScale());
        switch (networkMode) {
            case NONE       -> system.run();
            case LOCAL_MODE_MENU -> drawLocalModeMenu();
            case LOBBY_MENU -> drawLobbyMenu();
            case HOSTING    -> drawHosting();
            case JOINING    -> drawJoining();
            case CONNECTING -> drawConnecting();
            case ONLINE     -> drawOnlineGame();
        }
        popMatrix();
    }

    float canvasScale() {
        return Math.min(
                width / (float) INTERNAL_CANVAS_WIDTH,
                height / (float) INTERNAL_CANVAS_HEIGHT
        );
    }

    float canvasOffsetX() {
        return (width - INTERNAL_CANVAS_WIDTH * canvasScale()) * 0.5F;
    }

    float canvasOffsetY() {
        return (height - INTERNAL_CANVAS_HEIGHT * canvasScale()) * 0.5F;
    }

    boolean isInsideCanvas(float screenX, float screenY) {
        float scale = canvasScale();
        return screenX >= canvasOffsetX()
                && screenX < canvasOffsetX() + INTERNAL_CANVAS_WIDTH * scale
                && screenY >= canvasOffsetY()
                && screenY < canvasOffsetY() + INTERNAL_CANVAS_HEIGHT * scale;
    }

    /**
     * 将窗口坐标换算为固定 1280 x 720 竞技场坐标；调用方应先确认鼠标位于画布内。
     */
    CanvasPoint toCanvasPoint(float screenX, float screenY) {
        float scale = canvasScale();
        return new CanvasPoint(
                (screenX - canvasOffsetX()) / scale,
                (screenY - canvasOffsetY()) / scale
        );
    }

    record CanvasPoint(float x, float y) {}

    // ──────────────────────────────────────────────
    // 各模式 draw 方法
    // ──────────────────────────────────────────────

    private void drawOnlineGame() {
        if (activeNetwork != null && activeNetwork.isDisconnected()) {
            // 对方断线 → 回到演示
            activeNetwork = null;
            networkMode   = NetworkMode.NONE;
            newGame(true, true);
            return;
        }
        // 每帧把本地输入发往对端
        if (activeNetwork != null) {
            activeNetwork.sendInput(currentKeyInput);
        }
        system.run();
    }

    private void drawLobbyMenu() {
        pushMatrix();
        translate(INTERNAL_CANVAS_WIDTH * 0.5f, INTERNAL_CANVAS_HEIGHT * 0.5f);
        pushStyle();
        textFont(smallFont, 28);

        fill(0);
        text("Online Multiplayer", 0, -120);

        textFont(smallFont, 22);
        fill(50);
        text("H  -  Host (wait for opponent)", 0, -30);
        text("J  -  Join (enter host IP)",      0,  20);
        text("ESC - Back",                       0,  90);

        popStyle();
        popMatrix();
    }

    private void drawHosting() {
        // 轮询：连接建立后自动进入游戏
        if (networkServer != null && networkServer.isConnected()) {
            startOnlineGame(networkServer);
            networkServer = null;
            return;
        }
        if (networkServer != null && networkServer.getErrorMessage() != null) {
            // 错误：回到大厅菜单
            networkServer = null;
            networkMode   = NetworkMode.LOBBY_MENU;
            return;
        }

        List<String> localIPs = getLocalIPv4Addresses();

        pushMatrix();
        translate(INTERNAL_CANVAS_WIDTH * 0.5f, INTERNAL_CANVAS_HEIGHT * 0.5f);
        pushStyle();
        textFont(smallFont, 22);
        fill(0);
        text("Waiting for opponent...", 0, -100);

        textFont(smallFont, 18);
        fill(60);
        text("Port: " + DEFAULT_PORT, 0, -50);
        if (localIPs.isEmpty()) {
            text("Your IP: (unavailable)", 0, -10);
        } else {
            int yOff = -10;
            for (String ip : localIPs) {
                text("Your IP: " + ip, 0, yOff);
                yOff += 26;
            }
        }
        text("(Share your IP with opponent)", 0, 80);

        textFont(smallFont, 16);
        fill(100);
        text("ESC - Cancel", 0, 130);

        popStyle();
        popMatrix();
    }

    private void drawJoining() {
        pushMatrix();
        translate(INTERNAL_CANVAS_WIDTH * 0.5f, INTERNAL_CANVAS_HEIGHT * 0.5f);
        pushStyle();
        textFont(smallFont, 22);
        fill(0);
        text("Join Game", 0, -120);

        textFont(smallFont, 18);
        fill(50);

        // IP input field
        String ipLabel = "Host: " + joinIP.toString() + (editingIP ? "_" : "");
        fill(editingIP ? color(0) : color(120));
        text(ipLabel, 0, -50);

        // Port input field
        String portLabel = "Port: " + joinPortStr.toString() + (!editingIP ? "_" : "");
        fill(!editingIP ? color(0) : color(120));
        text(portLabel, 0, 0);

        textFont(smallFont, 16);
        fill(80);
        text("Tab - switch field | Enter - connect | ESC - back", 0, 60);

        if (connectError != null) {
            fill(200, 0, 0);
            text(connectError, 0, 100);
        }

        popStyle();
        popMatrix();
    }

    /** Draws an explicit mode choice so starting a match does not depend on an unexplained key. */
    private void drawLocalModeMenu() {
        pushMatrix();
        translate(INTERNAL_CANVAS_WIDTH * 0.5f, INTERNAL_CANVAS_HEIGHT * 0.5f);
        pushStyle();
        textFont(smallFont, 28);
        fill(0);
        text("Choose Game Mode", 0, -120);
        textFont(smallFont, 22);
        text("1  Basic AI", 0, -60);
        text("2  Standard AI", 0, -15);
        text("3  Advanced AI", 0, 30);
        text("4  Local 2P", 0, 75);
        text("5  Standard AI + Cover", 0, 120);
        text("ESC    Back to demo", 0, 175);
        popStyle();
        popMatrix();
    }

    private void drawConnecting() {
        // 轮询连接状态
        if (networkClient != null && networkClient.isConnected()) {
            startOnlineGame(networkClient);
            networkClient = null;
            connectError  = null;
            return;
        }
        if (networkClient != null && !networkClient.isConnecting()
                && networkClient.getErrorMessage() != null) {
            connectError  = networkClient.getErrorMessage();
            networkClient = null;
            networkMode   = NetworkMode.JOINING;
            return;
        }

        pushMatrix();
        translate(INTERNAL_CANVAS_WIDTH * 0.5f, INTERNAL_CANVAS_HEIGHT * 0.5f);
        pushStyle();
        textFont(smallFont, 22);
        fill(0);
        text("Connecting...", 0, -40);

        if (connectError != null) {
            textFont(smallFont, 16);
            fill(200, 0, 0);
            text("Connection failed: " + connectError, 0, 20);
        }

        popStyle();
        popMatrix();
    }

    // ──────────────────────────────────────────────
    // Processing 输入事件
    // ──────────────────────────────────────────────
    @Override
    public void mousePressed() {
        if (networkMode != NetworkMode.NONE || !isInsideCanvas(mouseX, mouseY)) return;

        if (system.isDemoPlay()) {
            system.setShowsInstructionWindow(!system.isShowsInstructionWindow());
            return;
        }

        updateMouseAim();
        if (mouseButton == LEFT) currentKeyInput.setMouseShotPressed(true);
        if (mouseButton == RIGHT) currentKeyInput.setMouseLongShotPressed(true);
    }

    @Override
    public void mouseReleased() {
        if (mouseButton == LEFT) currentKeyInput.setMouseShotPressed(false);
        if (mouseButton == RIGHT) currentKeyInput.setMouseLongShotPressed(false);
    }

    @Override
    public void mouseMoved() {
        updateMouseAim();
    }

    @Override
    public void mouseDragged() {
        if (!isInsideCanvas(mouseX, mouseY)) {
            currentKeyInput.releaseMouseButtons();
            return;
        }
        updateMouseAim();
    }

    /** 只接受本地竞技场内的鼠标位置，避免边框区域改变瞄准方向。 */
    private void updateMouseAim() {
        if (networkMode != NetworkMode.NONE || !isInsideCanvas(mouseX, mouseY)) return;
        CanvasPoint point = toCanvasPoint(mouseX, mouseY);
        currentKeyInput.updateMouseAim(point.x(), point.y());
    }

    @Override
    public void focusLost() {
        // AWT 可能在 setup() 创建输入对象前发送失焦事件；此时没有按键状态需要释放。
        clearLocalInputs();
    }

    @Override
    public void keyPressed() {
        // 联机大厅优先拦截
        switch (networkMode) {
            case NONE -> handleKeyNone();
            case LOCAL_MODE_MENU -> handleKeyLocalModeMenu();
            case LOBBY_MENU -> handleKeyLobbyMenu();
            case HOSTING -> {
                if (key == ESC) { key = 0; cancelHosting(); }
            }
            case JOINING -> handleKeyJoining();
            case CONNECTING -> { /* 等待结果，不处理输入 */ }
            case ONLINE -> handleKeyOnline();
        }
    }

    @Override
    public void keyReleased() {
        if (networkMode != NetworkMode.NONE && networkMode != NetworkMode.ONLINE) return;
        applyLocalKey(false);
    }

    // ──────────────────────────────────────────────
    // 各模式按键处理
    // ──────────────────────────────────────────────

    private void handleKeyNone() {
        if (key == 'n' || key == 'N') {
            networkMode = NetworkMode.LOBBY_MENU;
            return;
        }
        // 原有逻辑
        if (key != CODED) {
            if (key == 'p' || key == 'P') {
                if (paused) loop(); else noLoop();
                paused = !paused;
                return;
            }
            applyLocalKey(true);
            return;
        }
        applyLocalKey(true);
    }

    private void handleKeyOnline() {
        applyLocalKey(true);
    }

    private void handleKeyLocalModeMenu() {
        if (key == ESC) {
            key = 0;
            networkMode = NetworkMode.NONE;
            if (system != null && system.isDemoPlay()) system.setShowsInstructionWindow(true);
            return;
        }
        if (key == '1') {
            newGame(false, false, AiDifficulty.BASIC);
            return;
        }
        if (key == '2' || key == 'h' || key == 'H') {
            newGame(false, false, AiDifficulty.STANDARD);
            return;
        }
        if (key == '3') {
            newGame(false, false, AiDifficulty.ADVANCED);
            return;
        }
        if (key == '5') {
            newGame(false, false, AiDifficulty.STANDARD, ArenaLayout.centralCover());
            return;
        }
        if (key == '4' || key == 'l' || key == 'L') newLocalGame(false);
    }

    private void handleKeyLobbyMenu() {
        if (key == ESC)           { key = 0; networkMode = NetworkMode.NONE; return; }
        if (key == 'h' || key == 'H') startHosting();
        if (key == 'j' || key == 'J') {
            connectError = null;
            networkMode = NetworkMode.JOINING;
        }
    }

    /** Routes a key transition to both local snapshots while menus keep their own controls. */
    private void applyLocalKey(boolean pressed) {
        if (localInputRouter != null) localInputRouter.handleKey(key, keyCode, pressed);
    }

    private void handleKeyJoining() {
        if (key == ESC) {
            key = 0;
            connectError = null;
            networkMode  = NetworkMode.LOBBY_MENU;
            return;
        }
        if (keyCode == TAB) {
            editingIP = !editingIP;
            return;
        }
        if (key == ENTER || key == RETURN) {
            startConnecting();
            return;
        }
        if (key == BACKSPACE) {
            connectError = null;
            if (editingIP && joinIP.length() > 0)
                joinIP.deleteCharAt(joinIP.length() - 1);
            else if (!editingIP && joinPortStr.length() > 0)
                joinPortStr.deleteCharAt(joinPortStr.length() - 1);
            return;
        }
        // 仅允许可见 ASCII 字符
        if (key >= 32 && key < 127) {
            connectError = null;
            if (editingIP) {
                if (key > 32 && joinIP.length() < MAX_HOST_LENGTH) joinIP.append(key);
            } else {
                // Port 只允许数字，最多 5 位
                if (Character.isDigit(key) && joinPortStr.length() < 5)
                    joinPortStr.append(key);
            }
        }
    }

    // ──────────────────────────────────────────────
    // 联机辅助方法
    // ──────────────────────────────────────────────

    private void startHosting() {
        networkServer = new NetworkServer();
        networkServer.startListening(DEFAULT_PORT);
        networkMode = NetworkMode.HOSTING;
    }

    private void cancelHosting() {
        if (networkServer != null) {
            networkServer.stopListening();
            networkServer = null;
        }
        networkMode = NetworkMode.LOBBY_MENU;
    }

    private void startConnecting() {
        String host = joinIP.toString().trim();
        if (host.isEmpty()) {
            connectError = "Host is required.";
            editingIP = true;
            networkMode = NetworkMode.JOINING;
            return;
        }

        int port;
        try {
            port = parsePort(joinPortStr.toString());
        } catch (IllegalArgumentException e) {
            connectError = e.getMessage();
            editingIP = false;
            networkMode = NetworkMode.JOINING;
            return;
        }
        connectError  = null;
        networkClient = new NetworkClient();
        networkClient.connect(host, port);
        networkMode = NetworkMode.CONNECTING;
    }

    static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port < MIN_PORT || port > MAX_PORT) {
                throw new IllegalArgumentException("Port must be 1-65535.");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be 1-65535.", e);
        }
    }

    /** 获取本机所有非回环 IPv4 地址 */
    private List<String> getLocalIPv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) result.add(addr.getHostAddress());
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    // ──────────────────────────────────────────────
    // 入口
    // ──────────────────────────────────────────────
    public static void main(String[] args) {
        App.main("com.likanug.dual.App");
    }
}
