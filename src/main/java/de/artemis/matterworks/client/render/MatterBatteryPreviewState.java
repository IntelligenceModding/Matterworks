package de.artemis.matterworks.client.render;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockDefinition;
import de.artemis.matterworks.common.multiblock.MultiblockPattern;
import de.artemis.matterworks.common.multiblock.MultiblockTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public final class MatterBatteryPreviewState {
    private static boolean active;
    private static BlockPos controllerPos = BlockPos.ZERO;
    private static BlockPos originPos = BlockPos.ZERO;
    private static Direction front = Direction.NORTH;
    private static int selectedLayer;

    private MatterBatteryPreviewState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static BlockPos getControllerPos() {
        return controllerPos;
    }

    public static BlockPos getOriginPos() {
        return originPos;
    }

    public static Direction getFront() {
        return front;
    }

    public static int getSelectedLayer() {
        return selectedLayer;
    }

    public static int getLayerCount() {
        return MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getHeight();
    }

    public static void toggle(MatterBatteryCoreBlockEntity blockEntity) {
        if (active && controllerPos.equals(blockEntity.getBlockPos())) {
            clear();
        } else {
            enable(blockEntity);
        }
    }

    public static void enable(MatterBatteryCoreBlockEntity blockEntity) {
        MultiblockPattern pattern = MatterBatteryMultiblockDefinition.INSTANCE.getPattern();
        Direction resolvedFront = blockEntity.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        BlockPos resolvedOrigin = blockEntity.getMultiblockPartState().isFormed()
                ? blockEntity.getMultiblockPartState().getOriginPos()
                : MultiblockTransforms.controllerToOrigin(blockEntity.getBlockPos(), resolvedFront, pattern.getControllerOffset());

        active = true;
        controllerPos = blockEntity.getBlockPos().immutable();
        originPos = resolvedOrigin.immutable();
        front = resolvedFront;
        selectedLayer = clampLayer(selectedLayer);
    }

    public static void setSelectedLayer(int layer) {
        selectedLayer = clampLayer(layer);
    }

    public static void cycleLayer(int delta) {
        selectedLayer = clampLayer(selectedLayer + delta);
    }

    public static void clear() {
        active = false;
        controllerPos = BlockPos.ZERO;
        originPos = BlockPos.ZERO;
        front = Direction.NORTH;
        selectedLayer = 0;
    }

    private static int clampLayer(int layer) {
        return Math.max(0, Math.min(getLayerCount() - 1, layer));
    }
}
