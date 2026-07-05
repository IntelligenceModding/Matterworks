package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class MatterBatteryMultiblockLayout {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 16;

    private MatterBatteryMultiblockLayout() {
    }

    public static boolean isValidSize(int width, int height, int depth) {
        return isValidAxis(width) && isValidAxis(height) && isValidAxis(depth);
    }

    public static boolean isValidAxis(int axis) {
        return axis >= MIN_SIZE && axis <= MAX_SIZE;
    }

    public static BlockPos getControllerOffset(int width, int height, int depth) {
        return new BlockPos(1, Math.min(1, height - 2), 1);
    }

    public static BlockPos getOrigin(BlockPos minPos, BlockPos maxPos, Direction front) {
        return switch (front) {
            case NORTH -> new BlockPos(minPos.getX(), minPos.getY(), maxPos.getZ());
            case SOUTH -> new BlockPos(maxPos.getX(), minPos.getY(), minPos.getZ());
            case WEST -> new BlockPos(maxPos.getX(), minPos.getY(), maxPos.getZ());
            default -> new BlockPos(minPos.getX(), minPos.getY(), minPos.getZ());
        };
    }

    public static BlockPos getControllerPos(BlockPos minPos, BlockPos maxPos, Direction front) {
        int width = getWidth(minPos, maxPos, front);
        int height = getHeight(minPos, maxPos);
        int depth = getDepth(minPos, maxPos, front);
        return MultiblockTransforms.localToWorld(getOrigin(minPos, maxPos, front), front, getControllerOffset(width, height, depth));
    }

    public static WorldBounds getWorldBounds(BlockPos originPos, Direction front, int width, int height, int depth) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int localX : new int[]{0, width - 1}) {
            for (int localY : new int[]{0, height - 1}) {
                for (int localZ : new int[]{0, depth - 1}) {
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, new BlockPos(localX, localY, localZ));
                    minX = Math.min(minX, worldPos.getX());
                    minY = Math.min(minY, worldPos.getY());
                    minZ = Math.min(minZ, worldPos.getZ());
                    maxX = Math.max(maxX, worldPos.getX());
                    maxY = Math.max(maxY, worldPos.getY());
                    maxZ = Math.max(maxZ, worldPos.getZ());
                }
            }
        }

        return new WorldBounds(
                new BlockPos(minX, minY, minZ),
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1
        );
    }

    public static int getWidth(BlockPos minPos, BlockPos maxPos, Direction front) {
        return front.getAxis() == Direction.Axis.Z
                ? maxPos.getX() - minPos.getX() + 1
                : maxPos.getZ() - minPos.getZ() + 1;
    }

    public static int getHeight(BlockPos minPos, BlockPos maxPos) {
        return maxPos.getY() - minPos.getY() + 1;
    }

    public static int getDepth(BlockPos minPos, BlockPos maxPos, Direction front) {
        return front.getAxis() == Direction.Axis.Z
                ? maxPos.getZ() - minPos.getZ() + 1
                : maxPos.getX() - minPos.getX() + 1;
    }

    public static MultiblockRole getRole(BlockPos localPos, int width, int height, int depth) {
        if (localPos.equals(getControllerOffset(width, height, depth))) {
            return MultiblockRole.CONTROLLER;
        }
        if (isEdge(localPos, width, height, depth)) {
            return MultiblockRole.FRAME;
        }
        if (isSurface(localPos, width, height, depth)) {
            return MultiblockRole.CASING;
        }
        return MultiblockRole.INTERNAL;
    }

    public static String getRequirementDescription(BlockPos localPos, int width, int height, int depth) {
        return switch (getRole(localPos, width, height, depth)) {
            case CONTROLLER -> MatterBatteryMultiblockDefinition.DESC_CONTROLLER;
            case FRAME -> MatterBatteryMultiblockDefinition.DESC_FRAME;
            case CASING, PORT -> MatterBatteryMultiblockDefinition.DESC_CASING;
            case INTERNAL -> MatterBatteryMultiblockDefinition.DESC_CELL;
        };
    }

    public static boolean isEdge(BlockPos localPos, int width, int height, int depth) {
        int boundaries = 0;
        if (localPos.getX() == 0 || localPos.getX() == width - 1) {
            boundaries++;
        }
        if (localPos.getY() == 0 || localPos.getY() == height - 1) {
            boundaries++;
        }
        if (localPos.getZ() == 0 || localPos.getZ() == depth - 1) {
            boundaries++;
        }
        return boundaries >= 2;
    }

    public static boolean isSurface(BlockPos localPos, int width, int height, int depth) {
        return localPos.getX() == 0 || localPos.getX() == width - 1
                || localPos.getY() == 0 || localPos.getY() == height - 1
                || localPos.getZ() == 0 || localPos.getZ() == depth - 1;
    }

    public record WorldBounds(BlockPos minPos, int sizeX, int sizeY, int sizeZ) {
    }
}
