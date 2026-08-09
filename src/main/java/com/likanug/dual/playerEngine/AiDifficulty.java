package com.likanug.dual.playerEngine;

/** Defines fair AI timing and decision probabilities without changing shared combat rules. */
public enum AiDifficulty {
    BASIC(18, 0.20F, 0.55F, 0.20F, 0.03F, 0.00F, 0.00F),
    STANDARD(10, 0.30F, 0.70F, 0.20F, 0.05F, 0.00F, 0.00F),
    ADVANCED(5, 0.45F, 0.85F, 0.35F, 0.12F, 0.15F, 0.35F);

    private final int planUpdateFrameCount;
    private final float killAttemptProbability;
    private final float evadeProbability;
    private final float idleMoveProbability;
    private final float longbowReleaseProbability;
    private final float fakeChargeProbability;
    private final float interceptAimProbability;

    AiDifficulty(
            int planUpdateFrameCount,
            float killAttemptProbability,
            float evadeProbability,
            float idleMoveProbability,
            float longbowReleaseProbability,
            float fakeChargeProbability,
            float interceptAimProbability) {
        this.planUpdateFrameCount = planUpdateFrameCount;
        this.killAttemptProbability = killAttemptProbability;
        this.evadeProbability = evadeProbability;
        this.idleMoveProbability = idleMoveProbability;
        this.longbowReleaseProbability = longbowReleaseProbability;
        this.fakeChargeProbability = fakeChargeProbability;
        this.interceptAimProbability = interceptAimProbability;
    }

    public int getPlanUpdateFrameCount() {
        return planUpdateFrameCount;
    }

    public float getKillAttemptProbability() {
        return killAttemptProbability;
    }

    public float getEvadeProbability() {
        return evadeProbability;
    }

    public float getIdleMoveProbability() {
        return idleMoveProbability;
    }

    public float getLongbowReleaseProbability() {
        return longbowReleaseProbability;
    }

    public float getFakeChargeProbability() {
        return fakeChargeProbability;
    }

    public float getInterceptAimProbability() {
        return interceptAimProbability;
    }
}
