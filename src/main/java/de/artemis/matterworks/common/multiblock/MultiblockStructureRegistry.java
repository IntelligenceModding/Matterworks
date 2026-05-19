package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public final class MultiblockStructureRegistry {
    private static final Map<ServerLevel, DimensionState> STATES = new WeakHashMap<>();

    private MultiblockStructureRegistry() {
    }

    public static Optional<MultiblockStructure> getByMember(ServerLevel level, BlockPos pos) {
        DimensionState state = STATES.get(level);
        if (state == null) {
            return Optional.empty();
        }
        UUID structureId = state.memberToStructure.get(pos);
        return structureId == null ? Optional.empty() : Optional.ofNullable(state.structuresById.get(structureId));
    }

    public static Optional<MultiblockStructure> getById(ServerLevel level, UUID structureId) {
        DimensionState state = STATES.get(level);
        return state == null ? Optional.empty() : Optional.ofNullable(state.structuresById.get(structureId));
    }

    public static MultiblockValidationResult validate(ServerLevel level, BlockPos controllerPos, net.minecraft.core.Direction front, MultiblockDefinition definition) {
        MultiblockValidationResult result = MultiblockAssembler.validate(level, controllerPos, front, definition);
        if (!result.success()) {
            return result;
        }
        for (MultiblockMatchedPart part : result.match().getParts()) {
            Optional<MultiblockStructure> existing = getByMember(level, part.worldPos());
            if (existing.isPresent()) {
                return MultiblockValidationResult.failure(part.worldPos(), part.localPos(), "occupied by multiblock " + existing.get().definitionId());
            }
        }
        return result;
    }

    public static Optional<MultiblockStructure> assemble(ServerLevel level, MultiblockMatch match) {
        DimensionState state = STATES.computeIfAbsent(level, ignored -> new DimensionState());
        for (MultiblockMatchedPart part : match.getParts()) {
            if (state.memberToStructure.containsKey(part.worldPos())) {
                return Optional.empty();
            }
        }

        Map<BlockPos, MultiblockRole> members = new LinkedHashMap<>();
        for (MultiblockMatchedPart part : match.getParts()) {
            members.put(part.worldPos().immutable(), part.requirement().role());
        }

        MultiblockStructure structure = new MultiblockStructure(
                UUID.randomUUID(),
                match.getDefinition().getId(),
                match.getControllerPos(),
                match.getOriginPos(),
                match.getFront(),
                Map.copyOf(members)
        );

            state.structuresById.put(structure.structureId(), structure);
        for (var entry : structure.members().entrySet()) {
            state.memberToStructure.put(entry.getKey(), structure.structureId());
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (blockEntity instanceof MultiblockPartEntity multiblockPartEntity) {
                BlockPos localPos = findLocalPos(match, entry.getKey());
                multiblockPartEntity.getMultiblockPartState().applyAssembly(structure, entry.getValue(), localPos);
                multiblockPartEntity.getMultiblockPartState().sync(blockEntity);
                multiblockPartEntity.onMultiblockAssembled(structure, entry.getValue());
            }
        }
        return Optional.of(structure);
    }

    public static boolean disassembleByMember(ServerLevel level, BlockPos pos) {
        Optional<MultiblockStructure> structure = getByMember(level, pos);
        if (structure.isEmpty()) {
            return false;
        }
        return disassemble(level, structure.get().structureId());
    }

    public static boolean disassemble(ServerLevel level, UUID structureId) {
        DimensionState state = STATES.get(level);
        if (state == null) {
            return false;
        }
        MultiblockStructure structure = state.structuresById.remove(structureId);
        if (structure == null) {
            return false;
        }
        for (BlockPos memberPos : structure.members().keySet()) {
            state.memberToStructure.remove(memberPos);
            BlockEntity blockEntity = level.getBlockEntity(memberPos);
            if (blockEntity instanceof MultiblockPartEntity multiblockPartEntity) {
                multiblockPartEntity.getMultiblockPartState().clear();
                multiblockPartEntity.getMultiblockPartState().sync(blockEntity);
                multiblockPartEntity.onMultiblockDisassembled(structure);
            }
        }
        return true;
    }

    public static void clearServerLevelState(ServerLevel level) {
        STATES.remove(level);
    }

    private static BlockPos findLocalPos(MultiblockMatch match, BlockPos worldPos) {
        for (MultiblockMatchedPart part : match.getParts()) {
            if (part.worldPos().equals(worldPos)) {
                return part.localPos();
            }
        }
        return BlockPos.ZERO;
    }

    private static final class DimensionState {
        private final Map<UUID, MultiblockStructure> structuresById = new HashMap<>();
        private final Map<BlockPos, UUID> memberToStructure = new HashMap<>();
    }
}
