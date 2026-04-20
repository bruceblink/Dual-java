package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;

import java.io.*;
import java.net.Socket;

/**
 * P2P 网络连接基类。
 * <p>
 * 主线程调用 {@link #sendInput} 发送本地按键，后台接收线程更新 {@link #remoteInputFlags}，
 * 主线程通过 {@link #getRemoteInput} 取得最新远端输入。
 */
public abstract class GameNetwork {

    protected volatile Socket socket;
    protected volatile DataOutputStream out;
    protected volatile DataInputStream in;
    protected volatile boolean connected    = false;
    protected volatile boolean disconnected = false;

    /** 最新收到的对方按键状态（后台线程写，主线程读，volatile 保证可见性） */
    private volatile byte remoteInputFlags = 0;

    /** 双方共享的随机数种子，用于保证物理运算一致性 */
    private volatile int sharedSeed = 0;

    // ──────────────────────────────────────────────
    // 主线程调用
    // ──────────────────────────────────────────────

    /** 发送本地 KeyInput 到对端（每帧调用） */
    public void sendInput(KeyInput keyInput) {
        if (!connected || out == null) return;
        try {
            out.writeByte(NetworkMessage.TYPE_INPUT);
            out.writeByte(NetworkMessage.encodeInput(
                    keyInput.isUpPressed, keyInput.isDownPressed,
                    keyInput.isLeftPressed, keyInput.isRightPressed,
                    keyInput.isZPressed, keyInput.isXPressed));
            out.flush();
        } catch (IOException e) {
            disconnected = true;
        }
    }

    /** 获取对端最新 KeyInput（如尚未收到则返回全松开状态） */
    public KeyInput getRemoteInput() {
        byte flags = remoteInputFlags;
        KeyInput ki = new KeyInput();
        ki.isUpPressed    = NetworkMessage.isUp(flags);
        ki.isDownPressed  = NetworkMessage.isDown(flags);
        ki.isLeftPressed  = NetworkMessage.isLeft(flags);
        ki.isRightPressed = NetworkMessage.isRight(flags);
        ki.isZPressed     = NetworkMessage.isZ(flags);
        ki.isXPressed     = NetworkMessage.isX(flags);
        return ki;
    }

    /** 主动断线并通知对端 */
    public void disconnect() {
        if (disconnected) return;
        disconnected = true;
        try {
            if (out != null) {
                out.writeByte(NetworkMessage.TYPE_DISCONNECT);
                out.flush();
            }
        } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected()    { return connected && !disconnected; }
    public boolean isDisconnected() { return disconnected; }
    public int getSharedSeed()      { return sharedSeed; }

    // ──────────────────────────────────────────────
    // 子类调用
    // ──────────────────────────────────────────────

    protected void setSharedSeed(int seed) { this.sharedSeed = seed; }

    /** 启动后台接收线程（握手完成后调用） */
    protected void startReceiverThread() {
        Thread t = new Thread(() -> {
            try {
                while (!disconnected) {
                    int type = in.read();
                    if (type < 0) break; // EOF
                    switch ((byte) type) {
                        case NetworkMessage.TYPE_INPUT:
                            remoteInputFlags = in.readByte();
                            break;
                        case NetworkMessage.TYPE_DISCONNECT:
                            return;
                        default:
                            // 忽略未知消息
                            break;
                    }
                }
            } catch (IOException ignored) {
                // 连接关闭
            } finally {
                disconnected = true;
            }
        }, "network-receiver");
        t.setDaemon(true);
        t.start();
    }
}
