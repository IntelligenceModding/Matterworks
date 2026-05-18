package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.Direction;

import java.util.EnumSet;
import java.util.Set;

public interface MultiblockDefinition {
    String getId();

    MultiblockPattern getPattern();

    default Set<Direction> getSupportedFronts() {
        return EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    }

    default boolean canAssemble(MultiblockMatch match) {
        return true;
    }
}
