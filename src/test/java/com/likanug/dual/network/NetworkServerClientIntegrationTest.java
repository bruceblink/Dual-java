package com.likanug.dual.network;

import com.likanug.dual.inputDevice.KeyInput;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkServerClientIntegrationTest {

    @Test
    void directHostAndJoinShareSeedAndRelayGameplayFrames() throws Exception {
        int port = findFreePort();
        NetworkServer host = new NetworkServer();
        NetworkClient join = new NetworkClient();
        try {
            host.startListening(port);
            waitUntil(host::isWaiting, 1000);
            join.connect("127.0.0.1", port);
            waitUntil(() -> host.isConnected() && join.isConnected(), 2000);

            assertEquals(host.getSharedSeed(), join.getSharedSeed());

            KeyInput input = new KeyInput();
            input.isUpPressed = true;
            input.isZPressed = true;
            join.sendInput(input, true, -0.625F);
            waitUntil(() -> host.getRemoteInput().isUpPressed && host.getRemoteInput().isZPressed, 1000);
            waitUntil(host::hasRemoteAim, 1000);
            assertEquals(NetworkMessage.quantizeAimAngle(-0.625F),
                    NetworkMessage.quantizeAimAngle(host.getRemoteAimAngle()));

            for (int match = 0; match < 2; match++) {
                for (int round = 1; round <= 3; round++) {
                    NetworkRoundResult result = new NetworkRoundResult(
                            round, NetworkRoundResult.SIDE_ONE, round, 0, round == 3);
                    join.sendRoundResult(result);
                    waitUntil(() -> result.equals(host.getRemoteRoundResult()), 1000);
                }
                assertTrue(host.isConnected());
                if (match == 0) {
                    NetworkRematchRequest request = new NetworkRematchRequest(3, true);
                    join.sendRematchRequest(request);
                    waitUntil(() -> request.equals(host.getRemoteRematchRequest()), 1000);
                    host.resetRemoteMatchState();
                }
            }
            assertTrue(host.getRemoteRoundResult().matchComplete());
        } finally {
            join.disconnect();
            host.stopListening();
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
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
