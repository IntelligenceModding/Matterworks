package de.artemis.matterworks.common.event;

import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber
public final class PylonNetworkEvents {
    private PylonNetworkEvents() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MatterPylonBlockEntity.clearServerLevelState(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MatterPylonBlockEntity.clearAllSharedState();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MatterPylonBlockEntity.clearAllSharedState();
    }
}
