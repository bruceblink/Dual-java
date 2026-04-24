package com.likanug.dual.server;

import java.util.Random;

/**
 * A relay room holds exactly two connected players.
 */
public class RelayRoom {

    enum Phase {
        WAITING_SECOND_PLAYER,
        WAITING_ACKS,
        RELAYING,
        CLOSED
    }

    private final int roomId;
    private final Random random = new Random();

    RelayServer.ConnectionContext playerA;
    RelayServer.ConnectionContext playerB;

    private volatile boolean closed = false;
    private volatile Phase phase = Phase.WAITING_SECOND_PLAYER;

    private int seed;
    private boolean ackA = false;
    private boolean ackB = false;

    public RelayRoom(int roomId) {
        this.roomId = roomId;
    }

    public int getRoomId() {
        return roomId;
    }

    public synchronized void attach(RelayServer.Side side, RelayServer.ConnectionContext ctx) {
        if (side == RelayServer.Side.A) {
            playerA = ctx;
        } else {
            playerB = ctx;
        }
    }

    public synchronized void startHandshake() {
        if (closed) return;
        if (playerA == null || playerB == null) return;
        if (phase != Phase.WAITING_SECOND_PLAYER) return;

        seed = random.nextInt();
        ackA = false;
        ackB = false;
        phase = Phase.WAITING_ACKS;
    }

    public synchronized boolean isHandshakeReady() {
        return !closed && phase == Phase.WAITING_ACKS && playerA != null && playerB != null;
    }

    public synchronized int getSeed() {
        return seed;
    }

    public synchronized void markAck(RelayServer.Side side) {
        if (phase != Phase.WAITING_ACKS) return;
        if (side == RelayServer.Side.A) ackA = true;
        else ackB = true;

        if (ackA && ackB) {
            phase = Phase.RELAYING;
        }
    }

    public synchronized boolean isWaitingAck() {
        return phase == Phase.WAITING_ACKS;
    }

    public synchronized boolean isRelayPhase() {
        return phase == Phase.RELAYING;
    }

    public synchronized RelayServer.ConnectionContext peerOf(RelayServer.Side side) {
        return side == RelayServer.Side.A ? playerB : playerA;
    }

    public synchronized boolean isClosed() {
        return closed || phase == Phase.CLOSED;
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        phase = Phase.CLOSED;
    }
}
