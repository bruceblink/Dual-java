package com.likanug.dual.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
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

    private Selector selector;
    private ServerSocketChannel serverChannel;

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
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        log.info("Dual relay server listening on port " + port);

        while (running) {
            try {
                selector.select();
            } catch (IOException e) {
                if (!running) break;
                log.warning("Select error: " + e.getMessage());
                continue;
            }

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) continue;

                try {
                    if (key.isValid() && key.isAcceptable()) {
                        handleAccept();
                    }
                    if (key.isValid() && key.isReadable()) {
                        handleRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    if (key.attachment() instanceof ConnectionContext ctx) {
                        closeContext(ctx, false);
                    } else {
                        log.warning("IO error: " + e.getMessage());
                    }
                }
            }

            rooms.removeIf(RelayRoom::isClosed);
            if (pendingRoom != null && pendingRoom.isClosed()) {
                pendingRoom = null;
            }
        }

        closeAll();
        log.info("Server stopped.");
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        if (channel == null) return;

        channel.configureBlocking(false);
        channel.socket().setTcpNoDelay(true);

        RelayRoom room;
        Side side;

        if (pendingRoom == null || pendingRoom.isClosed()) {
            int id = roomIdCounter.getAndIncrement();
            room = new RelayRoom(id);
            rooms.add(room);
            pendingRoom = room;
            side = Side.A;
            log.info("[Room " + id + "] Created. Waiting for second player...");
        } else {
            room = pendingRoom;
            side = Side.B;
            pendingRoom = null;
            log.info("[Room " + room.getRoomId() + "] Full. Starting handshake.");
        }

        ConnectionContext ctx = new ConnectionContext(channel, room, side);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ, ctx);
        ctx.key = key;
        room.attach(side, ctx);

        if (side == Side.B) {
            room.startHandshake();
        }

        if (room.isHandshakeReady()) {
            enqueueStart(room.playerA);
            enqueueStart(room.playerB);
        }

        log.info("New connection from " + channel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        ConnectionContext ctx = (ConnectionContext) key.attachment();
        int read = ctx.channel.read(ctx.readBuffer);
        if (read < 0) {
            closeContext(ctx, false);
            return;
        }
        if (read == 0) return;

        ctx.readBuffer.flip();
        while (ctx.readBuffer.hasRemaining()) {
            if (ctx.expectingType) {
                ctx.currentType = ctx.readBuffer.get();
                ctx.expectingType = false;
                if (ctx.currentType == NetworkProtocol.TYPE_INPUT) {
                    continue;
                }
                processTypeOnlyMessage(ctx, ctx.currentType);
                if (ctx.room.isClosed()) break;
                ctx.expectingType = true;
            } else {
                if (ctx.currentType == NetworkProtocol.TYPE_INPUT) {
                    if (!ctx.readBuffer.hasRemaining()) break;
                    byte flags = ctx.readBuffer.get();
                    processInputMessage(ctx, flags);
                    if (ctx.room.isClosed()) break;
                    ctx.expectingType = true;
                } else {
                    ctx.expectingType = true;
                }
            }
        }
        ctx.readBuffer.compact();
    }

    private void processTypeOnlyMessage(ConnectionContext ctx, byte type) throws IOException {
        RelayRoom room = ctx.room;

        if (type == NetworkProtocol.TYPE_START_ACK) {
            room.markAck(ctx.side);
            if (room.isRelayPhase()) {
                log.info("[Room " + room.getRoomId() + "] Handshake complete. Entering relay mode.");
            }
            return;
        }

        if (type == NetworkProtocol.TYPE_DISCONNECT) {
            forwardDisconnectAndClose(ctx);
            return;
        }

        if (room.isWaitingAck()) {
            closeContext(ctx, false);
        }
    }

    private void processInputMessage(ConnectionContext ctx, byte flags) throws IOException {
        RelayRoom room = ctx.room;
        if (!room.isRelayPhase()) return;

        ConnectionContext peer = room.peerOf(ctx.side);
        if (peer == null || peer.closed) return;

        ByteBuffer frame = ByteBuffer.allocate(NetworkProtocol.INPUT_MSG_LEN);
        frame.put(NetworkProtocol.TYPE_INPUT);
        frame.put(flags);
        frame.flip();
        enqueueWrite(peer, frame);
    }

    private void enqueueStart(ConnectionContext ctx) {
        if (ctx == null || ctx.closed) return;

        ByteBuffer start = ByteBuffer.allocate(5);
        start.put(NetworkProtocol.TYPE_START);
        start.putInt(ctx.room.getSeed());
        start.flip();
        enqueueWrite(ctx, start);
    }

    private void enqueueDisconnect(ConnectionContext ctx) {
        if (ctx == null || ctx.closed) return;

        ByteBuffer frame = ByteBuffer.allocate(1);
        frame.put(NetworkProtocol.TYPE_DISCONNECT);
        frame.flip();
        enqueueWrite(ctx, frame);
    }

    private void enqueueWrite(ConnectionContext ctx, ByteBuffer frame) {
        ctx.pendingWrites.add(frame);
        if (ctx.key != null && ctx.key.isValid()) {
            ctx.key.interestOps(ctx.key.interestOps() | SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ConnectionContext ctx = (ConnectionContext) key.attachment();

        while (!ctx.pendingWrites.isEmpty()) {
            ByteBuffer head = ctx.pendingWrites.peek();
            ctx.channel.write(head);
            if (head.hasRemaining()) break;
            ctx.pendingWrites.poll();
        }

        if (ctx.pendingWrites.isEmpty()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            if (ctx.closeAfterWrite) {
                closeContext(ctx, true);
            }
        }
    }

    private void forwardDisconnectAndClose(ConnectionContext source) {
        RelayRoom room = source.room;
        ConnectionContext peer = room.peerOf(source.side);

        if (peer != null && !peer.closed) {
            enqueueDisconnect(peer);
            peer.closeAfterWrite = true;
        }

        source.closeAfterWrite = true;
        if (source.pendingWrites.isEmpty()) {
            closeContext(source, true);
        }
    }

    private void closeContext(ConnectionContext ctx, boolean normal) {
        if (ctx.closed) return;
        ctx.closed = true;

        try {
            if (ctx.key != null) ctx.key.cancel();
        } catch (Exception ignored) {
        }
        try {
            ctx.channel.close();
        } catch (IOException ignored) {
        }

        RelayRoom room = ctx.room;
        ConnectionContext peer = room.peerOf(ctx.side);
        room.close();

        if (peer != null && !peer.closed) {
            if (normal) {
                peer.closeAfterWrite = true;
                if (peer.pendingWrites.isEmpty()) closeContext(peer, true);
            } else {
                enqueueDisconnect(peer);
                peer.closeAfterWrite = true;
            }
        }
    }

    private void closeAll() {
        for (RelayRoom room : rooms) {
            room.close();
        }
        try {
            if (serverChannel != null) serverChannel.close();
        } catch (IOException ignored) {
        }
        try {
            if (selector != null) selector.close();
        } catch (IOException ignored) {
        }
    }

    /** Shuts the server down gracefully. */
    public void stop() {
        running = false;
        if (selector != null) selector.wakeup();
        try {
            if (serverChannel != null) serverChannel.close();
        } catch (IOException ignored) {
        }
    }

    public int getPort()         { return port; }
    public boolean isRunning()   { return running; }
    public int getActiveRooms()  {
        rooms.removeIf(RelayRoom::isClosed);
        return rooms.size();
    }

    enum Side {
        A, B
    }

    static final class ConnectionContext {
        final SocketChannel channel;
        final RelayRoom room;
        final Side side;
        final ByteBuffer readBuffer = ByteBuffer.allocate(256);
        final Queue<ByteBuffer> pendingWrites = new ArrayDeque<>();

        SelectionKey key;
        boolean expectingType = true;
        byte currentType = -1;
        boolean closeAfterWrite = false;
        boolean closed = false;

        ConnectionContext(SocketChannel channel, RelayRoom room, Side side) {
            this.channel = channel;
            this.room = room;
            this.side = side;
        }
    }
}
