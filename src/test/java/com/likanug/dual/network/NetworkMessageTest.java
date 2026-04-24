package com.likanug.dual.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
