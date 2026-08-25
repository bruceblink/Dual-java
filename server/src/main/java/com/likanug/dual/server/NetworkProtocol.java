package com.likanug.dual.server;

/**
 * Wire protocol constants shared between the relay server and game clients.
 * <p>
 * Message frame layout:
 * <pre>
 *   TYPE_INPUT      [0x00][flags(1 byte)][aimAngle(2 bytes)] – per-frame input state
 *   TYPE_START      [0x01][seed(4 bytes)]  – server → both clients, carries shared RNG seed
 *   TYPE_START_ACK  [0x02]                 – client → server, handshake confirmation
 *   TYPE_DISCONNECT [0x03]                 – either side notifying graceful disconnect
 *   TYPE_ROUND_RESULT [0x04][5 bytes]      – completed round and score snapshot
 *   TYPE_REMATCH_REQUEST [0x05][2 bytes]   – request the same round transition
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
 *   bit 6 = HAS_AIM (the following uint16 angle is valid)
 *   bit 7 is reserved and must remain zero
 * </pre>
 *
 * Keep this file in sync with the client-side NetworkMessage.java.
 */
public final class NetworkProtocol {

    public static final byte TYPE_INPUT      = 0x00;
    public static final byte TYPE_START      = 0x01;
    public static final byte TYPE_START_ACK  = 0x02;
    public static final byte TYPE_DISCONNECT = 0x03;
    public static final byte TYPE_ROUND_RESULT = 0x04;
    public static final byte TYPE_REMATCH_REQUEST = 0x05;

    /** Total byte length of a TYPE_INPUT message (type + flags + uint16 angle). */
    public static final int INPUT_MSG_LEN = 4;
    public static final int ROUND_RESULT_MSG_LEN = 6;
    public static final int REMATCH_REQUEST_MSG_LEN = 3;

    private NetworkProtocol() {}
}
