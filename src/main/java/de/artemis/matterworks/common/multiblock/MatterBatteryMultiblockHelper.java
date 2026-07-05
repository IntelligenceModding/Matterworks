package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
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
import java.util.List;

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
            if (!refreshExistingStructure(serverLevel, pos)) {
                MultiblockStructureRegistry.disassembleByMember(serverLevel, pos);
            }
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
        MultiblockMatch match = validateBattery(level, originPos, front, width, height, depth);
        if (match == null) {
            return false;
        }
        return MultiblockStructureRegistry.assemble(level, match).isPresent();
    }

    public static boolean tryAssembleAtOrigin(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth, @Nullable Player player, boolean notifyFailure) {
        MultiblockMatch match = validateBattery(level, originPos, front, width, height, depth);
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

        MultiblockMatch validation = validateBattery(level, structure.originPos(), structure.front(), structure.width(), structure.height(), structure.depth());
        if (validation == null) {
            MultiblockStructureRegistry.disassemble(level, structure.structureId());
            return true;
        }

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
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos worldPos = MultiblockTransforms.localToWorld(originPos, front, new BlockPos(x, y, z));
                    BlockEntity blockEntity = level.getBlockEntity(worldPos);
                    if (blockEntity instanceof MultiblockPartEntity multiblockPartEntity
                            && controllerPos.equals(multiblockPartEntity.getMultiblockPartState().getControllerPos())) {
                        multiblockPartEntity.getMultiblockPartState().clear();
                        multiblockPartEntity.getMultiblockPartState().sync(blockEntity);
                    }
                }
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

    private static @Nullable MultiblockMatch validateBattery(ServerLevel level, BlockPos originPos, Direction front, int width, int height, int depth) {
        if (!MatterBatteryMultiblockLayout.isValidSize(width, height, depth)) {
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
                    if (existing.isPresent()) {
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
}
