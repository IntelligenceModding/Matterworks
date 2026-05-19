package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class MultiblockAssembler {
    private MultiblockAssembler() {
    }

    public static MultiblockValidationResult validate(ServerLevel level, BlockPos controllerPos, Direction front, MultiblockDefinition definition) {
        if (!definition.getSupportedFronts().contains(front)) {
            return MultiblockValidationResult.failure("unsupported multiblock front: " + front.getName());
        }

        MultiblockPattern pattern = definition.getPattern();
        BlockPos originPos = MultiblockTransforms.controllerToOrigin(controllerPos, front, pattern.getControllerOffset());
        List<MultiblockMatchedPart> matchedParts = new ArrayList<>(pattern.getRequirements().size());

        for (var entry : pattern.getRequirements().entrySet()) {
            BlockPos localPos = entry.getKey();
            MultiblockRequirement requirement = entry.getValue();
            BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, localPos);
            BlockState state = level.getBlockState(worldPos);
            BlockEntity blockEntity = level.getBlockEntity(worldPos);
            MultiblockMatchContext context = new MultiblockMatchContext(level, controllerPos, originPos, localPos, worldPos, state, blockEntity);
            if (!requirement.predicate().matches(context) && !requirement.optional()) {
                return MultiblockValidationResult.failure(worldPos, localPos, requirement.description());
            }
            matchedParts.add(new MultiblockMatchedPart(localPos, worldPos, requirement, state, blockEntity));
        }

        MultiblockMatch match = new MultiblockMatch(definition, controllerPos, originPos, front, matchedParts);
        if (!definition.canAssemble(match)) {
            return MultiblockValidationResult.failure("definition rejected structure assembly");
        }
        return MultiblockValidationResult.success(match);
    }
}
