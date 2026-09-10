package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.UUID;

public final class MultiblockPartState {
    private static final String TAG_ROOT = "multiblock_state";

    private boolean formed;
    private UUID structureId;
    private String definitionId = "";
    private BlockPos controllerPos = BlockPos.ZERO;
    private BlockPos originPos = BlockPos.ZERO;
    private BlockPos localPos = BlockPos.ZERO;
    private Direction front = Direction.NORTH;
    private MultiblockRole role = MultiblockRole.CASING;
    private int width;
    private int height;
    private int depth;

    public boolean isFormed() {
        return formed;
    }

    public UUID getStructureId() {
        return structureId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public BlockPos getLocalPos() {
        return localPos;
    }

    public Direction getFront() {
        return front;
    }

    public MultiblockRole getRole() {
        return role;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isController(BlockPos pos) {
        return formed && controllerPos.equals(pos);
    }

    public boolean applyAssembly(MultiblockStructure structure, MultiblockRole assignedRole, BlockPos assignedLocalPos) {
        if (structure == null || assignedRole == null || assignedLocalPos == null) {
            return false;
        }
        boolean changed = !formed
                || !Objects.equals(structureId, structure.structureId())
                || !Objects.equals(definitionId, structure.definitionId())
                || !Objects.equals(controllerPos, structure.controllerPos())
                || !Objects.equals(originPos, structure.originPos())
                || !Objects.equals(localPos, assignedLocalPos)
                || front != structure.front()
                || role != assignedRole
                || width != structure.width()
                || height != structure.height()
                || depth != structure.depth();
        formed = true;
        structureId = structure.structureId();
        definitionId = structure.definitionId();
        controllerPos = structure.controllerPos().immutable();
        originPos = structure.originPos().immutable();
        localPos = assignedLocalPos.immutable();
        front = structure.front();
        role = assignedRole;
        width = structure.width();
        height = structure.height();
        depth = structure.depth();
        return changed;
    }

    public boolean clear() {
        if (!formed && structureId == null && definitionId.isEmpty()) {
            return false;
        }
        formed = false;
        structureId = null;
        definitionId = "";
        controllerPos = BlockPos.ZERO;
        originPos = BlockPos.ZERO;
        localPos = BlockPos.ZERO;
        front = Direction.NORTH;
        role = MultiblockRole.CASING;
        width = 0;
        height = 0;
        depth = 0;
        return true;
    }

    public void writeToTag(CompoundTag tag) {
        CompoundTag multiblockTag = new CompoundTag();
        multiblockTag.putBoolean("formed", formed);
        if (structureId != null) {
            multiblockTag.putUUID("structure_id", structureId);
        }
        if (!definitionId.isEmpty()) {
            multiblockTag.putString("definition_id", definitionId);
        }
        multiblockTag.putInt("controller_x", controllerPos.getX());
        multiblockTag.putInt("controller_y", controllerPos.getY());
        multiblockTag.putInt("controller_z", controllerPos.getZ());
        multiblockTag.putInt("origin_x", originPos.getX());
        multiblockTag.putInt("origin_y", originPos.getY());
        multiblockTag.putInt("origin_z", originPos.getZ());
        multiblockTag.putInt("local_x", localPos.getX());
        multiblockTag.putInt("local_y", localPos.getY());
        multiblockTag.putInt("local_z", localPos.getZ());
        multiblockTag.putString("front", front.getName());
        multiblockTag.putString("role", role.name().toLowerCase());
        multiblockTag.putInt("width", width);
        multiblockTag.putInt("height", height);
        multiblockTag.putInt("depth", depth);
        tag.put(TAG_ROOT, multiblockTag);
    }

    public void readFromTag(CompoundTag tag) {
        clear();
        if (!tag.contains(TAG_ROOT, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag multiblockTag = tag.getCompound(TAG_ROOT);
        formed = multiblockTag.getBoolean("formed");
        structureId = multiblockTag.hasUUID("structure_id") ? multiblockTag.getUUID("structure_id") : null;
        definitionId = multiblockTag.getString("definition_id");
        controllerPos = new BlockPos(multiblockTag.getInt("controller_x"), multiblockTag.getInt("controller_y"), multiblockTag.getInt("controller_z"));
        originPos = new BlockPos(multiblockTag.getInt("origin_x"), multiblockTag.getInt("origin_y"), multiblockTag.getInt("origin_z"));
        localPos = new BlockPos(multiblockTag.getInt("local_x"), multiblockTag.getInt("local_y"), multiblockTag.getInt("local_z"));
        front = Direction.byName(multiblockTag.getString("front"));
        if (front == null || !front.getAxis().isHorizontal()) {
            front = Direction.NORTH;
        }
        String serializedRole = multiblockTag.getString("role");
        role = switch (serializedRole) {
            case "controller" -> MultiblockRole.CONTROLLER;
            case "port" -> MultiblockRole.PORT;
            case "frame" -> MultiblockRole.FRAME;
            case "internal" -> MultiblockRole.INTERNAL;
            default -> MultiblockRole.CASING;
        };
        width = Math.max(0, multiblockTag.getInt("width"));
        height = Math.max(0, multiblockTag.getInt("height"));
        depth = Math.max(0, multiblockTag.getInt("depth"));
    }

    public boolean sync(BlockEntity blockEntity) {
        blockEntity.setChanged();
        if (blockEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            return true;
        }
        return false;
    }
}
