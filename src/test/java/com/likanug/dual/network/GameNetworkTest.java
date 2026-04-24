package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameNetworkTest {

    @Test
    void receiverThreadUpdatesRemoteInputAndDisconnects() {
        byte flags = NetworkMessage.encodeInput(true, false, true, false, true, false);
        byte[] packet = new byte[] {
                NetworkMessage.TYPE_INPUT,
                flags,
                NetworkMessage.TYPE_DISCONNECT
        };

        TestNetwork network = new TestNetwork();
        network.in = new DataInputStream(new ByteArrayInputStream(packet));
        network.connected = true;

        network.startReceiverThread();
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

    @Test
    void sendInputEncodesMessageAsTypeAndFlags() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TestNetwork network = new TestNetwork();
        network.out = new DataOutputStream(bos);
        network.connected = true;

        KeyInput keyInput = new KeyInput();
        keyInput.isUpPressed = true;
        keyInput.isRightPressed = true;
        keyInput.isXPressed = true;

        network.sendInput(keyInput);

        byte[] written = bos.toByteArray();
        assertTrue(written.length >= 2);
        assertTrue(written[0] == NetworkMessage.TYPE_INPUT);

        KeyInput remote = new KeyInput();
        byte flags = written[1];
        remote.isUpPressed = NetworkMessage.isUp(flags);
        remote.isDownPressed = NetworkMessage.isDown(flags);
        remote.isLeftPressed = NetworkMessage.isLeft(flags);
        remote.isRightPressed = NetworkMessage.isRight(flags);
        remote.isZPressed = NetworkMessage.isZ(flags);
        remote.isXPressed = NetworkMessage.isX(flags);

        assertTrue(remote.isUpPressed);
        assertFalse(remote.isDownPressed);
        assertFalse(remote.isLeftPressed);
        assertTrue(remote.isRightPressed);
        assertFalse(remote.isZPressed);
        assertTrue(remote.isXPressed);
    }

    @Test
    void disconnectWritesDisconnectMessage() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        TestNetwork network = new TestNetwork();
        network.out = new DataOutputStream(bos);

        network.disconnect();

        byte[] written = bos.toByteArray();
        assertTrue(written.length >= 1);
        assertTrue(written[0] == NetworkMessage.TYPE_DISCONNECT);
        assertTrue(network.isDisconnected());
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
}
