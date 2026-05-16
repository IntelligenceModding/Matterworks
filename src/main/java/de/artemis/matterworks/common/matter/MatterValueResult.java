package de.artemis.matterworks.common.matter;

public record MatterValueResult(boolean allowed, int matterMillibuckets, String reason) {
    public static MatterValueResult allowed(int matterMillibuckets) {
        return new MatterValueResult(true, Math.max(0, matterMillibuckets), "allowed");
    }

    public static MatterValueResult rejected(String reason) {
        return new MatterValueResult(false, 0, reason);
    }
}
