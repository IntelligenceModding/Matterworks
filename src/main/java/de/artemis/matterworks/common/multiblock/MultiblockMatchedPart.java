package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record MultiblockMatchedPart(
        BlockPos localPos,
        BlockPos worldPos,
        MultiblockRequirement requirement,
        BlockState state,
        @Nullable BlockEntity blockEntity
) {
}
