package de.artemis.matterworks.common.io;

public enum SideAccessMode {
    DISABLED,
    INPUT,
    OUTPUT,
    BOTH;

    public boolean allowsInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean allowsOutput() {
        return this == OUTPUT || this == BOTH;
    }

    public String getShortLabel() {
        return switch (this) {
            case DISABLED -> "Off";
            case INPUT -> "In";
            case OUTPUT -> "Out";
            case BOTH -> "Both";
        };
    }
}
