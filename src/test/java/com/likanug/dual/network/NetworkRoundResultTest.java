package com.likanug.dual.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkRoundResultTest {

    @Test
    void roundResultRoundTripsThroughFixedFrame() {
        NetworkRoundResult expected = new NetworkRoundResult(2, NetworkRoundResult.SIDE_TWO, 1, 2, true);

        NetworkRoundResult actual = NetworkMessage.decodeRoundResult(NetworkMessage.encodeRoundResult(expected));

        assertEquals(expected, actual);
        assertEquals(new NetworkRoundResult(2, NetworkRoundResult.SIDE_ONE, 2, 1, true), expected.mirrored());
    }

    @Test
    void invalidRoundResultFrameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkMessage.decodeRoundResult(new byte[]{NetworkMessage.TYPE_INPUT}));
    }

    @Test
    void rematchRequestRoundTripsThroughFixedFrame() {
        NetworkRematchRequest expected = new NetworkRematchRequest(3, true);

        assertEquals(expected, NetworkMessage.decodeRematchRequest(NetworkMessage.encodeRematchRequest(expected)));
    }
}
