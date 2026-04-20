package com.likanug.dual.network;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * 房主（Host）端网络实现。
 * 调用 {@link #startListening} 后在后台线程监听，等待一位客户端连接。
 * 握手时生成随机种子并发送给客户端，保证双方物理运算一致。
 */
public class NetworkServer extends GameNetwork {

    private volatile ServerSocket serverSocket;
    private volatile boolean      waiting      = false;
    private volatile String       errorMessage = null;

    /**
     * 在后台线程开始监听指定端口。
     * 通过 {@link #isConnected()} / {@link #isWaiting()} / {@link #getErrorMessage()} 轮询状态。
     */
    public void startListening(int port) {
        waiting = true;
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();           // 阻塞直到有客户端连接
                socket.setTcpNoDelay(true);               // 关闭 Nagle，降低延迟

                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                // 握手：Server 生成种子并发送 TYPE_START + 4字节种子
                int seed = new Random().nextInt();
                setSharedSeed(seed);
                out.writeByte(NetworkMessage.TYPE_START);
                out.writeInt(seed);
                out.flush();

                // 等待客户端确认
                int ack = in.read();
                if (ack == NetworkMessage.TYPE_START_ACK) {
                    connected = true;
                    startReceiverThread();
                } else {
                    errorMessage = "握手失败（意外响应: " + ack + "）";
                }

            } catch (IOException e) {
                if (!disconnected) errorMessage = e.getMessage();
            } finally {
                waiting = false;
                try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
            }
        }, "network-server");
        t.setDaemon(true);
        t.start();
    }

    /** 取消等待（在 HOSTING 界面按 ESC 时调用） */
    public void stopListening() {
        disconnected = true;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    public boolean isWaiting()      { return waiting; }
    public String getErrorMessage() { return errorMessage; }
}
