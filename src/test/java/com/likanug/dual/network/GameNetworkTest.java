package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameNetworkTest {

    @Test
    void receiverThreadUpdatesRemoteInputAndDisconnects() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            byte flags = NetworkMessage.encodeInput(true, false, true, false, true, false);
            byte[] packet = new byte[] {
                    NetworkMessage.TYPE_INPUT,
                    flags,
                    NetworkMessage.TYPE_DISCONNECT
            };

            TestNetwork network = new TestNetwork();
            network.channel = pair.local;
            network.connected = true;

            network.startReceiverThread();
            writeFully(pair.remote, packet);
            waitUntilDisconnected(network, 1000);

            KeyInput remote = network.getRemoteInput();
            assertTrue(remote.isUpPressed);
            assertFalse(remote.isDownPressed);
            assertTrue(remote.isLeftPressed);
            assertFalse(remote.isRightPressed);
            assertTrue(remote.isZPressed);
            assertFalse(remote.isXPressed);
            assertTrue(network.isDisconnected());
        }
    }

    @Test
    void sendInputEncodesMessageAsTypeAndFlags() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            TestNetwork network = new TestNetwork();
            network.channel = pair.local;
            network.connected = true;

            KeyInput keyInput = new KeyInput();
            keyInput.isUpPressed = true;
            keyInput.isRightPressed = true;
            keyInput.isXPressed = true;

            network.sendInput(keyInput);

            byte[] written = readExactly(pair.remote, 2, 1000);
            assertTrue(written[0] == NetworkMessage.TYPE_INPUT);

            byte flags = written[1];
            assertTrue(NetworkMessage.isUp(flags));
            assertFalse(NetworkMessage.isDown(flags));
            assertFalse(NetworkMessage.isLeft(flags));
            assertTrue(NetworkMessage.isRight(flags));
            assertFalse(NetworkMessage.isZ(flags));
            assertTrue(NetworkMessage.isX(flags));
        }
    }

    @Test
    void disconnectWritesDisconnectMessage() throws Exception {
        try (ChannelPair pair = createConnectedPair()) {
            TestNetwork network = new TestNetwork();
            network.channel = pair.local;
            network.connected = true;

            network.disconnect();

            byte[] written = readExactly(pair.remote, 1, 1000);
            assertTrue(written[0] == NetworkMessage.TYPE_DISCONNECT);
            assertTrue(network.isDisconnected());
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
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("timed out while reading test data");
            }
            int read = channel.read(buffer);
            if (read < 0) throw new IOException("channel closed while reading");
            if (read == 0) Thread.onSpinWait();
        }
        return buffer.array();
    }

    private static void waitUntilDisconnected(GameNetwork network, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (network.isDisconnected()) return;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new AssertionError("receiver thread did not stop within timeout");
    }

    private static class TestNetwork extends GameNetwork {
    }

    private record ChannelPair(SocketChannel local, SocketChannel remote) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            local.close();
            remote.close();
        }
    }
}
