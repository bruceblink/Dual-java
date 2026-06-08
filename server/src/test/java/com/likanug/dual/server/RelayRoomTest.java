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

class RelayRoomTest {

    @Test
    void closesRoomWhenHandshakeAckTimesOut() throws Exception {
        try (SocketPair playerA = openSocketPair();
             SocketPair playerB = openSocketPair()) {
            RelayRoom room = new RelayRoom(1, playerA.serverSide(), 100);

            room.addPlayer(playerB.serverSide());

            assertTrue(waitUntilClosed(room, 2000));
        }
    }

    @Test
    void forwardsDisconnectToOtherPlayerAndClosesRoom() throws Exception {
        try (SocketPair playerA = openSocketPair();
             SocketPair playerB = openSocketPair()) {
            playerB.clientSide().setSoTimeout(2000);
            RelayRoom room = new RelayRoom(2, playerA.serverSide(), 1000);
            room.addPlayer(playerB.serverSide());

            DataInputStream inA = new DataInputStream(playerA.clientSide().getInputStream());
            DataOutputStream outA = new DataOutputStream(playerA.clientSide().getOutputStream());
            DataInputStream inB = new DataInputStream(playerB.clientSide().getInputStream());
            DataOutputStream outB = new DataOutputStream(playerB.clientSide().getOutputStream());

            assertEquals(NetworkProtocol.TYPE_START, inA.read());
            int seedA = inA.readInt();
            assertEquals(NetworkProtocol.TYPE_START, inB.read());
            int seedB = inB.readInt();
            assertEquals(seedA, seedB);

            outA.writeByte(NetworkProtocol.TYPE_START_ACK);
            outA.flush();
            outB.writeByte(NetworkProtocol.TYPE_START_ACK);
            outB.flush();

            outA.writeByte(NetworkProtocol.TYPE_DISCONNECT);
            outA.flush();

            assertEquals(NetworkProtocol.TYPE_DISCONNECT, inB.read());
            assertTrue(waitUntilClosed(room, 2000));
        }
    }

    private static SocketPair openSocketPair() throws IOException {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Socket clientSide = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
            Socket serverSide = listener.accept();
            return new SocketPair(clientSide, serverSide);
        }
    }

    private static boolean waitUntilClosed(RelayRoom room, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (room.isClosed()) return true;
            Thread.sleep(10);
        }
        return room.isClosed();
    }

    private record SocketPair(Socket clientSide, Socket serverSide) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            IOException thrown = null;
            try {
                clientSide.close();
            } catch (IOException e) {
                thrown = e;
            }
            try {
                serverSide.close();
            } catch (IOException e) {
                if (thrown == null) thrown = e;
                else thrown.addSuppressed(e);
            }
            if (thrown != null) throw thrown;
        }
    }
}
