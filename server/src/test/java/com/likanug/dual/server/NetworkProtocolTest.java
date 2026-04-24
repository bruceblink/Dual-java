package com.likanug.dual.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkProtocolTest {

    @Test
    void messageTypeValuesMatchClientProtocol() {
        assertEquals(0x00, NetworkProtocol.TYPE_INPUT);
        assertEquals(0x01, NetworkProtocol.TYPE_START);
        assertEquals(0x02, NetworkProtocol.TYPE_START_ACK);
        assertEquals(0x03, NetworkProtocol.TYPE_DISCONNECT);
    }

    @Test
    void inputMessageLengthIsTypePlusFlags() {
        assertEquals(2, NetworkProtocol.INPUT_MSG_LEN);
    }
}
