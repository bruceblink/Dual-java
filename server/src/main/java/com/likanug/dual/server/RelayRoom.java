package com.likanug.dual.server;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;

/**
 * A relay room holds exactly two connected players.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>Player A connects → room is created and waits for player B.</li>
 *   <li>Player B connects → {@link #addPlayer} is called; handshake begins.</li>
 *   <li>Both players send {@code TYPE_START_ACK} → room enters relay mode.</li>
 *   <li>Each player's input is forwarded verbatim to the other.</li>
 *   <li>Either player disconnects → the other is notified and the room closes.</li>
 * </ol>
 */
public class RelayRoom {

    private static final Logger log = Logger.getLogger(RelayRoom.class.getName());

    private final int roomId;
    private final Socket playerA;
    private Socket playerB;

    private volatile boolean closed = false;

    public RelayRoom(int roomId, Socket playerA) {
        this.roomId  = roomId;
        this.playerA = playerA;
    }

    public boolean isFull() { return playerB != null; }
    public boolean isClosed() { return closed; }

    /**
     * Called when the second player connects.
     * Performs the handshake on the calling thread, then starts two relay threads.
     */
    public void addPlayer(Socket socket) {
        this.playerB = socket;
        log.info("[Room " + roomId + "] Full. Starting handshake.");

        Thread t = new Thread(this::runHandshakeAndRelay, "room-" + roomId);
        t.setDaemon(true);
        t.start();
    }

    // ──────────────────────────────────────────────
    // Private
    // ──────────────────────────────────────────────

    private void runHandshakeAndRelay() {
        try (DataOutputStream outA = new DataOutputStream(new BufferedOutputStream(playerA.getOutputStream()));
             DataInputStream  inA  = new DataInputStream(new BufferedInputStream(playerA.getInputStream()));
             DataOutputStream outB = new DataOutputStream(new BufferedOutputStream(playerB.getOutputStream()));
             DataInputStream  inB  = new DataInputStream(new BufferedInputStream(playerB.getInputStream()))) {

            // Generate shared seed and send TYPE_START to both players simultaneously
            int seed = new Random().nextInt();
            log.info("[Room " + roomId + "] Sending TYPE_START with seed=" + seed);

            sendStart(outA, seed);
            sendStart(outB, seed);

            // Wait for TYPE_START_ACK from both
            waitAck(inA, "A");
            waitAck(inB, "B");
            log.info("[Room " + roomId + "] Handshake complete. Entering relay mode.");

            // Start bidirectional relay (one thread each direction)
            Thread aToB = new Thread(() -> relay(inA, outB, "A→B"), "relay-" + roomId + "-AtB");
            Thread bToA = new Thread(() -> relay(inB, outA, "B→A"), "relay-" + roomId + "-BtA");
            aToB.setDaemon(true);
            bToA.setDaemon(true);
            aToB.start();
            bToA.start();

            // Wait for both relay threads to finish
            aToB.join();
            bToA.join();

        } catch (Exception e) {
            if (!closed) {
                log.warning("[Room " + roomId + "] Error during handshake: " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    private void sendStart(DataOutputStream out, int seed) throws IOException {
        out.writeByte(NetworkProtocol.TYPE_START);
        out.writeInt(seed);
        out.flush();
    }

    private void waitAck(DataInputStream in, String label) throws IOException {
        int b = in.read();
        if (b != NetworkProtocol.TYPE_START_ACK) {
            throw new IOException("Expected TYPE_START_ACK from player " + label + " but got " + b);
        }
        log.fine("[Room " + roomId + "] Got ACK from player " + label);
    }

    /**
     * Reads messages from {@code in} and forwards them to {@code out}.
     * Stops on any read error or TYPE_DISCONNECT message.
     */
    private void relay(DataInputStream in, DataOutputStream out, String direction) {
        try {
            while (!closed) {
                int type = in.read();
                if (type < 0) break; // EOF

                byte typeByte = (byte) type;

                switch (typeByte) {
                    case NetworkProtocol.TYPE_INPUT -> {
                        byte flags = in.readByte();
                        out.writeByte(NetworkProtocol.TYPE_INPUT);
                        out.writeByte(flags);
                        out.flush();
                    }
                    case NetworkProtocol.TYPE_DISCONNECT -> {
                        log.info("[Room " + roomId + "] [" + direction + "] Disconnect received.");
                        // Forward disconnect to the other side
                        try {
                            out.writeByte(NetworkProtocol.TYPE_DISCONNECT);
                            out.flush();
                        } catch (IOException ignored) {}
                        return;
                    }
                    default -> log.fine("[Room " + roomId + "] [" + direction + "] Unknown message type: " + type);
                }
            }
        } catch (IOException e) {
            if (!closed) {
                log.info("[Room " + roomId + "] [" + direction + "] Connection lost: " + e.getMessage());
            }
        }
    }

    private void close() {
        if (closed) return;
        closed = true;
        log.info("[Room " + roomId + "] Closed.");
        try { playerA.close(); } catch (IOException ignored) {}
        try { if (playerB != null) playerB.close(); } catch (IOException ignored) {}
    }
}
