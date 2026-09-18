package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import de.artemis.matterworks.common.block.MatterBatteryCoreBlock;
import de.artemis.matterworks.common.block.MatterCapacitorCellBlock;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
    private static final ThreadLocal<Boolean> SUPPRESS_STRUCTURE_REFRESH = ThreadLocal.withInitial(() -> false);

    private MatterBatteryMultiblockHelper() {
    }

    public static void onBlockPlaced(Level level, BlockPos pos) {
        if (SUPPRESS_STRUCTURE_REFRESH.get()) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (refreshExistingStructure(serverLevel, pos)) {
                return;
            }
            recoverNearbyStoredBattery(serverLevel, pos);
        }
    }

    public static void onBlockRemoved(Level level, BlockPos pos) {
        if (SUPPRESS_STRUCTURE_REFRESH.get()) {
            return;
        }
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

    public static void runWithoutStructureRefresh(Runnable action) {
        boolean previous = SUPPRESS_STRUCTURE_REFRESH.get();
        SUPPRESS_STRUCTURE_REFRESH.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_STRUCTURE_REFRESH.set(previous);
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

        BlockPos initialPortPos = level.getBlockEntity(memberPos) instanceof MultiblockPortBlockEntity ? memberPos.immutable() : null;
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new MatterBatteryCoreMenu(containerId, playerInventory, controller, controller.getData(), initialPortPos),
                net.minecraft.network.chat.Component.literal("Matter Battery")
        );
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> {
                buffer.writeBlockPos(controller.getBlockPos());
                if (initialPortPos != null) {
                    buffer.writeBlockPos(initialPortPos);
                }
            });
            return true;
        }
        return false;
    }

    public static ItemInteractionResult useFormedBatteryMemberWithItem(Level level, BlockPos memberPos, Player player) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return isKnownFormedBatteryMember(level, memberPos) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel serverLevel && tryOpenBatteryMenu(serverLevel, memberPos, player)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static boolean isKnownFormedBatteryMember(Level level, BlockPos memberPos) {
        return findKnownBatteryMemberState(level, memberPos) != null;
    }

    public static boolean wouldExceedPortLimitForKnownMember(Level level, BlockPos memberPos, BlockState replacementState) {
        if (!replacementState.is(ModBlocks.MULTIBLOCK_PORT.get())) {
            return false;
        }

        MultiblockPartState state = findKnownBatteryMemberState(level, memberPos);
        if (state == null) {
            return false;
        }

        return countPorts(level, state.getOriginPos(), state.getFront(), state.getWidth(), state.getHeight(), state.getDepth(), memberPos, replacementState) > MatterBatteryMultiblockLayout.MAX_PORTS;
    }

    private static @Nullable MultiblockPartState findKnownBatteryMemberState(Level level, BlockPos memberPos) {
        BlockEntity directBlockEntity = level.getBlockEntity(memberPos);
        if (directBlockEntity instanceof MultiblockPartEntity directPart
                && isBatteryMemberState(directPart.getMultiblockPartState(), memberPos)) {
            return directPart.getMultiblockPartState();
        }

        int radius = MatterBatteryMultiblockLayout.MAX_SIZE;
        BlockPos minSearch = memberPos.offset(-radius, -radius, -radius);
        BlockPos maxSearch = memberPos.offset(radius, radius, radius);
        for (BlockPos scanPos : BlockPos.betweenClosed(minSearch, maxSearch)) {
            BlockEntity blockEntity = level.getBlockEntity(scanPos);
            if (blockEntity instanceof MultiblockPartEntity multiblockPartEntity
                    && isBatteryMemberState(multiblockPartEntity.getMultiblockPartState(), memberPos)) {
                return multiblockPartEntity.getMultiblockPartState();
            }
        }
        return null;
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

    public static boolean wouldExceedPortLimit(Level level, BlockPos originPos, Direction front, int width, int height, int depth, BlockPos replacementPos, BlockState replacementState) {
        if (!replacementState.is(ModBlocks.MULTIBLOCK_PORT.get())) {
            return false;
        }
        return countPorts(level, originPos, front, width, height, depth, replacementPos, replacementState) > MatterBatteryMultiblockLayout.MAX_PORTS;
    }

    private static @Nullable MultiblockMatch validateBattery(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth, @Nullable UUID allowedStructureId) {
        if (!front.getAxis().isHorizontal() || !MatterBatteryMultiblockLayout.isValidSize(width, height, depth)) {
            return null;
        }

        List<MultiblockMatchedPart> matchedParts = new ArrayList<>(width * height * depth);
        BlockPos controllerLocalPos = MatterBatteryMultiblockLayout.getControllerOffset(width, height, depth);
        BlockPos controllerPos = MultiblockTransforms.localToWorld(originPos, front, controllerLocalPos);
        int portCount = 0;

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, localPos);
                    BlockState state = level.getBlockState(worldPos);
                    BlockEntity blockEntity = level.getBlockEntity(worldPos);
                    MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, width, height, depth);
                    String description = MatterBatteryMultiblockLayout.getRequirementDescription(localPos, width, height, depth);
                    if (!matchesRequirement(role, state, allowedStructureId != null)) {
                        return null;
                    }
                    if (state.is(ModBlocks.MULTIBLOCK_PORT.get()) && ++portCount > MatterBatteryMultiblockLayout.MAX_PORTS) {
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

    private static boolean matchesRequirement(MultiblockRole role, net.minecraft.world.level.block.state.BlockState state, boolean existingStructureRefresh) {
        return switch (role) {
            case CONTROLLER -> state.is(ModBlocks.MATTER_BATTERY_CORE.get());
            case FRAME -> MatterBatteryMultiblockDefinition.matchesFrameState(state);
            case CASING, PORT -> MatterBatteryMultiblockDefinition.matchesShellFaceState(state);
            case INTERNAL -> MatterBatteryMultiblockDefinition.matchesInternalState(state);
        };
    }

    private static int countPorts(Level level, BlockPos originPos, Direction front, int width, int height, int depth, @Nullable BlockPos replacementPos, @Nullable BlockState replacementState) {
        int ports = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, localPos);
                    BlockState state = replacementPos != null && replacementPos.equals(worldPos) && replacementState != null
                            ? replacementState
                            : level.getBlockState(worldPos);
                    if (state.is(ModBlocks.MULTIBLOCK_PORT.get())) {
                        ports++;
                    }
                }
            }
        }
        return ports;
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

    private static boolean isBatteryMemberState(MultiblockPartState state, BlockPos memberPos) {
        if (!state.isFormed()
                || !MatterBatteryMultiblockDefinition.ID.equals(state.getDefinitionId())
                || !state.getFront().getAxis().isHorizontal()
                || !MatterBatteryMultiblockLayout.isValidSize(state.getWidth(), state.getHeight(), state.getDepth())) {
            return false;
        }

        BlockPos localPos = MultiblockTransforms.worldToLocal(state.getOriginPos(), state.getFront(), memberPos);
        if (!MultiblockTransforms.localToWorld(state.getOriginPos(), state.getFront(), localPos).equals(memberPos)) {
            return false;
        }
        return localPos.getX() >= 0 && localPos.getX() < state.getWidth()
                && localPos.getY() >= 0 && localPos.getY() < state.getHeight()
                && localPos.getZ() >= 0 && localPos.getZ() < state.getDepth();
    }
}
