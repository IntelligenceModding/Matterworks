package de.artemis.matterworks.common.debug;

public final class SideConfigDebugOverlayState {
    private static boolean enabled;

    private SideConfigDebugOverlayState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        SideConfigDebugOverlayState.enabled = enabled;
    }
}
