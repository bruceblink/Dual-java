package com.likanug.dual.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 加入方（Client）端网络实现。
 * 调用 {@link #connect} 后在后台线程发起连接。
 */
public class NetworkClient extends GameNetwork {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    private volatile boolean connecting  = false;
    private volatile String  errorMessage = null;

    /**
     * 在后台线程连接到指定主机和端口。
     * 通过 {@link #isConnected()} / {@link #isConnecting()} / {@link #getErrorMessage()} 轮询状态。
     */
    public void connect(String host, int port) {
        connecting = true;
        Thread t = new Thread(() -> {
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(new InetSocketAddress(host, port));

                long connectDeadline = System.nanoTime() + CONNECT_TIMEOUT_MS * 1_000_000L;
                while (!channel.finishConnect()) {
                    if (System.nanoTime() >= connectDeadline) {
                        throw new IOException("连接或握手超时");
                    }
                    waitForNonBlockingIO();
                }

                channel.socket().setTcpNoDelay(true);

                long handshakeDeadline = System.nanoTime() + HANDSHAKE_TIMEOUT_MS * 1_000_000L;
                ByteBuffer typeBuffer = ByteBuffer.allocate(1);
                if (!readFullyWithDeadline(channel, typeBuffer, handshakeDeadline)) {
                    throw new IOException("连接或握手超时");
                }
                byte type = typeBuffer.get(0);

                if (type == NetworkMessage.TYPE_START) {
                    ByteBuffer seedBuffer = ByteBuffer.allocate(4);
                    if (!readFullyWithDeadline(channel, seedBuffer, handshakeDeadline)) {
                        throw new IOException("连接或握手超时");
                    }
                    seedBuffer.flip();
                    int seed = seedBuffer.getInt();
                    setSharedSeed(seed);

                    writeFully(ByteBuffer.wrap(new byte[]{NetworkMessage.TYPE_START_ACK}));

                    connected = true;
                    startReceiverThread();
                } else {
                    errorMessage = "握手失败（意外消息: " + type + "）";
                    disconnected = true;
                    closeChannelQuietly();
                }

            } catch (IOException e) {
                errorMessage = e.getMessage();
                disconnected = true;
                closeChannelQuietly();
            } finally {
                connecting = false;
            }
        }, "network-client");
        t.setDaemon(true);
        t.start();
    }

    public boolean isConnecting()   { return connecting; }
    public String getErrorMessage() { return errorMessage; }
}
