package de.artemis.matterworks.client;

import de.artemis.matterworks.client.render.PowerCrystalOreBlockEntityRenderer;
import de.artemis.matterworks.client.screen.MatterAnalyzerScreen;
import de.artemis.matterworks.client.screen.MatterConstructorScreen;
import de.artemis.matterworks.client.screen.MatterGeneratorScreen;
import de.artemis.matterworks.client.screen.MatterRecyclerScreen;
import de.artemis.matterworks.client.screen.MatterSeparatorScreen;
import de.artemis.matterworks.client.screen.MatterStabilizerScreen;
import de.artemis.matterworks.client.screen.PowerCrystalChargerScreen;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientModEvents {
    private ClientModEvents() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MATTER_RECYCLER.get(), MatterRecyclerScreen::new);
        event.register(ModMenuTypes.MATTER_STABILIZER.get(), MatterStabilizerScreen::new);
        event.register(ModMenuTypes.MATTER_ANALYZER.get(), MatterAnalyzerScreen::new);
        event.register(ModMenuTypes.MATTER_CONSTRUCTOR.get(), MatterConstructorScreen::new);
        event.register(ModMenuTypes.MATTER_GENERATOR.get(), MatterGeneratorScreen::new);
        event.register(ModMenuTypes.MATTER_SEPARATOR.get(), MatterSeparatorScreen::new);
        event.register(ModMenuTypes.POWER_CRYSTAL_CHARGER.get(), PowerCrystalChargerScreen::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.POWER_CRYSTAL_ORE.get(), PowerCrystalOreBlockEntityRenderer::new);
    }
}
