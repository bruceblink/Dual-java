package com.likanug.dual.server;

import org.junit.jupiter.api.Test;

import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayRoomTest {

    @Test
    void startHandshakeTransitionsToWaitingAckWhenRoomIsFull() throws Exception {
        RelayRoom room = new RelayRoom(1);
        try (SocketChannel channelA = SocketChannel.open();
             SocketChannel channelB = SocketChannel.open()) {
            RelayServer.ConnectionContext ctxA = new RelayServer.ConnectionContext(channelA, room, RelayServer.Side.A);
            RelayServer.ConnectionContext ctxB = new RelayServer.ConnectionContext(channelB, room, RelayServer.Side.B);

            room.attach(RelayServer.Side.A, ctxA);
            room.attach(RelayServer.Side.B, ctxB);
            room.startHandshake();

            assertTrue(room.isWaitingAck());
            assertNotEquals(0, room.getSeed());
            assertFalse(room.isRelayPhase());
        }
    }

    @Test
    void relayPhaseRequiresAckFromBothSides() throws Exception {
        RelayRoom room = new RelayRoom(2);
        try (SocketChannel channelA = SocketChannel.open();
             SocketChannel channelB = SocketChannel.open()) {
            RelayServer.ConnectionContext ctxA = new RelayServer.ConnectionContext(channelA, room, RelayServer.Side.A);
            RelayServer.ConnectionContext ctxB = new RelayServer.ConnectionContext(channelB, room, RelayServer.Side.B);

            room.attach(RelayServer.Side.A, ctxA);
            room.attach(RelayServer.Side.B, ctxB);
            room.startHandshake();

            room.markAck(RelayServer.Side.A);
            assertFalse(room.isRelayPhase());

            room.markAck(RelayServer.Side.B);
            assertTrue(room.isRelayPhase());
        }
    }
}
