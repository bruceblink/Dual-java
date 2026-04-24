package com.likanug.dual.network;

import java.io.*;
import java.net.*;

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
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);

                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                // 握手：等待 Server 发送 TYPE_START + 种子
                int type = in.read();
                if (type == NetworkMessage.TYPE_START) {
                    int seed = in.readInt();
                    setSharedSeed(seed);

                    // 回应确认
                    out.writeByte(NetworkMessage.TYPE_START_ACK);
                    out.flush();

                    socket.setSoTimeout(0);
                    connected = true;
                    startReceiverThread();
                } else {
                    errorMessage = "握手失败（意外消息: " + type + "）";
                }

            } catch (SocketTimeoutException e) {
                errorMessage = "连接或握手超时";
            } catch (IOException e) {
                errorMessage = e.getMessage();
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
