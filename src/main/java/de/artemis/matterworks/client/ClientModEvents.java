package de.artemis.matterworks.client;

import de.artemis.matterworks.client.particle.RawMatterDripParticle;
import de.artemis.matterworks.client.render.MatterPylonBlockEntityRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingHudRenderer;
import de.artemis.matterworks.client.render.PowerCrystalOreBlockEntityRenderer;
import de.artemis.matterworks.client.screen.MatterAnalyzerScreen;
import de.artemis.matterworks.client.screen.MatterConstructorScreen;
import de.artemis.matterworks.client.screen.MatterEnergyCellScreen;
import de.artemis.matterworks.client.screen.MatterFilterScreen;
import de.artemis.matterworks.client.screen.MatterFluidTankScreen;
import de.artemis.matterworks.client.screen.MatterGeneratorScreen;
import de.artemis.matterworks.client.screen.MatterNetworkControllerScreen;
import de.artemis.matterworks.client.screen.MatterNetworkMonitorScreen;
import de.artemis.matterworks.client.screen.MatterPylonScreen;
import de.artemis.matterworks.client.screen.MatterRecyclerScreen;
import de.artemis.matterworks.client.screen.MatterSeparatorScreen;
import de.artemis.matterworks.client.screen.MatterStorageBarrelScreen;
import de.artemis.matterworks.client.screen.MatterStabilizerScreen;
import de.artemis.matterworks.client.screen.PowerCrystalChargerScreen;
import de.artemis.matterworks.client.tooltip.MatterFilterClientTooltipComponent;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.registry.ModParticles;
import de.artemis.matterworks.common.tooltip.MatterFilterTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class ClientModEvents {
    private ClientModEvents() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MATTER_RECYCLER.get(), MatterRecyclerScreen::new);
        event.register(ModMenuTypes.MATTER_STABILIZER.get(), MatterStabilizerScreen::new);
        event.register(ModMenuTypes.MATTER_ANALYZER.get(), MatterAnalyzerScreen::new);
        event.register(ModMenuTypes.MATTER_CONSTRUCTOR.get(), MatterConstructorScreen::new);
        event.register(ModMenuTypes.MATTER_GENERATOR.get(), MatterGeneratorScreen::new);
        event.register(ModMenuTypes.MATTER_ENERGY_CELL.get(), MatterEnergyCellScreen::new);
        event.register(ModMenuTypes.MATTER_FILTER.get(), MatterFilterScreen::new);
        event.register(ModMenuTypes.MATTER_FLUID_TANK.get(), MatterFluidTankScreen::new);
        event.register(ModMenuTypes.MATTER_STORAGE_BARREL.get(), MatterStorageBarrelScreen::new);
        event.register(ModMenuTypes.MATTER_PYLON.get(), MatterPylonScreen::new);
        event.register(ModMenuTypes.MATTER_NETWORK_CONTROLLER.get(), MatterNetworkControllerScreen::new);
        event.register(ModMenuTypes.MATTER_NETWORK_MONITOR.get(), MatterNetworkMonitorScreen::new);
        event.register(ModMenuTypes.MATTER_SEPARATOR.get(), MatterSeparatorScreen::new);
        event.register(ModMenuTypes.POWER_CRYSTAL_CHARGER.get(), PowerCrystalChargerScreen::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DRIPPING_RAW_MATTER.get(), RawMatterDripParticle.DrippingProvider::new);
        event.registerSpriteSet(ModParticles.FALLING_RAW_MATTER.get(), RawMatterDripParticle.FallingProvider::new);
        event.registerSpriteSet(ModParticles.LANDING_RAW_MATTER.get(), RawMatterDripParticle.LandingProvider::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_PYLON.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_CONTROLLER.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_MONITOR.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_ENERGY_CELL.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_FLUID_TANK.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_STORAGE_BARREL.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POWER_CRYSTAL_ORE.get(), PowerCrystalOreBlockEntityRenderer::new);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MatterFilterTooltip.class, MatterFilterClientTooltipComponent::new);
    }

    public static void addGuiOverlayLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.EXPERIENCE_BAR,
                ResourceLocation.fromNamespaceAndPath("matterworks", "matter_network_tracking_hud"),
                MatterNetworkTrackingHudRenderer.LAYER
        );
    }

    public static void renderMatterNetworkLinks(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        MatterPylonBlockEntityRenderer.renderMatterNetworkLinks(
                MatterPylonBlockEntity.getClientLoadedNodes(minecraft.level),
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition(),
                minecraft.level.getGameTime()
        );
        MatterPylonBlockEntityRenderer.renderSideConfigOverlays(
                SideConfigDebugTracker.getClientLoadedBlockEntities(minecraft.level),
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition()
        );
        MatterNetworkTrackingRenderer.renderTrackingOverlay(
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition()
        );
    }
}
