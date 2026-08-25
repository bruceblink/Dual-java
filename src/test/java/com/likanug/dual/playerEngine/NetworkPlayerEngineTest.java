package com.likanug.dual.playerEngine;

import com.likanug.dual.inputDevice.AbstractInputDevice;
import com.likanug.dual.inputDevice.KeyInput;
import com.likanug.dual.network.GameNetwork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void mirrorsRemoteAimAngleForTopPlayerView() {
        StubNetwork network = new StubNetwork(new KeyInput(), true, 0.25F);
        NetworkPlayerEngine engine = new NetworkPlayerEngine(network);

        engine.run(null);

        AbstractInputDevice input = engine.getControllingInputDevice();
        assertTrue(input.hasAimAngle());
        assertEquals(NetworkPlayerEngine.mirrorArenaAngle(0.25F), input.getAimAngle(), 0.000001F);
    }

    @Test
    void noRemoteAimDoesNotInventAnAngle() {
        NetworkPlayerEngine engine = new NetworkPlayerEngine(new StubNetwork(new KeyInput()));

        engine.run(null);

        assertFalse(engine.getControllingInputDevice().hasAimAngle());
    }

    private static class StubNetwork extends GameNetwork {
        private final KeyInput remoteInput;
        private final boolean hasAim;
        private final float aimAngle;

        private StubNetwork(KeyInput remoteInput) {
            this(remoteInput, false, 0.0F);
        }

        private StubNetwork(KeyInput remoteInput, boolean hasAim, float aimAngle) {
            this.remoteInput = remoteInput;
            this.hasAim = hasAim;
            this.aimAngle = aimAngle;
        }

        @Override
        public KeyInput getRemoteInput() {
            return remoteInput;
        }

        @Override
        public boolean hasRemoteAim() {
            return hasAim;
        }

        @Override
        public float getRemoteAimAngle() {
            return aimAngle;
        }
    }
}
