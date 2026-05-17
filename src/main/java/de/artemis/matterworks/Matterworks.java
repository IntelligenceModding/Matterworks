package de.artemis.matterworks;

import de.artemis.matterworks.common.capability.ModCapabilities;
import de.artemis.matterworks.common.datagen.DataGenerators;
import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModCreativeModeTabs;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.registry.ModMobEffects;
import de.artemis.matterworks.common.registry.ModParticles;
import de.artemis.matterworks.common.registry.ModPotions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Matterworks.MOD_ID)
public class Matterworks {
    public static final String MOD_ID = "matterworks";

    public Matterworks(IEventBus modEventBus) {
        MatterValueManager.registerGameEvents();
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModParticles.register(modEventBus);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(DataGenerators::gatherData);
    }
}
