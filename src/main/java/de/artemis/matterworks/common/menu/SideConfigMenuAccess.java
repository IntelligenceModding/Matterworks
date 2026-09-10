package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    default List<SideAccessMode> getAllowedSideAccessModes(SideConfigType type) {
        List<SideAccessMode> modes = new ArrayList<>();
        modes.add(SideAccessMode.DISABLED);
        if (supportsSideConfigInput(type)) {
            modes.add(SideAccessMode.INPUT);
        }
        if (supportsSideConfigOutput(type)) {
            modes.add(SideAccessMode.OUTPUT);
        }
        if (supportsSideConfigInput(type) && supportsSideConfigOutput(type)) {
            modes.add(SideAccessMode.BOTH);
        }
        return modes;
    }

    default String getSideAccessModeLabel(SideConfigType type, Direction side, SideAccessMode mode) {
        return mode.getShortLabel();
    }

    default String getSideAccessModeShortLabel(SideConfigType type, Direction side, SideAccessMode mode) {
        return mode.getShortLabel();
    }
}
