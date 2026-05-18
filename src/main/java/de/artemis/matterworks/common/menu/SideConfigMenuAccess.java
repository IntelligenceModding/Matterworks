package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface SideConfigMenuAccess extends NamedBlockMenu {
    boolean supportsSideConfigType(SideConfigType type);

    boolean supportsSideConfigInput(SideConfigType type);

    boolean supportsSideConfigOutput(SideConfigType type);

    SideAccessMode getSideAccessMode(SideConfigType type, Direction side);

    Direction getSideConfigFrontFacing();

    ItemStack getPrimaryTabIcon();

    default boolean hasNetworkTab() {
        return false;
    }

    default boolean isRemoteAccess() {
        return false;
    }
}
