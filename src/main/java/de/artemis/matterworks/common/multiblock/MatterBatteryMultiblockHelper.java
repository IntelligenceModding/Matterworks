package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.block.MatterBatteryCoreBlock;
import de.artemis.matterworks.common.block.MatterCapacitorCellBlock;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MatterBatteryMultiblockHelper {
    private MatterBatteryMultiblockHelper() {
    }

    public static void onBlockPlaced(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            if (refreshExistingStructure(serverLevel, pos)) {
                return;
            }
            recoverNearbyStoredBattery(serverLevel, pos);
        }
    }

    public static void onBlockRemoved(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            MultiblockStructure storedStructure = getStoredStructureFromPart(level, pos);
            if (refreshExistingStructure(serverLevel, pos)) {
                return;
            }
            if (MultiblockStructureRegistry.disassembleByMember(serverLevel, pos)) {
                return;
            }
            if (storedStructure != null) {
                clearStoredStructureState(serverLevel, storedStructure);
                return;
            }
            clearNearbyStoredStructureState(serverLevel, pos);
        }
    }

    public static boolean tryOpenBatteryMenu(ServerLevel level, BlockPos memberPos, Player player) {
        var structureOptional = getOrRecoverBatteryStructure(level, memberPos);
        if (structureOptional.isEmpty()) {
            return false;
        }

        MultiblockStructure structure = structureOptional.get();
        if (!MatterBatteryMultiblockDefinition.ID.equals(structure.definitionId())) {
            return false;
        }

        BlockEntity controllerBlockEntity = level.getBlockEntity(structure.controllerPos());
        if (!(controllerBlockEntity instanceof MatterBatteryCoreBlockEntity controller) || !controller.isFormed()) {
            return false;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new MatterBatteryCoreMenu(containerId, playerInventory, controller, controller.getData()),
                net.minecraft.network.chat.Component.literal("Matter Battery")
        );
        player.openMenu(provider, controller.getBlockPos());
        return true;
    }

    public static java.util.Optional<MultiblockStructure> getOrRecoverBatteryStructure(ServerLevel level, BlockPos memberPos) {
        var structureOptional = MultiblockStructureRegistry.getByMember(level, memberPos);
        if (structureOptional.isPresent()) {
            return structureOptional;
        }

        BlockEntity memberBlockEntity = level.getBlockEntity(memberPos);
        if (!(memberBlockEntity instanceof MultiblockPartEntity multiblockPartEntity)) {
            return java.util.Optional.empty();
        }

        MultiblockPartState state = multiblockPartEntity.getMultiblockPartState();
        if (!state.isFormed() || !MatterBatteryMultiblockDefinition.ID.equals(state.getDefinitionId())) {
            return java.util.Optional.empty();
        }

        if (!recoverStructure(level, state.getOriginPos(), state.getFront(), state.getWidth(), state.getHeight(), state.getDepth())) {
            return java.util.Optional.empty();
        }

        return MultiblockStructureRegistry.getByMember(level, memberPos);
    }

    public static boolean recoverStructure(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth) {
        MultiblockMatch match = validateBattery(level, originPos, front, width, height, depth, null);
        if (match == null) {
            return false;
        }
        return MultiblockStructureRegistry.assemble(level, match).isPresent();
    }

    public static boolean tryAssembleAtOrigin(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth, @Nullable Player player, boolean notifyFailure) {
        MultiblockMatch match = validateBattery(level, originPos, front, width, height, depth, null);
        if (match == null) {
            if (notifyFailure && player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Battery blueprint is incomplete"), true);
            }
            return false;
        }
        boolean assembled = MultiblockStructureRegistry.assemble(level, match).isPresent();
        if (assembled && player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.matterworks.matter_battery.assembled"), true);
        }
        return assembled;
    }

    private static boolean refreshExistingStructure(ServerLevel level, BlockPos changedPos) {
        var structureOptional = MultiblockStructureRegistry.getByMember(level, changedPos);
        if (structureOptional.isEmpty()) {
            return false;
        }

        MultiblockStructure structure = structureOptional.get();
        if (!MatterBatteryMultiblockDefinition.ID.equals(structure.definitionId())) {
            return false;
        }

        BlockEntity controllerBlockEntity = level.getBlockEntity(structure.controllerPos());
        if (!(controllerBlockEntity instanceof MatterBatteryCoreBlockEntity controller)) {
            MultiblockStructureRegistry.disassemble(level, structure.structureId());
            return true;
        }

        MultiblockMatch validation = validateBattery(level, structure.originPos(), structure.front(), structure.width(), structure.height(), structure.depth(), structure.structureId());
        if (validation == null) {
            MultiblockStructureRegistry.disassemble(level, structure.structureId());
            return true;
        }

        MultiblockStructureRegistry.refreshMembers(level, structure, validation);
        controller.refreshStructureStats(level, structure);
        return true;
    }

    private static boolean recoverNearbyStoredBattery(ServerLevel level, BlockPos changedPos) {
        int radius = MatterBatteryMultiblockLayout.MAX_SIZE;
        BlockPos minSearch = changedPos.offset(-radius, -radius, -radius);
        BlockPos maxSearch = changedPos.offset(radius, radius, radius);
        for (BlockPos scanPos : BlockPos.betweenClosed(minSearch, maxSearch)) {
            BlockEntity blockEntity = level.getBlockEntity(scanPos);
            if (!(blockEntity instanceof MatterBatteryCoreBlockEntity controller)) {
                continue;
            }
            if (!controller.hasStoredBlueprint() || !controller.usesStoredBlueprintPosition(changedPos)) {
                continue;
            }
            if (controller.tryRecoverStoredStructure(level)) {
                return true;
            }
        }
        return false;
    }

    public static void clearStoredStates(ServerLevel level, BlockPos originPos, BlockPos controllerPos, Direction front, int width, int height, int depth) {
        clearStoredStructureState(level, createStructure(UUID.randomUUID(), controllerPos, originPos, front, width, height, depth));
    }

    public static MultiblockStructure createStructure(UUID structureId, BlockPos controllerPos, BlockPos originPos, Direction front, int width, int height, int depth) {
        Map<BlockPos, MultiblockRole> members = new LinkedHashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, localPos);
                    members.put(worldPos.immutable(), MatterBatteryMultiblockLayout.getRole(localPos, width, height, depth));
                }
            }
        }
        return new MultiblockStructure(structureId, MatterBatteryMultiblockDefinition.ID, controllerPos, originPos, front, width, height, depth, Map.copyOf(members));
    }

    public static void clearStoredStructureState(ServerLevel level, MultiblockStructure structure) {
        for (BlockPos memberPos : structure.members().keySet()) {
            BlockState currentState = level.getBlockState(memberPos);
            BlockState updatedState = currentState;
            if (updatedState.hasProperty(MatterBatteryCoreBlock.FORMED)) {
                updatedState = updatedState.setValue(MatterBatteryCoreBlock.FORMED, false);
            }
            if (updatedState.hasProperty(MatterCapacitorCellBlock.FORMED)) {
                updatedState = updatedState.setValue(MatterCapacitorCellBlock.FORMED, false);
            }
            if (updatedState != currentState) {
                level.setBlock(memberPos, updatedState, 3);
            }

            BlockEntity blockEntity = level.getBlockEntity(memberPos);
            if (blockEntity instanceof MultiblockPartEntity multiblockPartEntity
                    && (memberPos.equals(structure.controllerPos()) || belongsToStructure(multiblockPartEntity.getMultiblockPartState(), structure))) {
                multiblockPartEntity.getMultiblockPartState().clear();
                multiblockPartEntity.getMultiblockPartState().sync(blockEntity);
                multiblockPartEntity.onMultiblockDisassembled(structure);
            }
        }
    }

    public static Direction getOutwardSide(BlockPos localPos, Direction front, int width, int height, int depth) {
        Direction right = front.getClockWise();
        if (localPos.getY() == 0) {
            return Direction.DOWN;
        }
        if (localPos.getY() == height - 1) {
            return Direction.UP;
        }
        if (localPos.getZ() == 0) {
            return front.getOpposite();
        }
        if (localPos.getZ() == depth - 1) {
            return front;
        }
        if (localPos.getX() == 0) {
            return right.getOpposite();
        }
        if (localPos.getX() == width - 1) {
            return right;
        }
        return front;
    }

    private static @Nullable MultiblockMatch validateBattery(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth, @Nullable UUID allowedStructureId) {
        if (!front.getAxis().isHorizontal() || !MatterBatteryMultiblockLayout.isValidSize(width, height, depth)) {
            return null;
        }

        List<MultiblockMatchedPart> matchedParts = new ArrayList<>(width * height * depth);
        BlockPos controllerLocalPos = MatterBatteryMultiblockLayout.getControllerOffset(width, height, depth);
        BlockPos controllerPos = MultiblockTransforms.localToWorld(originPos, front, controllerLocalPos);

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, localPos);
                    BlockState state = level.getBlockState(worldPos);
                    BlockEntity blockEntity = level.getBlockEntity(worldPos);
                    MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, width, height, depth);
                    String description = MatterBatteryMultiblockLayout.getRequirementDescription(localPos, width, height, depth);
                    if (!matchesRequirement(role, state)) {
                        return null;
                    }
                    var existing = MultiblockStructureRegistry.getByMember(level, worldPos);
                    if (existing.isPresent()
                            && (allowedStructureId == null || !allowedStructureId.equals(existing.get().structureId()))) {
                        return null;
                    }
                    matchedParts.add(new MultiblockMatchedPart(
                            localPos,
                            worldPos,
                            new MultiblockRequirement(role, MultiblockPredicate.any(), description, false),
                            state,
                            blockEntity
                    ));
                }
            }
        }

        return new MultiblockMatch(MatterBatteryMultiblockDefinition.INSTANCE, controllerPos, originPos, front, width, height, depth, matchedParts);
    }

    private static boolean matchesRequirement(MultiblockRole role, net.minecraft.world.level.block.state.BlockState state) {
        return switch (role) {
            case CONTROLLER -> state.is(de.artemis.matterworks.common.registry.ModBlocks.MATTER_BATTERY_CORE.get());
            case FRAME -> MatterBatteryMultiblockDefinition.matchesFrameState(state);
            case CASING, PORT -> MatterBatteryMultiblockDefinition.matchesShellFaceState(state);
            case INTERNAL -> MatterBatteryMultiblockDefinition.matchesInternalState(state);
        };
    }

    private static @Nullable MultiblockStructure getStoredStructureFromPart(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MultiblockPartEntity multiblockPartEntity)) {
            return null;
        }
        MultiblockPartState state = multiblockPartEntity.getMultiblockPartState();
        if (!state.isFormed()
                || !MatterBatteryMultiblockDefinition.ID.equals(state.getDefinitionId())
                || !MatterBatteryMultiblockLayout.isValidSize(state.getWidth(), state.getHeight(), state.getDepth())) {
            return null;
        }
        UUID structureId = state.getStructureId() == null ? UUID.randomUUID() : state.getStructureId();
        return createStructure(structureId, state.getControllerPos(), state.getOriginPos(), state.getFront(), state.getWidth(), state.getHeight(), state.getDepth());
    }

    private static boolean clearNearbyStoredStructureState(ServerLevel level, BlockPos changedPos) {
        int radius = MatterBatteryMultiblockLayout.MAX_SIZE;
        BlockPos minSearch = changedPos.offset(-radius, -radius, -radius);
        BlockPos maxSearch = changedPos.offset(radius, radius, radius);
        for (BlockPos scanPos : BlockPos.betweenClosed(minSearch, maxSearch)) {
            BlockEntity blockEntity = level.getBlockEntity(scanPos);
            if (!(blockEntity instanceof MatterBatteryCoreBlockEntity controller)
                    || !controller.hasStoredBlueprint()
                    || !controller.usesStoredBlueprintPosition(changedPos)) {
                continue;
            }
            MultiblockStructure storedStructure = controller.createStoredBlueprintStructure();
            if (storedStructure != null) {
                clearStoredStructureState(level, storedStructure);
                return true;
            }
        }
        return false;
    }

    private static boolean belongsToStructure(MultiblockPartState state, MultiblockStructure structure) {
        return state.isFormed()
                && MatterBatteryMultiblockDefinition.ID.equals(state.getDefinitionId())
                && structure.controllerPos().equals(state.getControllerPos());
    }
}
