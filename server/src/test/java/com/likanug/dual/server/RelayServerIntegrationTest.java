package com.likanug.dual.server;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayServerIntegrationTest {

    @Test
    void relayServerPairsClientsAndForwardsResultAndRematchFrames() throws Exception {
        int port = findFreePort();
        RelayServer server = new RelayServer(port);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
                // stop() closes the listening socket during normal test teardown.
            }
        }, "relay-integration-test");
        serverThread.setDaemon(true);
        serverThread.start();
        waitUntil(server::isRunning, 1000);

        try (Socket playerA = new Socket(InetAddress.getLoopbackAddress(), port);
             Socket playerB = new Socket(InetAddress.getLoopbackAddress(), port)) {
            playerB.setSoTimeout(2000);
            DataInputStream inA = new DataInputStream(playerA.getInputStream());
            DataOutputStream outA = new DataOutputStream(playerA.getOutputStream());
            DataInputStream inB = new DataInputStream(playerB.getInputStream());
            DataOutputStream outB = new DataOutputStream(playerB.getOutputStream());

            assertEquals(NetworkProtocol.TYPE_START, inA.read());
            int seedA = inA.readInt();
            assertEquals(NetworkProtocol.TYPE_START, inB.read());
            int seedB = inB.readInt();
            assertEquals(seedA, seedB);

            outA.writeByte(NetworkProtocol.TYPE_START_ACK);
            outA.flush();
            outB.writeByte(NetworkProtocol.TYPE_START_ACK);
            outB.flush();

            byte[] result = new byte[]{NetworkProtocol.TYPE_ROUND_RESULT, 1, 0, 1, 0, 0};
            outA.write(result);
            outA.flush();
            byte[] receivedResult = readExactly(inB, NetworkProtocol.ROUND_RESULT_MSG_LEN);
            assertArrayEquals(result, receivedResult);

            byte[] rematch = new byte[]{NetworkProtocol.TYPE_REMATCH_REQUEST, 1, 0};
            outB.write(rematch);
            outB.flush();
            byte[] receivedRematch = readExactly(inA, NetworkProtocol.REMATCH_REQUEST_MSG_LEN);
            assertArrayEquals(rematch, receivedRematch);

            byte[] nextMatchResult = new byte[]{NetworkProtocol.TYPE_ROUND_RESULT, 1, 0, 1, 0, 1};
            outA.write(nextMatchResult);
            outA.flush();
            byte[] receivedNextMatchResult = readExactly(inB, NetworkProtocol.ROUND_RESULT_MSG_LEN);
            assertArrayEquals(nextMatchResult, receivedNextMatchResult);

            byte[] nextMatchRematch = new byte[]{NetworkProtocol.TYPE_REMATCH_REQUEST, 1, 1};
            outB.write(nextMatchRematch);
            outB.flush();
            byte[] receivedNextMatchRematch = readExactly(inA, NetworkProtocol.REMATCH_REQUEST_MSG_LEN);
            assertArrayEquals(nextMatchRematch, receivedNextMatchRematch);
        } finally {
            server.stop();
            serverThread.join(1000);
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static byte[] readExactly(DataInputStream input, int length) throws IOException {
        byte[] result = new byte[length];
        input.readFully(result);
        return result;
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], actual[index], "byte " + index + " differs");
        }
    }

    private static void waitUntil(Check check, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (check.value()) return;
            Thread.onSpinWait();
        }
        assertTrue(check.value(), "condition did not become true within timeout");
    }

    private interface Check {
        boolean value();
    }
}
