package de.artemis.matterworks.common.menu;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SideConfigOrientation {
    private SideConfigOrientation() {
    }

    public static Direction resolveFrontFacing(BlockState state) {
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction direction = state.getValue(HorizontalDirectionalBlock.FACING);
            if (direction.getAxis().isHorizontal()) {
                return direction;
            }
        }
        return Direction.NORTH;
    }
}
