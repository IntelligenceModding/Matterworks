package de.artemis.matterworks;

import de.artemis.matterworks.common.capability.ModCapabilities;
import de.artemis.matterworks.common.datagen.DataGenerators;
import de.artemis.matterworks.common.event.PylonNetworkEvents;
import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.network.ModPayloads;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModCreativeModeTabs;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.registry.ModMobEffects;
import de.artemis.matterworks.common.registry.ModParticles;
import de.artemis.matterworks.common.registry.ModPotions;
import de.artemis.matterworks.common.registry.ModRecipeSerializers;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Matterworks.MOD_ID)
public class Matterworks {
    public static final String MOD_ID = "matterworks";

    public Matterworks(IEventBus modEventBus) {
        MatterValueManager.registerGameEvents();
        NeoForge.EVENT_BUS.register(PylonNetworkEvents.class);
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModParticles.register(modEventBus);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(PylonChunkLoading::registerTicketControllers);
        modEventBus.addListener(DataGenerators::gatherData);
    }
}
