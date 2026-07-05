package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.UUID;

public final class MultiblockMatch {
    private final UUID matchId;
    private final MultiblockDefinition definition;
    private final BlockPos controllerPos;
    private final BlockPos originPos;
    private final Direction front;
    private final int width;
    private final int height;
    private final int depth;
    private final List<MultiblockMatchedPart> parts;

    public MultiblockMatch(MultiblockDefinition definition, BlockPos controllerPos, BlockPos originPos, Direction front, List<MultiblockMatchedPart> parts) {
        this(definition, controllerPos, originPos, front, definition.getPattern().getWidth(), definition.getPattern().getHeight(), definition.getPattern().getDepth(), parts);
    }

    public MultiblockMatch(MultiblockDefinition definition, BlockPos controllerPos, BlockPos originPos, Direction front, int width, int height, int depth, List<MultiblockMatchedPart> parts) {
        this.matchId = UUID.randomUUID();
        this.definition = definition;
        this.controllerPos = controllerPos.immutable();
        this.originPos = originPos.immutable();
        this.front = front;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.parts = List.copyOf(parts);
    }

    public UUID getMatchId() {
        return matchId;
    }

    public MultiblockDefinition getDefinition() {
        return definition;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public Direction getFront() {
        return front;
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

    public List<MultiblockMatchedPart> getParts() {
        return parts;
    }
}
