package de.artemis.matterworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.artemis.matterworks.client.particle.RawMatterDripParticle;
import de.artemis.matterworks.client.render.MatterBatteryFormationRenderer;
import de.artemis.matterworks.client.render.MatterBatteryCoreBlockEntityRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewHudRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewRenderer;
import de.artemis.matterworks.client.render.MatterBatteryPreviewState;
import de.artemis.matterworks.client.render.MatterPylonBlockEntityRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingRenderer;
import de.artemis.matterworks.client.render.MatterNetworkTrackingHudRenderer;
import de.artemis.matterworks.client.render.PowerCrystalOreBlockEntityRenderer;
import de.artemis.matterworks.client.render.SingularityLinkBlockEntityRenderer;
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
import de.artemis.matterworks.client.screen.GraviticCondenserScreen;
import de.artemis.matterworks.client.screen.MatterStorageBarrelScreen;
import de.artemis.matterworks.client.screen.MatterStabilizerScreen;
import de.artemis.matterworks.client.screen.PowerCrystalChargerScreen;
import de.artemis.matterworks.client.screen.SingularityLinkScreen;
import de.artemis.matterworks.client.tooltip.MatterFilterClientTooltipComponent;
import de.artemis.matterworks.client.tooltip.LinkedBlockItemDecorator;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.item.MatterArchitectItem;
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import de.artemis.matterworks.common.network.MoveMatterArchitectSelectionPayload;
import de.artemis.matterworks.common.network.PlaceMatterBatteryPreviewBlockPayload;
import de.artemis.matterworks.common.network.ResizeMatterArchitectSelectionPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.registry.ModParticles;
import de.artemis.matterworks.common.tooltip.MatterFilterTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class ClientModEvents {
    private static final String KEY_CATEGORY = "key.categories.matterworks";
    private static final KeyMapping ARCHITECT_LAYER_UP = new KeyMapping("key.matterworks.architect.layer_up", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, KEY_CATEGORY);
    private static final KeyMapping ARCHITECT_LAYER_DOWN = new KeyMapping("key.matterworks.architect.layer_down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, KEY_CATEGORY);

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
        event.register(ModMenuTypes.SINGULARITY_LINK.get(), SingularityLinkScreen::new);
        event.register(ModMenuTypes.MATTER_NETWORK_CONTROLLER.get(), MatterNetworkControllerScreen::new);
        event.register(ModMenuTypes.MATTER_NETWORK_MONITOR.get(), MatterNetworkMonitorScreen::new);
        event.register(ModMenuTypes.MATTER_SEPARATOR.get(), MatterSeparatorScreen::new);
        event.register(ModMenuTypes.GRAVITIC_CONDENSER.get(), GraviticCondenserScreen::new);
        event.register(ModMenuTypes.POWER_CRYSTAL_CHARGER.get(), PowerCrystalChargerScreen::new);
    }

    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DRIPPING_RAW_MATTER.get(), RawMatterDripParticle.DrippingProvider::new);
        event.registerSpriteSet(ModParticles.FALLING_RAW_MATTER.get(), RawMatterDripParticle.FallingProvider::new);
        event.registerSpriteSet(ModParticles.LANDING_RAW_MATTER.get(), RawMatterDripParticle.LandingProvider::new);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ARCHITECT_LAYER_UP);
        event.register(ARCHITECT_LAYER_DOWN);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MULTIBLOCK_GLASS.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SINGULARITY_LINK.get(), RenderType.translucent());
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_PYLON.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SINGULARITY_LINK.get(), SingularityLinkBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_CONTROLLER.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_NETWORK_MONITOR.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_ENERGY_CELL.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_BATTERY_CORE.get(), MatterBatteryCoreBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_TANK.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MATTER_STORAGE_BARREL.get(), MatterPylonBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POWER_CRYSTAL_ORE.get(), PowerCrystalOreBlockEntityRenderer::new);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MatterFilterTooltip.class, MatterFilterClientTooltipComponent::new);
    }

    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        LinkedBlockItemDecorator decorator = new LinkedBlockItemDecorator();
        event.register(ModItems.NETWORK_DATA_CARD.get(), decorator);
        event.register(ModItems.NETWORK_REMOTE_TERMINAL.get(), decorator);
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
        SingularityLinkBlockEntityRenderer.renderStructureOverlays(
                MatterPylonBlockEntity.getClientLoadedNodes(minecraft.level),
                event.getPoseStack(),
                minecraft.renderBuffers().bufferSource(),
                event.getCamera().getPosition(),
                minecraft.level.getGameTime()
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

        if (MatterBatteryPreviewPlacementHelper.matchesRequirement(previewHit.role(), previewHit.actualState())) {
            return;
        }

        if (!previewHit.actualState().isAir() && !previewHit.actualState().canBeReplaced()) {
            return;
        }

        event.setCanceled(true);
        if (!MatterBatteryPreviewPlacementHelper.canPlaceFromInventory(minecraft.player, event.getHand(), previewHit.role())) {
            event.setSwingHand(false);
            minecraft.player.displayClientMessage(Component.literal("Missing: " + MatterBatteryPreviewRenderer.getRequirementLabel(previewHit.localPos())), true);
            return;
        }

        event.setSwingHand(true);
        PacketDistributor.sendToServer(new PlaceMatterBatteryPreviewBlockPayload(
                MatterBatteryPreviewState.getOriginPos(),
                MatterBatteryPreviewState.getFront().ordinal(),
                MatterBatteryPreviewState.getWidth(),
                MatterBatteryPreviewState.getHeight(),
                MatterBatteryPreviewState.getDepth(),
                previewHit.worldPos(),
                event.getHand().ordinal()
        ));
    }

    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        HeldArchitect heldArchitect = getHeldArchitect(minecraft);
        if (heldArchitect == null) {
            return;
        }

        int amount = normalizeScrollAmount(event.getScrollDeltaY());
        if (amount == 0) {
            return;
        }

        boolean controlDown = isControlDown(minecraft);
        boolean shiftDown = minecraft.player.isShiftKeyDown();

        if (MatterArchitectItem.isLocked(heldArchitect.stack())) {
            if (!shiftDown || controlDown || !MatterArchitectItem.hasCornerB(heldArchitect.stack())) {
                return;
            }
            event.setCanceled(true);
            MatterBatteryPreviewState.cycleLayer(amount);
            return;
        }

        if (!controlDown && !shiftDown) {
            return;
        }

        event.setCanceled(true);
        if (!MatterArchitectItem.hasCornerB(heldArchitect.stack())) {
            return;
        }

        Direction lookDirection = getDominantLookDirection(minecraft);
        if (controlDown) {
            if (MatterArchitectItem.resizeSelection(heldArchitect.stack(), lookDirection, amount)) {
                MatterArchitectItem.syncPreviewState(heldArchitect.stack(), getHoveredBlockPos(minecraft), minecraft.player.getDirection());
                PacketDistributor.sendToServer(new ResizeMatterArchitectSelectionPayload(
                        heldArchitect.hand().ordinal(),
                        lookDirection.ordinal(),
                        amount
                ));
            }
            return;
        }

        int dx = lookDirection.getStepX() * amount;
        int dy = lookDirection.getStepY() * amount;
        int dz = lookDirection.getStepZ() * amount;

        if (dx == 0 && dy == 0 && dz == 0) {
            return;
        }

        MatterArchitectItem.shiftSelection(heldArchitect.stack(), dx, dy, dz);
        MatterBatteryPreviewState.shift(dx, dy, dz);
        PacketDistributor.sendToServer(new MoveMatterArchitectSelectionPayload(heldArchitect.hand().ordinal(), dx, dy, dz));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            boolean syncedFromTool = false;
            HeldArchitect heldArchitect = getHeldArchitect(minecraft);
            if (heldArchitect != null) {
                MatterArchitectItem.syncPreviewState(heldArchitect.stack(), getHoveredBlockPos(minecraft), minecraft.player.getDirection());
                while (ARCHITECT_LAYER_UP.consumeClick()) {
                    MatterBatteryPreviewState.cycleLayer(1);
                }
                while (ARCHITECT_LAYER_DOWN.consumeClick()) {
                    MatterBatteryPreviewState.cycleLayer(-1);
                }
                syncedFromTool = true;
            }
            if (!syncedFromTool && MatterBatteryPreviewState.isToolDriven()) {
                MatterBatteryPreviewState.clear();
            }
        }
        MatterBatteryFormationRenderer.tickParticles();
    }

    private static @org.jetbrains.annotations.Nullable HeldArchitect getHeldArchitect(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }
        if (minecraft.player.getMainHandItem().getItem() instanceof MatterArchitectItem) {
            return new HeldArchitect(net.minecraft.world.InteractionHand.MAIN_HAND, minecraft.player.getMainHandItem());
        }
        if (minecraft.player.getOffhandItem().getItem() instanceof MatterArchitectItem) {
            return new HeldArchitect(net.minecraft.world.InteractionHand.OFF_HAND, minecraft.player.getOffhandItem());
        }
        return null;
    }

    private static BlockPos getHoveredBlockPos(Minecraft minecraft) {
        if (minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }
        return null;
    }

    private static int normalizeScrollAmount(double delta) {
        if (delta == 0.0D) {
            return 0;
        }
        int rounded = (int) Math.round(delta);
        return rounded != 0 ? rounded : (delta > 0.0D ? 1 : -1);
    }

    private static boolean isControlDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static Direction getDominantLookDirection(Minecraft minecraft) {
        Vec3 view = minecraft.player.getViewVector(1.0F);
        double absX = Math.abs(view.x);
        double absY = Math.abs(view.y);
        double absZ = Math.abs(view.z);
        if (absY >= absX && absY >= absZ) {
            return view.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (absX >= absZ) {
            return view.x >= 0.0D ? Direction.EAST : Direction.WEST;
        }
        return view.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private record HeldArchitect(net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
    }
}

