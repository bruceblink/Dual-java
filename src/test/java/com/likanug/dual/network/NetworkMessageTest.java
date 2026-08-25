package com.likanug.dual.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMessageTest {

    @Test
    void encodeInputSetsExpectedBits() {
        byte flags = NetworkMessage.encodeInput(true, false, true, false, true, false);

        assertTrue(NetworkMessage.isUp(flags));
        assertFalse(NetworkMessage.isDown(flags));
        assertTrue(NetworkMessage.isLeft(flags));
        assertFalse(NetworkMessage.isRight(flags));
        assertTrue(NetworkMessage.isZ(flags));
        assertFalse(NetworkMessage.isX(flags));
    }

    @Test
    void encodeInputClearsAllBitsWhenNoInput() {
        byte flags = NetworkMessage.encodeInput(false, false, false, false, false, false);

        assertEquals(0, flags);
        assertFalse(NetworkMessage.isUp(flags));
        assertFalse(NetworkMessage.isDown(flags));
        assertFalse(NetworkMessage.isLeft(flags));
        assertFalse(NetworkMessage.isRight(flags));
        assertFalse(NetworkMessage.isZ(flags));
        assertFalse(NetworkMessage.isX(flags));
    }

    @Test
    void encodeInputSetsAllBitsWhenAllPressed() {
        byte flags = NetworkMessage.encodeInput(true, true, true, true, true, true);

        assertEquals(0x3F, flags & 0xFF);
        assertTrue(NetworkMessage.isUp(flags));
        assertTrue(NetworkMessage.isDown(flags));
        assertTrue(NetworkMessage.isLeft(flags));
        assertTrue(NetworkMessage.isRight(flags));
        assertTrue(NetworkMessage.isZ(flags));
        assertTrue(NetworkMessage.isX(flags));
    }

    @Test
    void inputFrameRoundTripsButtonsAndQuantizedAim() {
        byte[] frame = NetworkMessage.encodeInputFrame(
                true, false, true, false, true, false, true, -0.75F);

        assertEquals(NetworkMessage.INPUT_MSG_LEN, frame.length);
        NetworkMessage.InputFrame decoded = NetworkMessage.decodeInput(frame);
        assertTrue(NetworkMessage.isUp(decoded.flags()));
        assertTrue(NetworkMessage.isLeft(decoded.flags()));
        assertTrue(NetworkMessage.isZ(decoded.flags()));
        assertTrue(decoded.hasAim());
        assertEquals(NetworkMessage.quantizeAimAngle(-0.75F),
                NetworkMessage.quantizeAimAngle(decoded.aimAngle()));
    }

    @Test
    void inputFrameWithoutAimUsesZeroAnglePayload() {
        byte[] frame = NetworkMessage.encodeInputFrame(
                false, false, false, false, false, false, false, 0.0F);

        assertEquals(0, frame[2]);
        assertEquals(0, frame[3]);
        NetworkMessage.InputFrame decoded = NetworkMessage.decodeInput(frame);
        assertFalse(decoded.hasAim());
        assertEquals(0.0F, decoded.aimAngle());
    }

    @Test
    void angleQuantizationWrapsFullTurnsAndRejectsNonFiniteValues() {
        assertEquals(NetworkMessage.quantizeAimAngle(0.0F),
                NetworkMessage.quantizeAimAngle((float) (Math.PI * 2.0)));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.quantizeAimAngle(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.quantizeAimAngle(Float.POSITIVE_INFINITY));
    }

    @Test
    void malformedInputFramesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.decodeInput(new byte[]{NetworkMessage.TYPE_INPUT, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.decodeInput(new byte[]{NetworkMessage.TYPE_START, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.decodeInput(new byte[]{NetworkMessage.TYPE_INPUT, (byte) 0x80, 0, 0}));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.decodeInput(new byte[]{NetworkMessage.TYPE_INPUT, 0, 1, 0}));
    }
}
