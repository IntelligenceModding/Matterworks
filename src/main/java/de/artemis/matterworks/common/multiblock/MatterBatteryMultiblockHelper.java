package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MatterBatteryMultiblockHelper {
    private static final int SEARCH_RADIUS = 4;

    private MatterBatteryMultiblockHelper() {
    }

    public static void onBlockPlaced(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            if (refreshExistingStructure(serverLevel, pos)) {
                return;
            }
            tryAssembleNearby(serverLevel, pos);
        }
    }

    public static void onBlockRemoved(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            if (!refreshExistingStructure(serverLevel, pos)) {
                MultiblockStructureRegistry.disassembleByMember(serverLevel, pos);
            }
        }
    }

    public static void tryAssembleNearby(ServerLevel level, BlockPos origin) {
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockEntity blockEntity = level.getBlockEntity(origin.offset(x, y, z));
                    if (blockEntity instanceof MatterBatteryCoreBlockEntity controller) {
                        controller.tryAssemble(null, false);
                    }
                }
            }
        }
    }

    public static boolean tryOpenBatteryMenu(ServerLevel level, BlockPos memberPos, Player player) {
        var structureOptional = MultiblockStructureRegistry.getByMember(level, memberPos);
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
                controller.getDisplayName()
        );
        player.openMenu(provider, controller.getBlockPos());
        return true;
    }

    public static boolean recoverStructure(ServerLevel level, BlockPos controllerPos, Direction front) {
        MultiblockValidationResult validation = MultiblockStructureRegistry.validate(level, controllerPos, front, MatterBatteryMultiblockDefinition.INSTANCE);
        if (!validation.success() || validation.match() == null) {
            return false;
        }
        return MultiblockStructureRegistry.assemble(level, validation.match()).isPresent();
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

        MultiblockValidationResult validation = MultiblockAssembler.validate(level, structure.controllerPos(), structure.front(), MatterBatteryMultiblockDefinition.INSTANCE);
        if (!validation.success()) {
            MultiblockStructureRegistry.disassemble(level, structure.structureId());
            return true;
        }

        controller.refreshStructureStats(level, structure);
        return true;
    }

    public static void clearStoredStates(ServerLevel level, BlockPos originPos, BlockPos controllerPos, Direction front) {
        for (int y = 0; y < MatterBatteryMultiblockDefinition.STRUCTURE_SIZE; y++) {
            for (int z = 0; z < MatterBatteryMultiblockDefinition.STRUCTURE_SIZE; z++) {
                for (int x = 0; x < MatterBatteryMultiblockDefinition.STRUCTURE_SIZE; x++) {
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

    public static Direction getOutwardSide(BlockPos localPos, Direction front) {
        Direction right = front.getClockWise();
        if (localPos.getY() == 0) {
            return Direction.DOWN;
        }
        if (localPos.getY() == MatterBatteryMultiblockDefinition.MAX_OFFSET) {
            return Direction.UP;
        }
        if (localPos.getZ() == 0) {
            return front.getOpposite();
        }
        if (localPos.getZ() == MatterBatteryMultiblockDefinition.MAX_OFFSET) {
            return front;
        }
        if (localPos.getX() == 0) {
            return right.getOpposite();
        }
        if (localPos.getX() == MatterBatteryMultiblockDefinition.MAX_OFFSET) {
            return right;
        }
        return front;
    }
}
