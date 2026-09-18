package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BatteryTopTabCache {
    private static final Map<BlockPos, List<MatterBatteryCoreBlockEntity.PortOverview>> PORTS_BY_MEMBER = new HashMap<>();

    private BatteryTopTabCache() {
    }

    static void remember(BlockPos corePos, List<MatterBatteryCoreBlockEntity.PortOverview> ports) {
        if (corePos == null || ports.isEmpty()) {
            return;
        }

        List<MatterBatteryCoreBlockEntity.PortOverview> snapshot = List.copyOf(ports);
        PORTS_BY_MEMBER.put(corePos.immutable(), snapshot);
        for (MatterBatteryCoreBlockEntity.PortOverview port : snapshot) {
            PORTS_BY_MEMBER.put(port.pos().immutable(), snapshot);
        }
    }

    static List<MatterBatteryCoreBlockEntity.PortOverview> get(BlockPos memberPos) {
        if (memberPos == null) {
            return List.of();
        }
        return PORTS_BY_MEMBER.getOrDefault(memberPos, List.of());
    }
}
