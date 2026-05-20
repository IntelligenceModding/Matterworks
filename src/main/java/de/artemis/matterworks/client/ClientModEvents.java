package de.artemis.matterworks.client;

import de.artemis.matterworks.client.particle.RawMatterDripParticle;
import de.artemis.matterworks.client.render.MatterBatteryFormationRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewHudRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewState;
import de.artemis.matterworks.client.render.MatterPylonBlockEntityRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingHudRenderer;
import de.artemis.matterworks.client.render.PowerCrystalOreBlockEntityRenderer;
import de.artemis.matterworks.client.screen.MatterAnalyzerScreen;
import de.artemis.matterworks.client.screen.MatterBatteryCoreScreen;
import de.artemis.matterworks.client.screen.MatterBatteryPreviewScreen;
import de.artemis.matterworks.client.screen.MatterConstructorScreen;
import de.artemis.matterworks.client.screen.EnergyCellScreen;
import de.artemis.matterworks.client.screen.MatterFilterScreen;
import de.artemis.matterworks.client.screen.FluidTankScreen;
import de.artemis.matterworks.client.screen.CombustionGeneratorScreen;
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
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import de.artemis.matterworks.common.network.PlaceMatterBatteryPreviewBlockPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.registry.ModParticles;
import de.artemis.matterworks.common.tooltip.MatterFilterTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientModEvents {
    private ClientModEvents() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MATTER_RECYCLER.get(), MatterRecyclerScreen::new);
        event.register(ModMenuTypes.MATTER_STABILIZER.get(), MatterStabilizerScreen::new);
        event.register(ModMenuTypes.MATTER_ANALYZER.get(), MatterAnalyzerScreen::new);
        event.register(ModMenuTypes.MATTER_CONSTRUCTOR.get(), MatterConstructorScreen::new);
        event.register(ModMenuTypes.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenuTypes.MATTER_ENERGY_CELL.get(), EnergyCellScreen::new);
        event.register(ModMenuTypes.MATTER_BATTERY_CORE.get(), MatterBatteryCoreScreen::new);
        event.register(ModMenuTypes.MATTER_BATTERY_PREVIEW.get(), MatterBatteryPreviewScreen::new);
        event.register(ModMenuTypes.MATTER_FILTER.get(), MatterFilterScreen::new);
        event.register(ModMenuTypes.FLUID_TANK.get(), FluidTankScreen::new);
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

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModBlocks.MULTIBLOCK_GLASS.get(), RenderType.translucent()));
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_PYLON.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_CONTROLLER.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_MONITOR.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_ENERGY_CELL.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_TANK.get(), MatterPylonBlockEntityRenderer::new);
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
        event.registerAbove(
                VanillaGuiLayers.EXPERIENCE_BAR,
                ResourceLocation.fromNamespaceAndPath("matterworks", "matter_battery_preview_hud"),
                MatterBatteryPreviewHudRenderer.LAYER
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
        MatterBatteryPreviewRenderer.renderPreviewOverlay(
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition()
        );
        MatterBatteryFormationRenderer.renderFormationOverlay(
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition(),
                minecraft.level.getGameTime()
        );
    }

    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        MatterBatteryPreviewRenderer.PreviewHit previewHit = MatterBatteryPreviewRenderer.findTargetedPreviewHit(minecraft);
        if (previewHit == null) {
            return;
        }

        if (MatterBatteryPreviewRenderer.matchesRequirement(previewHit.requirement(), previewHit.actualState())) {
            return;
        }

        if (!previewHit.actualState().isAir() && !previewHit.actualState().canBeReplaced()) {
            return;
        }

        event.setCanceled(true);
        if (!MatterBatteryPreviewPlacementHelper.canPlaceFromInventory(minecraft.player, event.getHand(), previewHit.requirement())) {
            event.setSwingHand(false);
            minecraft.player.displayClientMessage(Component.literal("Missing: " + MatterBatteryPreviewRenderer.getRequirementLabel(previewHit.localPos())), true);
            return;
        }

        event.setSwingHand(true);
        PacketDistributor.sendToServer(new PlaceMatterBatteryPreviewBlockPayload(
                MatterBatteryPreviewState.getControllerPos(),
                previewHit.worldPos(),
                event.getHand().ordinal()
        ));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        MatterBatteryFormationRenderer.tickParticles();
    }
}

