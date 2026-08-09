package com.likanug.dual.network;

/** Immutable request to start the next round or replay a completed match. */
public record NetworkRematchRequest(int roundNumber, boolean matchReset) {

    public static final int MSG_LEN = 3;

    public NetworkRematchRequest {
        if (roundNumber <= 0 || roundNumber > 255) {
            throw new IllegalArgumentException("Round number must fit in one positive byte.");
        }
    }
}
