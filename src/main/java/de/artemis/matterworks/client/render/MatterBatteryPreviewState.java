package de.artemis.matterworks.client.render;

import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class MatterBatteryPreviewState {
    private static boolean active;
    private static boolean toolDriven;
    private static boolean valid;
    private static boolean locked;
    private static BlockPos controllerPos = BlockPos.ZERO;
    private static BlockPos originPos = BlockPos.ZERO;
    private static BlockPos minPos = BlockPos.ZERO;
    private static BlockPos maxPos = BlockPos.ZERO;
    private static Direction front = Direction.NORTH;
    private static int selectedLayer;
    private static int width;
    private static int height;
    private static int depth;

    private MatterBatteryPreviewState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isToolDriven() {
        return toolDriven;
    }

    public static boolean isValid() {
        return valid;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static BlockPos getControllerPos() {
        return controllerPos;
    }

    public static BlockPos getOriginPos() {
        return originPos;
    }

    public static BlockPos getMinPos() {
        return minPos;
    }

    public static BlockPos getMaxPos() {
        return maxPos;
    }

    public static Direction getFront() {
        return front;
    }

    public static int getSelectedLayer() {
        return selectedLayer;
    }

    public static int getLayerCount() {
        return Math.max(1, height);
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getDepth() {
        return depth;
    }

    public static void enable(BlockPos minPos, BlockPos maxPos, Direction front, boolean locked, int selectedLayer, boolean toolDriven) {
        MatterBatteryPreviewState.minPos = minPos.immutable();
        MatterBatteryPreviewState.maxPos = maxPos.immutable();
        MatterBatteryPreviewState.front = front;
        width = MatterBatteryMultiblockLayout.getWidth(minPos, maxPos, front);
        height = MatterBatteryMultiblockLayout.getHeight(minPos, maxPos);
        depth = MatterBatteryMultiblockLayout.getDepth(minPos, maxPos, front);
        originPos = MatterBatteryMultiblockLayout.getOrigin(minPos, maxPos, front);
        controllerPos = MatterBatteryMultiblockLayout.getControllerPos(minPos, maxPos, front);
        valid = MatterBatteryMultiblockLayout.isValidSize(width, height, depth);
        active = true;
        MatterBatteryPreviewState.locked = locked;
        MatterBatteryPreviewState.toolDriven = toolDriven;
        MatterBatteryPreviewState.selectedLayer = clampLayer(selectedLayer);
    }

    public static void shift(int dx, int dy, int dz) {
        if (!active) {
            return;
        }
        enable(minPos.offset(dx, dy, dz), maxPos.offset(dx, dy, dz), front, locked, selectedLayer, toolDriven);
    }

    public static void setSelectedLayer(int layer) {
        selectedLayer = clampLayer(layer);
    }

    public static void cycleLayer(int delta) {
        selectedLayer = clampLayer(selectedLayer + delta);
    }

    public static void clear() {
        active = false;
        toolDriven = false;
        valid = false;
        locked = false;
        controllerPos = BlockPos.ZERO;
        originPos = BlockPos.ZERO;
        minPos = BlockPos.ZERO;
        maxPos = BlockPos.ZERO;
        front = Direction.NORTH;
        selectedLayer = 0;
        width = 0;
        height = 0;
        depth = 0;
    }

    private static int clampLayer(int layer) {
        return Math.max(0, Math.min(getLayerCount() - 1, layer));
    }
}
