package de.artemis.matterworks.common.matter;

record MatterValueError(int value, String reason) {
    static MatterValueError zero(String reason) {
        return new MatterValueError(0, reason);
    }
}
