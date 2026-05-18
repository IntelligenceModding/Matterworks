package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class MultiblockTransforms {
    private MultiblockTransforms() {
    }

    public static BlockPos localToWorld(BlockPos originPos, Direction front, BlockPos localPos) {
        Direction right = front.getClockWise();
        return originPos.offset(
                right.getStepX() * localPos.getX() + front.getStepX() * localPos.getZ(),
                localPos.getY(),
                right.getStepZ() * localPos.getX() + front.getStepZ() * localPos.getZ()
        );
    }

    public static BlockPos controllerToOrigin(BlockPos controllerPos, Direction front, BlockPos controllerOffset) {
        BlockPos transformedOffset = localToWorld(BlockPos.ZERO, front, controllerOffset);
        return controllerPos.offset(-transformedOffset.getX(), -transformedOffset.getY(), -transformedOffset.getZ());
    }
}
