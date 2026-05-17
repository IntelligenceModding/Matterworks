package de.artemis.matterworks;

import de.artemis.matterworks.client.ClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Matterworks.MOD_ID, dist = Dist.CLIENT)
public class MatterworksClient {
    public MatterworksClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::registerParticleProviders);
        modEventBus.addListener(ClientModEvents::registerScreens);
        modEventBus.addListener(ClientModEvents::registerRenderers);
    }
}
