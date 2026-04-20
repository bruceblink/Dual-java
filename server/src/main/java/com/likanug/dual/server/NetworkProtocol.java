package com.likanug.dual.server;

/**
 * Wire protocol constants shared between the relay server and game clients.
 * <p>
 * Message frame layout:
 * <pre>
 *   TYPE_INPUT      [0x00][flags(1 byte)]  – per-frame key state from a client
 *   TYPE_START      [0x01][seed(4 bytes)]  – server → both clients, carries shared RNG seed
 *   TYPE_START_ACK  [0x02]                 – client → server, handshake confirmation
 *   TYPE_DISCONNECT [0x03]                 – either side notifying graceful disconnect
 * </pre>
 *
 * Input flags byte bit layout:
 * <pre>
 *   bit 0 = UP
 *   bit 1 = DOWN
 *   bit 2 = LEFT
 *   bit 3 = RIGHT
 *   bit 4 = Z  (weak shot)
 *   bit 5 = X  (strong shot / aim)
 * </pre>
 *
 * Keep this file in sync with the client-side NetworkMessage.java.
 */
public final class NetworkProtocol {

    public static final byte TYPE_INPUT      = 0x00;
    public static final byte TYPE_START      = 0x01;
    public static final byte TYPE_START_ACK  = 0x02;
    public static final byte TYPE_DISCONNECT = 0x03;

    /** Total byte length of a TYPE_INPUT message (type byte + flags byte). */
    public static final int INPUT_MSG_LEN = 2;

    private NetworkProtocol() {}
}
