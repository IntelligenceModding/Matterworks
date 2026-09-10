package de.artemis.matterworks.common.io;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public interface SideConfigurableBlockEntity {
    boolean supportsSideConfigType(SideConfigType type);

    boolean supportsSideConfigInput(SideConfigType type);

    boolean supportsSideConfigOutput(SideConfigType type);

    SideAccessMode getSideAccessMode(SideConfigType type, Direction side);

    void setSideAccessMode(SideConfigType type, Direction side, SideAccessMode mode);

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
