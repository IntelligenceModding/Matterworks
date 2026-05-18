package de.artemis.matterworks.common.io;

import net.minecraft.core.Direction;

public interface SideConfigurableBlockEntity {
    boolean supportsSideConfigType(SideConfigType type);

    boolean supportsSideConfigInput(SideConfigType type);

    boolean supportsSideConfigOutput(SideConfigType type);

    SideAccessMode getSideAccessMode(SideConfigType type, Direction side);

    void setSideAccessMode(SideConfigType type, Direction side, SideAccessMode mode);
}
