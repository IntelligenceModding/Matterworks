package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;

public record MultiblockMatchedPart(
        BlockPos localPos,
        BlockPos worldPos,
        MultiblockRequirement requirement
) {
}
