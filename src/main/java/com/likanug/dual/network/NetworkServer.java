package com.likanug.dual.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

/**
 * 房主（Host）端网络实现。
 * 调用 {@link #startListening} 后在后台线程监听，等待一位客户端连接。
 * 握手时生成随机种子并发送给客户端，保证双方物理运算一致。
 */
public class NetworkServer extends GameNetwork {

    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    private volatile ServerSocketChannel serverSocketChannel;
    private volatile boolean             waiting      = false;
    private volatile String              errorMessage = null;

    /**
     * 在后台线程开始监听指定端口。
     * 通过 {@link #isConnected()} / {@link #isWaiting()} / {@link #getErrorMessage()} 轮询状态。
     */
    public void startListening(int port) {
        waiting = true;
        Thread t = new Thread(() -> {
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.bind(new InetSocketAddress(port));

                long handshakeDeadline = System.nanoTime() + HANDSHAKE_TIMEOUT_MS * 1_000_000L;
                SocketChannel acceptedChannel;
                while (true) {
                    if (disconnected) return;
                    if (System.nanoTime() >= handshakeDeadline) {
                        throw new IOException("连接或握手超时");
                    }
                    acceptedChannel = serverSocketChannel.accept();
                    if (acceptedChannel != null) break;
                    Thread.onSpinWait();
                }

                channel = acceptedChannel;
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);

                int seed = new Random().nextInt();
                setSharedSeed(seed);

                ByteBuffer startMessage = ByteBuffer.allocate(5);
                startMessage.put(NetworkMessage.TYPE_START);
                startMessage.putInt(seed);
                startMessage.flip();
                writeFully(startMessage);

                ByteBuffer ackBuffer = ByteBuffer.allocate(1);
                if (!readFullyWithDeadline(channel, ackBuffer, handshakeDeadline)) {
                    throw new IOException("连接或握手超时");
                }

                int ack = ackBuffer.get(0) & 0xFF;
                if (ack == NetworkMessage.TYPE_START_ACK) {
                    connected = true;
                    startReceiverThread();
                } else {
                    errorMessage = "握手失败（意外响应: " + ack + "）";
                    disconnected = true;
                    closeChannelQuietly();
                }

            } catch (IOException e) {
                if (!disconnected) errorMessage = e.getMessage();
                disconnected = true;
                closeChannelQuietly();
            } finally {
                waiting = false;
                try {
                    if (serverSocketChannel != null) serverSocketChannel.close();
                } catch (IOException ignored) {
                }
            }
        }, "network-server");
        t.setDaemon(true);
        t.start();
    }

    /** 取消等待（在 HOSTING 界面按 ESC 时调用） */
    public void stopListening() {
        disconnected = true;
        closeChannelQuietly();
        try {
            if (serverSocketChannel != null) serverSocketChannel.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isWaiting()      { return waiting; }
    public String getErrorMessage() { return errorMessage; }
}
