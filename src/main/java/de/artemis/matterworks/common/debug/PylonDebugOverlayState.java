package de.artemis.matterworks.common.debug;

public final class PylonDebugOverlayState {
    private static boolean enabled;

    private PylonDebugOverlayState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        PylonDebugOverlayState.enabled = enabled;
    }
}
