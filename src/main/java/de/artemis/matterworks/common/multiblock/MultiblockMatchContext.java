package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record MultiblockMatchContext(
        ServerLevel level,
        BlockPos controllerPos,
        BlockPos originPos,
        BlockPos localPos,
        BlockPos worldPos,
        BlockState state,
        @Nullable BlockEntity blockEntity
) {
}
