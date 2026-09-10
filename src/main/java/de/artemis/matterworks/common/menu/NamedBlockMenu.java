package de.artemis.matterworks.common.menu;

import net.minecraft.core.BlockPos;

public interface NamedBlockMenu {
    BlockPos getBlockPos();

    String getBlockDisplayName();

    default boolean isRemoteAccess() {
        return false;
    }
}
