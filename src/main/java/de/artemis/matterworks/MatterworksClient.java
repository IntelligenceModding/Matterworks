package de.artemis.matterworks;

import de.artemis.matterworks.client.ClientModEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Matterworks.MOD_ID, dist = Dist.CLIENT)
public class MatterworksClient {
    public MatterworksClient(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(ClientModEvents::renderMatterNetworkLinks);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onInteractionKeyMappingTriggered);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onMouseScrolling);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        modEventBus.addListener(ClientModEvents::addGuiOverlayLayers);
        modEventBus.addListener(ClientModEvents::onClientSetup);
        modEventBus.addListener(ClientModEvents::registerParticleProviders);
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
        modEventBus.addListener(ClientModEvents::registerScreens);
        modEventBus.addListener(ClientModEvents::registerRenderers);
        modEventBus.addListener(ClientModEvents::registerTooltipComponents);
        modEventBus.addListener(ClientModEvents::registerItemDecorations);
    }
}
