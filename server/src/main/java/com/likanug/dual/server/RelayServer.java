package com.likanug.dual.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * TCP relay server for Dual.
 * <p>
 * Accepts incoming connections on the configured port.
 * The first connection creates a pending room; the second connection
 * completes the room, triggering the handshake and relay mode.
 * Multiple simultaneous rooms are supported.
 */
public class RelayServer {

    private static final Logger log = Logger.getLogger(RelayServer.class.getName());

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    /** Pending room waiting for a second player. At most one at a time. */
    private RelayRoom pendingRoom = null;

    private final AtomicInteger roomIdCounter = new AtomicInteger(1);

    /** All rooms (for monitoring / cleanup). */
    private final List<RelayRoom> rooms = new ArrayList<>();

    public RelayServer(int port) {
        this.port = port;
    }

    /**
     * Starts the server on the current thread (blocking).
     * Call {@link #stop()} from another thread to shut it down.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log.info("Dual relay server listening on port " + port);

        while (running) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (!running) break; // stopped intentionally
                log.warning("Accept error: " + e.getMessage());
                continue;
            }

            socket.setTcpNoDelay(true);
            log.info("New connection from " + socket.getRemoteSocketAddress());

            // Clean up fully closed rooms
            rooms.removeIf(RelayRoom::isClosed);

            if (pendingRoom == null || pendingRoom.isClosed()) {
                // First player – create a new room
                int id = roomIdCounter.getAndIncrement();
                pendingRoom = new RelayRoom(id, socket);
                rooms.add(pendingRoom);
                log.info("[Room " + id + "] Created. Waiting for second player...");
            } else {
                // Second player – complete the pending room
                pendingRoom.addPlayer(socket);
                pendingRoom = null;
            }
        }

        log.info("Server stopped.");
    }

    /** Shuts the server down gracefully. */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }

    public int getPort()         { return port; }
    public boolean isRunning()   { return running; }
    public int getActiveRooms()  {
        rooms.removeIf(RelayRoom::isClosed);
        return rooms.size();
    }
}
