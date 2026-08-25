package com.likanug.dual.network;

/**
 * 网络消息类型常量与 KeyInput 二进制编解码工具。
 * <p>
 * 消息帧格式：
 * <pre>
 *   TYPE_INPUT      [0x00][flags(1byte)][aimAngle(2bytes)] - 每帧发送按键和可选瞄准角
 *   TYPE_START      [0x01][seed(4bytes)]   - 握手：Server→Client 携带共享随机种子
 *   TYPE_START_ACK  [0x02]                 - 握手回应：Client→Server
 *   TYPE_DISCONNECT [0x03]                 - 主动断线通知
 *   TYPE_ROUND_RESULT [0x04][5 bytes]      - completed round and score snapshot
 *   TYPE_REMATCH_REQUEST [0x05][2 bytes]   - request the same round transition
 * </pre>
 * flags 字节位定义：bit0=UP, bit1=DOWN, bit2=LEFT, bit3=RIGHT, bit4=Z, bit5=X,
 * bit6=HAS_AIM；bit7 保留且必须为 0。瞄准角使用 0..65535 映射一整圈，单位为弧度。
 */
public final class NetworkMessage {

    public static final byte TYPE_INPUT      = 0x00;
    public static final byte TYPE_START      = 0x01;
    public static final byte TYPE_START_ACK  = 0x02;
    public static final byte TYPE_DISCONNECT = 0x03;
    public static final byte TYPE_ROUND_RESULT = 0x04;
    public static final byte TYPE_REMATCH_REQUEST = 0x05;
    public static final int INPUT_MSG_LEN = 4;
    public static final int ROUND_RESULT_MSG_LEN = 6;
    public static final int REMATCH_REQUEST_MSG_LEN = NetworkRematchRequest.MSG_LEN;
    public static final int INPUT_HAS_AIM_MASK = 0x40;
    private static final int INPUT_KNOWN_FLAGS_MASK = 0x7F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    /** 将 6 个按键布尔值编码为单个字节 */
    public static byte encodeInput(boolean up, boolean down, boolean left,
                                   boolean right, boolean z, boolean x) {
        int flags = 0;
        if (up)    flags |= 0x01;
        if (down)  flags |= 0x02;
        if (left)  flags |= 0x04;
        if (right) flags |= 0x08;
        if (z)     flags |= 0x10;
        if (x)     flags |= 0x20;
        return (byte) flags;
    }

    public static boolean isUp(byte f)    { return (f & 0x01) != 0; }
    public static boolean isDown(byte f)  { return (f & 0x02) != 0; }
    public static boolean isLeft(byte f)  { return (f & 0x04) != 0; }
    public static boolean isRight(byte f) { return (f & 0x08) != 0; }
    public static boolean isZ(byte f)     { return (f & 0x10) != 0; }
    public static boolean isX(byte f)     { return (f & 0x20) != 0; }
    public static boolean hasAim(byte f)  { return (f & INPUT_HAS_AIM_MASK) != 0; }

    /**
     * 将规则输入编码为固定长度帧；无有效瞄准时角度字段必须为 0。
     * 发送端只传输入意图，不传本地渲染结果或玩家坐标。
     */
    public static byte[] encodeInputFrame(boolean up, boolean down, boolean left,
                                           boolean right, boolean z, boolean x,
                                           boolean hasAim, float aimAngle) {
        byte flags = encodeInput(up, down, left, right, z, x);
        int quantizedAngle = 0;
        if (hasAim) {
            flags = (byte) ((flags & 0xFF) | INPUT_HAS_AIM_MASK);
            quantizedAngle = quantizeAimAngle(aimAngle);
        }
        return new byte[]{
                TYPE_INPUT,
                flags,
                (byte) (quantizedAngle >>> 8),
                (byte) quantizedAngle
        };
    }

    /** Decodes one fixed-length input frame and rejects unknown flags or malformed headers. */
    public static InputFrame decodeInput(byte[] frame) {
        if (frame == null || frame.length != INPUT_MSG_LEN || frame[0] != TYPE_INPUT) {
            throw new IllegalArgumentException("Invalid input frame.");
        }
        int flags = frame[1] & 0xFF;
        if ((flags & ~INPUT_KNOWN_FLAGS_MASK) != 0) {
            throw new IllegalArgumentException("Input frame contains unknown flags.");
        }
        int quantizedAngle = ((frame[2] & 0xFF) << 8) | (frame[3] & 0xFF);
        boolean hasAim = hasAim(frame[1]);
        if (!hasAim && quantizedAngle != 0) {
            throw new IllegalArgumentException("Input frame has an angle without HAS_AIM.");
        }
        return new InputFrame(frame[1], hasAim, hasAim ? dequantizeAimAngle(quantizedAngle) : 0.0F);
    }

    /** Quantizes a finite angle into an unsigned 16-bit turn fraction. */
    public static int quantizeAimAngle(float angle) {
        if (!Float.isFinite(angle)) {
            throw new IllegalArgumentException("Aim angle must be finite.");
        }
        float normalized = angle % TWO_PI;
        if (normalized < 0.0F) normalized += TWO_PI;
        return Math.round(normalized / TWO_PI * 65536.0F) & 0xFFFF;
    }

    /** Restores an unsigned 16-bit turn fraction to the [0, 2π) range. */
    public static float dequantizeAimAngle(int quantizedAngle) {
        return (quantizedAngle & 0xFFFF) * TWO_PI / 65536.0F;
    }

    public record InputFrame(byte flags, boolean hasAim, float aimAngle) {}

    /** Encodes one completed round so a relay can forward it without understanding game rules. */
    public static byte[] encodeRoundResult(NetworkRoundResult result) {
        return new byte[]{
                TYPE_ROUND_RESULT,
                (byte) result.roundNumber(),
                (byte) result.winnerSide(),
                (byte) result.playerOneWins(),
                (byte) result.playerTwoWins(),
                (byte) (result.matchComplete() ? 1 : 0)
        };
    }

    /** Decodes and validates a fixed-size round-result frame received from the wire. */
    public static NetworkRoundResult decodeRoundResult(byte[] frame) {
        if (frame == null || frame.length != ROUND_RESULT_MSG_LEN || frame[0] != TYPE_ROUND_RESULT) {
            throw new IllegalArgumentException("Invalid round-result frame.");
        }
        return new NetworkRoundResult(
                frame[1] & 0xFF,
                frame[2] & 0xFF,
                frame[3] & 0xFF,
                frame[4] & 0xFF,
                frame[5] != 0);
    }

    /** Encodes a replay or next-round request with the completed round number. */
    public static byte[] encodeRematchRequest(NetworkRematchRequest request) {
        return new byte[]{TYPE_REMATCH_REQUEST, (byte) request.roundNumber(), (byte) (request.matchReset() ? 1 : 0)};
    }

    /** Decodes and validates a fixed-size rematch request frame. */
    public static NetworkRematchRequest decodeRematchRequest(byte[] frame) {
        if (frame == null || frame.length != REMATCH_REQUEST_MSG_LEN || frame[0] != TYPE_REMATCH_REQUEST) {
            throw new IllegalArgumentException("Invalid rematch request frame.");
        }
        return new NetworkRematchRequest(frame[1] & 0xFF, frame[2] != 0);
    }

    private NetworkMessage() {}
}
