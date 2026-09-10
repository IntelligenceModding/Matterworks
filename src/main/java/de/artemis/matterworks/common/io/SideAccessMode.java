package de.artemis.matterworks.common.io;

public enum SideAccessMode {
    DISABLED,
    INPUT,
    OUTPUT,
    BOTH,
    OUTPUT_PRIMARY,
    OUTPUT_SECONDARY,
    OUTPUT_TERTIARY;

    public boolean allowsInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean allowsOutput() {
        return this == OUTPUT || this == BOTH || isTargetedOutput();
    }

    public boolean isTargetedOutput() {
        return this == OUTPUT_PRIMARY || this == OUTPUT_SECONDARY || this == OUTPUT_TERTIARY;
    }

    public String getShortLabel() {
        return switch (this) {
            case DISABLED -> "Off";
            case INPUT -> "In";
            case OUTPUT -> "Out";
            case BOTH -> "Both";
            case OUTPUT_PRIMARY -> "Out 1";
            case OUTPUT_SECONDARY -> "Out 2";
            case OUTPUT_TERTIARY -> "Out 3";
        };
    }
}
