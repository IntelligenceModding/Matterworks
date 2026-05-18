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
    private final List<MultiblockMatchedPart> parts;

    public MultiblockMatch(MultiblockDefinition definition, BlockPos controllerPos, BlockPos originPos, Direction front, List<MultiblockMatchedPart> parts) {
        this.matchId = UUID.randomUUID();
        this.definition = definition;
        this.controllerPos = controllerPos.immutable();
        this.originPos = originPos.immutable();
        this.front = front;
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

    public List<MultiblockMatchedPart> getParts() {
        return parts;
    }
}
