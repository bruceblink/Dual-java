package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * P2P 网络连接基类。
 * <p>
 * 主线程调用 {@link #sendInput} 发送本地按键，后台接收线程更新 {@link #remoteInputFlags}，
 * 主线程通过 {@link #getRemoteInput} 取得最新远端输入。
 */
public abstract class GameNetwork {

    protected volatile SocketChannel channel;
    protected volatile boolean connected    = false;
    protected volatile boolean disconnected = false;

    private final Object writeLock = new Object();

    /** 最新收到的对方按键状态（后台线程写，主线程读，volatile 保证可见性） */
    private volatile byte remoteInputFlags = 0;

    /** 双方共享的随机数种子，用于保证物理运算一致性 */
    private volatile int sharedSeed = 0;

    // ──────────────────────────────────────────────
    // 主线程调用
    // ──────────────────────────────────────────────

    /** 发送本地 KeyInput 到对端（每帧调用） */
    public void sendInput(KeyInput keyInput) {
        if (!connected || channel == null || disconnected) return;
        byte flags = NetworkMessage.encodeInput(
                keyInput.isUpPressed, keyInput.isDownPressed,
                keyInput.isLeftPressed, keyInput.isRightPressed,
                keyInput.isZPressed, keyInput.isXPressed);
        try {
            synchronized (writeLock) {
                writeFully(ByteBuffer.wrap(new byte[]{NetworkMessage.TYPE_INPUT, flags}));
            }
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
            if (channel != null && channel.isOpen()) {
                synchronized (writeLock) {
                    writeFully(ByteBuffer.wrap(new byte[]{NetworkMessage.TYPE_DISCONNECT}));
                }
            }
        } catch (IOException ignored) {
        } finally {
            closeChannelQuietly();
        }
    }

    public boolean isConnected()    { return connected && !disconnected; }
    public boolean isDisconnected() { return disconnected; }
    public int getSharedSeed()      { return sharedSeed; }

    // ──────────────────────────────────────────────
    // 子类调用
    // ──────────────────────────────────────────────

    protected void setSharedSeed(int seed) { this.sharedSeed = seed; }

    protected void writeFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int written = channel.write(buffer);
            if (written < 0) throw new IOException("channel closed");
            if (written == 0) {
                Thread.onSpinWait();
            }
        }
    }

    protected static boolean readFullyWithDeadline(SocketChannel channel, ByteBuffer buffer, long deadlineNanos) throws IOException {
        while (buffer.hasRemaining()) {
            if (System.nanoTime() >= deadlineNanos) return false;
            int read = channel.read(buffer);
            if (read < 0) throw new IOException("channel closed");
            if (read == 0) {
                Thread.onSpinWait();
            }
        }
        return true;
    }

    protected void closeChannelQuietly() {
        try {
            if (channel != null) channel.close();
        } catch (IOException ignored) {
        }
    }

    /** 启动后台接收线程（握手完成后调用） */
    protected void startReceiverThread() {
        Thread t = new Thread(() -> {
            ByteBuffer oneByte = ByteBuffer.allocate(1);
            try {
                while (!disconnected) {
                    oneByte.clear();
                    int read = channel.read(oneByte);
                    if (read < 0) break;
                    if (read == 0) {
                        Thread.onSpinWait();
                        continue;
                    }

                    byte type = oneByte.get(0);
                    switch (type) {
                        case NetworkMessage.TYPE_INPUT -> {
                            oneByte.clear();
                            if (!readFullyWithDeadline(channel, oneByte, System.nanoTime() + 5_000_000_000L)) {
                                continue;
                            }
                            remoteInputFlags = oneByte.get(0);
                        }
                        case NetworkMessage.TYPE_DISCONNECT -> {
                            return;
                        }
                        default -> {
                            // 忽略未知消息
                        }
                    }
                }
            } catch (IOException ignored) {
                // 连接关闭
            } finally {
                disconnected = true;
                closeChannelQuietly();
            }
        }, "network-receiver");
        t.setDaemon(true);
        t.start();
    }
}
