package com.likanug.dual.game;

import com.likanug.dual.App;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.network.GameNetwork;
import com.likanug.dual.network.NetworkMessage;
import com.likanug.dual.network.NetworkRematchRequest;
import com.likanug.dual.network.NetworkRoundResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSystemNetworkSyncTest {

    @Test
    void matchingMirroredResultAndRematchRequestUnlockTheNextRound() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            TestNetwork network = new TestNetwork();
            network.attach(pair.local);

            App app = new App();
            app.setCurrentKeyInput(new KeyInput());
            GameSystem system = new GameSystem(network, app);
            MatchScore.RoundResult localResult = system.recordRoundWin(PlayerSide.ONE);
            readExactly(pair.remote, NetworkMessage.ROUND_RESULT_MSG_LEN, 1000);

            NetworkRoundResult remoteResult = new NetworkRoundResult(1, NetworkRoundResult.SIDE_TWO, 0, 1, false);
            NetworkRematchRequest request = new NetworkRematchRequest(1, false);
            writeFully(pair.remote, concat(
                    NetworkMessage.encodeRoundResult(remoteResult),
                    NetworkMessage.encodeRematchRequest(request)));
            waitUntil(() -> network.getRemoteRematchRequest() != null, 1000);

            assertTrue(system.isNetworkRoundResultConsistent(localResult));
            assertTrue(system.isNetworkRematchReady(localResult));
            network.disconnect();
        }
    }

    @Test
    void divergentRemoteScoreCannotUnlockRematch() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            TestNetwork network = new TestNetwork();
            network.attach(pair.local);

            App app = new App();
            app.setCurrentKeyInput(new KeyInput());
            GameSystem system = new GameSystem(network, app);
            MatchScore.RoundResult localResult = system.recordRoundWin(PlayerSide.ONE);
            readExactly(pair.remote, NetworkMessage.ROUND_RESULT_MSG_LEN, 1000);

            NetworkRoundResult divergentResult = new NetworkRoundResult(1, NetworkRoundResult.SIDE_TWO, 0, 0, false);
            writeFully(pair.remote, concat(
                    NetworkMessage.encodeRoundResult(divergentResult),
                    NetworkMessage.encodeRematchRequest(new NetworkRematchRequest(1, false))));
            waitUntil(() -> network.getRemoteRematchRequest() != null, 1000);

            assertFalse(system.isNetworkRoundResultConsistent(localResult));
            assertFalse(system.isNetworkRematchReady(localResult));
            assertTrue(system.hasNetworkRoundResultMismatch(localResult));
            network.disconnect();
        }
    }

    @Test
    void fullMatchRematchClearsSnapshotsForTheNextMatchOnTheSameConnection() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            TestNetwork network = new TestNetwork();
            network.attach(pair.local);

            App app = new App();
            app.setCurrentKeyInput(new KeyInput());
            GameSystem system = new GameSystem(network, app);

            for (int round = 1; round <= 3; round++) {
                MatchScore.RoundResult localResult = system.recordRoundWin(PlayerSide.ONE);
                NetworkRoundResult sent = NetworkMessage.decodeRoundResult(
                        readExactly(pair.remote, NetworkMessage.ROUND_RESULT_MSG_LEN, 1000));
                assertEquals(round, sent.roundNumber());

                NetworkRoundResult remoteResult = new NetworkRoundResult(
                        round,
                        NetworkRoundResult.SIDE_TWO,
                        0,
                        round,
                        round == 3);
                writeFully(pair.remote, NetworkMessage.encodeRoundResult(remoteResult));
                waitUntil(() -> remoteResult.equals(network.getRemoteRoundResult()), 1000);

                if (round == 3) {
                    NetworkRematchRequest request = new NetworkRematchRequest(round, true);
                    writeFully(pair.remote, NetworkMessage.encodeRematchRequest(request));
                    waitUntil(() -> request.equals(network.getRemoteRematchRequest()), 1000);
                    assertTrue(system.isNetworkRoundResultConsistent(localResult));
                    assertTrue(system.isNetworkRematchReady(localResult));
                    system.resetMatch();
                    assertTrue(network.getRemoteRoundResult() == null);
                    assertTrue(network.getRemoteRematchRequest() == null);
                }
            }

            system.recordRoundWin(PlayerSide.ONE);
            NetworkRoundResult nextMatchResult = NetworkMessage.decodeRoundResult(
                    readExactly(pair.remote, NetworkMessage.ROUND_RESULT_MSG_LEN, 1000));
            assertEquals(1, nextMatchResult.roundNumber());
            assertEquals(1, nextMatchResult.playerOneWins());
            network.disconnect();
        }
    }

    private static ChannelPair createConnectedPair() throws Exception {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0));
            InetSocketAddress address = (InetSocketAddress) server.getLocalAddress();

            SocketChannel local = SocketChannel.open();
            local.connect(new InetSocketAddress("127.0.0.1", address.getPort()));
            SocketChannel remote = server.accept();
            local.configureBlocking(false);
            remote.configureBlocking(false);
            return new ChannelPair(local, remote);
        }
    }

    private static void writeFully(SocketChannel channel, byte[] payload) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        while (buffer.hasRemaining()) {
            int written = channel.write(buffer);
            if (written < 0) throw new IOException("channel closed while writing");
            if (written == 0) Thread.onSpinWait();
        }
    }

    private static byte[] readExactly(SocketChannel channel, int length, long timeoutMs) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (buffer.hasRemaining()) {
            if (System.nanoTime() >= deadline) throw new AssertionError("timed out while reading test data");
            int read = channel.read(buffer);
            if (read < 0) throw new IOException("channel closed while reading");
            if (read == 0) Thread.onSpinWait();
        }
        return buffer.array();
    }

    private static void waitUntil(Check check, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (check.value()) return;
            Thread.onSpinWait();
        }
        throw new AssertionError("condition did not become true within timeout");
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) length += array.length;
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private interface Check {
        boolean value();
    }

    private static class TestNetwork extends GameNetwork {
        void attach(SocketChannel socketChannel) {
            channel = socketChannel;
            connected = true;
            startReceiverThread();
        }
    }

    private record ChannelPair(SocketChannel local, SocketChannel remote) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            local.close();
            remote.close();
        }
    }
}
