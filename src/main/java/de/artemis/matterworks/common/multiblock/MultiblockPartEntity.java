package de.artemis.matterworks.common.multiblock;

public interface MultiblockPartEntity {
    MultiblockPartState getMultiblockPartState();

    default void onMultiblockAssembled(MultiblockStructure structure, MultiblockRole role) {
    }

    default void onMultiblockDisassembled(MultiblockStructure structure) {
    }
}
