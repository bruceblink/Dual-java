package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.LockSupport;

/**
 * P2P 网络连接基类。
 * <p>
 * 主线程调用 {@link #sendInput} 发送本地按键，后台接收线程更新 {@link #remoteInputFlags}，
 * 主线程通过 {@link #getRemoteInput} 取得最新远端输入。
 */
public abstract class GameNetwork {

    private static final long NON_BLOCKING_WAIT_NANOS = 1_000_000L;

    protected volatile SocketChannel channel;
    protected volatile boolean connected    = false;
    protected volatile boolean disconnected = false;

    private final Object writeLock = new Object();

    /** 最新收到的对方按键状态（后台线程写，主线程读，volatile 保证可见性） */
    private volatile byte remoteInputFlags = 0;

    /** 双方共享的随机数种子，用于保证物理运算一致性 */
    private volatile int sharedSeed = 0;

    /** Latest accepted remote round result; older or duplicate round numbers are ignored. */
    private volatile NetworkRoundResult remoteRoundResult;
    /** Latest accepted rematch request; older round requests cannot reset a newer result. */
    private volatile NetworkRematchRequest remoteRematchRequest;

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

    /** Sends one completed round snapshot while keeping input frames independent and cheap. */
    public void sendRoundResult(NetworkRoundResult result) {
        if (!connected || channel == null || disconnected || result == null) return;
        try {
            synchronized (writeLock) {
                writeFully(ByteBuffer.wrap(NetworkMessage.encodeRoundResult(result)));
            }
        } catch (IOException e) {
            disconnected = true;
        }
    }

    /** Sends a single transition request after the local player confirms the result overlay. */
    public void sendRematchRequest(NetworkRematchRequest request) {
        if (!connected || channel == null || disconnected || request == null) return;
        try {
            synchronized (writeLock) {
                writeFully(ByteBuffer.wrap(NetworkMessage.encodeRematchRequest(request)));
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
    public NetworkRoundResult getRemoteRoundResult() { return remoteRoundResult; }
    public NetworkRematchRequest getRemoteRematchRequest() { return remoteRematchRequest; }

    // ──────────────────────────────────────────────
    // 子类调用
    // ──────────────────────────────────────────────

    protected void setSharedSeed(int seed) { this.sharedSeed = seed; }

    protected void writeFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int written = channel.write(buffer);
            if (written < 0) throw new IOException("channel closed");
            if (written == 0) {
                waitForNonBlockingIO();
            }
        }
    }

    protected static boolean readFullyWithDeadline(SocketChannel channel, ByteBuffer buffer, long deadlineNanos) throws IOException {
        while (buffer.hasRemaining()) {
            if (System.nanoTime() >= deadlineNanos) return false;
            int read = channel.read(buffer);
            if (read < 0) throw new IOException("channel closed");
            if (read == 0) {
                waitForNonBlockingIO();
            }
        }
        return true;
    }

    protected static void waitForNonBlockingIO() {
        LockSupport.parkNanos(NON_BLOCKING_WAIT_NANOS);
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
                        waitForNonBlockingIO();
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
                        case NetworkMessage.TYPE_ROUND_RESULT -> {
                            ByteBuffer resultBuffer = ByteBuffer.allocate(NetworkMessage.ROUND_RESULT_MSG_LEN - 1);
                            if (!readFullyWithDeadline(channel, resultBuffer, System.nanoTime() + 5_000_000_000L)) {
                                continue;
                            }
                            byte[] frame = new byte[NetworkMessage.ROUND_RESULT_MSG_LEN];
                            frame[0] = NetworkMessage.TYPE_ROUND_RESULT;
                            resultBuffer.flip();
                            resultBuffer.get(frame, 1, frame.length - 1);
                            NetworkRoundResult result = NetworkMessage.decodeRoundResult(frame);
                            NetworkRoundResult previous = remoteRoundResult;
                            if (previous == null || result.roundNumber() > previous.roundNumber()) {
                                remoteRoundResult = result;
                            }
                        }
                        case NetworkMessage.TYPE_REMATCH_REQUEST -> {
                            ByteBuffer requestBuffer = ByteBuffer.allocate(NetworkMessage.REMATCH_REQUEST_MSG_LEN - 1);
                            if (!readFullyWithDeadline(channel, requestBuffer, System.nanoTime() + 5_000_000_000L)) {
                                continue;
                            }
                            byte[] frame = new byte[NetworkMessage.REMATCH_REQUEST_MSG_LEN];
                            frame[0] = NetworkMessage.TYPE_REMATCH_REQUEST;
                            requestBuffer.flip();
                            requestBuffer.get(frame, 1, frame.length - 1);
                            NetworkRematchRequest request = NetworkMessage.decodeRematchRequest(frame);
                            NetworkRematchRequest previous = remoteRematchRequest;
                            if (previous == null || request.roundNumber() > previous.roundNumber()) {
                                remoteRematchRequest = request;
                            }
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
