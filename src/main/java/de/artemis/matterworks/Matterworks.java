package de.artemis.matterworks;

import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModCreativeModeTabs;
import de.artemis.matterworks.common.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Matterworks.MOD_ID)
public class Matterworks {
    public static final String MOD_ID = "matterworks";

    public Matterworks(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
    }
}
