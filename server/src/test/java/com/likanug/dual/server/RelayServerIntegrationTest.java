package com.likanug.dual.server;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelayServerIntegrationTest {

    @Test
    void relayForwardsInputAndDisconnectAfterHandshake() throws Exception {
        int port = allocateFreePort();
        RelayServer server = new RelayServer(port);

        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
            }
        }, "relay-server-test");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServerStart(server);

        try (Socket playerA = new Socket()) {
            playerA.connect(new InetSocketAddress("127.0.0.1", port), 2000);
            playerA.setSoTimeout(2000);
            try (Socket playerB = new Socket()) {
                playerB.connect(new InetSocketAddress("127.0.0.1", port), 2000);
                playerB.setSoTimeout(2000);

                DataInputStream inA = new DataInputStream(playerA.getInputStream());
                DataOutputStream outA = new DataOutputStream(playerA.getOutputStream());
                DataInputStream inB = new DataInputStream(playerB.getInputStream());
                DataOutputStream outB = new DataOutputStream(playerB.getOutputStream());

                int typeA = inA.readUnsignedByte();
                int seedA = inA.readInt();
                int typeB = inB.readUnsignedByte();
                int seedB = inB.readInt();

                assertEquals(NetworkProtocol.TYPE_START & 0xFF, typeA);
                assertEquals(NetworkProtocol.TYPE_START & 0xFF, typeB);
                assertEquals(seedA, seedB);

                outA.writeByte(NetworkProtocol.TYPE_START_ACK);
                outA.flush();
                outB.writeByte(NetworkProtocol.TYPE_START_ACK);
                outB.flush();

                Thread.sleep(60);

                byte flags = 0x15;
                outA.writeByte(NetworkProtocol.TYPE_INPUT);
                outA.writeByte(flags);
                outA.flush();

                int forwardedType = inB.readUnsignedByte();
                int forwardedFlags = inB.readUnsignedByte();
                assertEquals(NetworkProtocol.TYPE_INPUT & 0xFF, forwardedType);
                assertEquals(flags & 0xFF, forwardedFlags);

                outA.writeByte(NetworkProtocol.TYPE_DISCONNECT);
                outA.flush();

                int disconnectType = inB.readUnsignedByte();
                assertEquals(NetworkProtocol.TYPE_DISCONNECT & 0xFF, disconnectType);
            }
        } finally {
            server.stop();
            serverThread.join(2000);
        }
    }

    private static int allocateFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForServerStart(RelayServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (server.isRunning()) return;
            Thread.sleep(10);
        }
        throw new AssertionError("server did not start in time");
    }
}
