package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.io.SideConfigType;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class PendingMachineTabSelection {
    private static final Map<BlockPos, SideConfigType> PENDING_SELECTIONS = new HashMap<>();

    private PendingMachineTabSelection() {
    }

    public static void set(BlockPos pos, SideConfigType type) {
        if (type == null) {
            PENDING_SELECTIONS.remove(pos);
            return;
        }
        PENDING_SELECTIONS.put(pos.immutable(), type);
    }

    public static SideConfigType consume(BlockPos pos) {
        return PENDING_SELECTIONS.remove(pos);
    }

    public static void clear(BlockPos pos) {
        PENDING_SELECTIONS.remove(pos);
    }
}
