package de.artemis.matterworks.common.debug;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;

public final class SideConfigDebugTracker {
    private static final Set<BlockEntity> CLIENT_LOADED_BLOCK_ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());

    private SideConfigDebugTracker() {
    }

    public static void onClientLoad(BlockEntity blockEntity) {
        CLIENT_LOADED_BLOCK_ENTITIES.add(blockEntity);
    }

    public static void onClientUnload(BlockEntity blockEntity) {
        CLIENT_LOADED_BLOCK_ENTITIES.remove(blockEntity);
    }

    public static Set<BlockEntity> getClientLoadedBlockEntities(Level level) {
        Set<BlockEntity> blockEntities = new LinkedHashSet<>();
        for (BlockEntity blockEntity : CLIENT_LOADED_BLOCK_ENTITIES) {
            if (blockEntity.getLevel() == level && !blockEntity.isRemoved()) {
                blockEntities.add(blockEntity);
            }
        }
        return blockEntities;
    }
}
