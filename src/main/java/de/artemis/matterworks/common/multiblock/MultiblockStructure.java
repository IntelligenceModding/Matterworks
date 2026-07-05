package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.UUID;

public record MultiblockStructure(
        UUID structureId,
        String definitionId,
        BlockPos controllerPos,
        BlockPos originPos,
        Direction front,
        int width,
        int height,
        int depth,
        Map<BlockPos, MultiblockRole> members
) {
    public boolean contains(BlockPos pos) {
        return members.containsKey(pos);
    }
}
