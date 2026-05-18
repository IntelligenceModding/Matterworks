package de.artemis.matterworks.client.render;

import net.minecraft.core.BlockPos;

public final class MatterNetworkTrackingState {
    private static boolean tracking;
    private static BlockPos targetPos = BlockPos.ZERO;
    private static String label = "";

    private MatterNetworkTrackingState() {
    }

    public static boolean isTracking() {
        return tracking;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static String getLabel() {
        return label;
    }

    public static void setTarget(BlockPos pos, String label) {
        MatterNetworkTrackingState.tracking = true;
        MatterNetworkTrackingState.targetPos = pos.immutable();
        MatterNetworkTrackingState.label = label;
    }

    public static void clear() {
        tracking = false;
        targetPos = BlockPos.ZERO;
        label = "";
    }
}
