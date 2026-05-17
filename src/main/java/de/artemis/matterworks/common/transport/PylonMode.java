package de.artemis.matterworks.common.transport;

public enum PylonMode {
    DISABLED,
    EXPORT,
    IMPORT,
    IMPORT_EXPORT;

    public PylonMode next() {
        return switch (this) {
            case DISABLED -> EXPORT;
            case EXPORT -> IMPORT;
            case IMPORT -> IMPORT_EXPORT;
            case IMPORT_EXPORT -> DISABLED;
        };
    }

    public boolean canExport() {
        return this == EXPORT || this == IMPORT_EXPORT;
    }

    public boolean canImport() {
        return this == IMPORT || this == IMPORT_EXPORT;
    }

    public String translationKey() {
        return "message.matterworks.pylon.mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
