package com.likanug.dual.network;

/**
 * 网络消息类型常量与 KeyInput 二进制编解码工具。
 * <p>
 * 消息帧格式：
 * <pre>
 *   TYPE_INPUT      [0x00][flags(1byte)]   - 每帧发送本地按键状态
 *   TYPE_START      [0x01][seed(4bytes)]   - 握手：Server→Client 携带共享随机种子
 *   TYPE_START_ACK  [0x02]                 - 握手回应：Client→Server
 *   TYPE_DISCONNECT [0x03]                 - 主动断线通知
 * </pre>
 * flags 字节位定义：bit0=UP, bit1=DOWN, bit2=LEFT, bit3=RIGHT, bit4=Z, bit5=X
 */
public final class NetworkMessage {

    public static final byte TYPE_INPUT      = 0x00;
    public static final byte TYPE_START      = 0x01;
    public static final byte TYPE_START_ACK  = 0x02;
    public static final byte TYPE_DISCONNECT = 0x03;

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

    private NetworkMessage() {}
}
