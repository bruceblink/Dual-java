package com.likanug.dual.playerEngine;

import com.likanug.dual.inputDevice.AbstractInputDevice;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.network.GameNetwork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkPlayerEngineTest {

    @Test
    void mirrorsRemoteDirectionForLocalPerspective() {
        KeyInput remoteInput = new KeyInput();
        remoteInput.isUpPressed = true;
        remoteInput.isLeftPressed = true;
        remoteInput.isZPressed = true;
        remoteInput.isXPressed = true;

        NetworkPlayerEngine engine = new NetworkPlayerEngine(new StubNetwork(remoteInput));
        engine.run(null);

        AbstractInputDevice input = engine.getControllingInputDevice();
        assertEquals(1, input.getHorizontalMoveButton());
        assertEquals(1, input.getVerticalMoveButton());
        assertTrue(input.isShotButtonPressed());
        assertTrue(input.isLongShotButtonPressed());
    }

    @Test
    void opposingRemoteDirectionsCancelBeforeMirroring() {
        KeyInput remoteInput = new KeyInput();
        remoteInput.isUpPressed = true;
        remoteInput.isDownPressed = true;
        remoteInput.isLeftPressed = true;
        remoteInput.isRightPressed = true;

        NetworkPlayerEngine engine = new NetworkPlayerEngine(new StubNetwork(remoteInput));
        engine.run(null);

        AbstractInputDevice input = engine.getControllingInputDevice();
        assertEquals(0, input.getHorizontalMoveButton());
        assertEquals(0, input.getVerticalMoveButton());
    }

    private static class StubNetwork extends GameNetwork {
        private final KeyInput remoteInput;

        private StubNetwork(KeyInput remoteInput) {
            this.remoteInput = remoteInput;
        }

        @Override
        public KeyInput getRemoteInput() {
            return remoteInput;
        }
    }
}
