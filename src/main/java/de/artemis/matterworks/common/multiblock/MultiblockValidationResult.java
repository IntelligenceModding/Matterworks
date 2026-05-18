package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public record MultiblockValidationResult(
        boolean success,
        @Nullable MultiblockMatch match,
        @Nullable BlockPos failedWorldPos,
        @Nullable BlockPos failedLocalPos,
        String message
) {
    public static MultiblockValidationResult success(MultiblockMatch match) {
        return new MultiblockValidationResult(true, match, null, null, "");
    }

    public static MultiblockValidationResult failure(BlockPos failedWorldPos, BlockPos failedLocalPos, String message) {
        return new MultiblockValidationResult(false, null, failedWorldPos, failedLocalPos, message);
    }

    public static MultiblockValidationResult failure(String message) {
        return new MultiblockValidationResult(false, null, null, null, message);
    }
}
