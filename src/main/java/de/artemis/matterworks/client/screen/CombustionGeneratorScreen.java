package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.CombustionGeneratorBlockEntity;
import de.artemis.matterworks.common.menu.CombustionGeneratorMenu;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CombustionGeneratorScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<CombustionGeneratorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/combustion_generator.png");
    private static final ResourceLocation FUEL_FILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final int FUEL_GRAPH_X = 8;
    private static final int FUEL_GRAPH_Y = 18;
    private static final int FUEL_GRAPH_WIDTH = 77;
    private static final int FUEL_GRAPH_HEIGHT = 41;
    private static final int ENERGY_GRAPH_X = 91;
    private static final int ENERGY_GRAPH_Y = 18;
    private static final int ENERGY_GRAPH_WIDTH = 77;
    private static final int ENERGY_GRAPH_HEIGHT = 41;
    private static final int FUEL_BAR_X = 8;
    private static final int FUEL_BAR_Y = 63;
    private static final int FUEL_BAR_WIDTH = 77;
    private static final int FUEL_BAR_HEIGHT = 41;
    private static final int ENERGY_BAR_X = 91;
    private static final int ENERGY_BAR_Y = 63;
    private static final int ENERGY_BAR_WIDTH = 77;
    private static final int ENERGY_BAR_HEIGHT = 41;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int FUEL_GRAPH_COLOR = 0xFFF29B2E;
    private static final int FUEL_GRAPH_AREA_COLOR = 0x35F29B2E;
    private static final int FUEL_FILL_COLOR = 0xFFFFC13A;
    private static final int FUEL_FILL_TOP_COLOR = 0xFFFFF0A0;
    private static final int ENERGY_GRAPH_COLOR = 0xFFE23D2D;
    private static final int ENERGY_GRAPH_AREA_COLOR = 0x35E23D2D;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        sideConfig.init(menu);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (sideConfig.isShowing()) {
            return;
        }
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (sideConfig.isShowing()) {
            sideConfig.renderBackground(guiGraphics, leftPos, topPos);
            return;
        }

        renderFuelUsageGraph(guiGraphics, mouseX, mouseY);
        renderGenerationGraph(guiGraphics, mouseX, mouseY);
        GuiWidgets.drawInsetVerticalAnimatedFillBar(
                guiGraphics,
                leftPos + FUEL_BAR_X,
                topPos + FUEL_BAR_Y,
                FUEL_BAR_WIDTH,
                FUEL_BAR_HEIGHT,
                menu.getScaledFuelAmount(FUEL_BAR_HEIGHT - 4),
                FUEL_FILL_TEXTURE,
                FUEL_FILL_COLOR,
                FUEL_FILL_TOP_COLOR
        );
        GuiWidgets.drawInsetVerticalEnergyBar(
                guiGraphics,
                leftPos + ENERGY_BAR_X,
                topPos + ENERGY_BAR_Y,
                ENERGY_BAR_WIDTH,
                ENERGY_BAR_HEIGHT,
                menu.getScaledEnergyAmount(ENERGY_BAR_HEIGHT - 4)
        );
        VanillaGuiHelper.drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sideConfig.syncSlotLayout(menu);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, titleLabelX, titleLabelY, 0x404040, title.getString(), inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderDataTooltips(guiGraphics, mouseX, mouseY);
        }
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
        TopCategoryTabs.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TopCategoryTabs.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, buildTabs())) {
            return true;
        }
        if (sideConfig.mouseClicked(menu, mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sideConfig.mouseDragged(menu, mouseX, mouseY, button, dragX, dragY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sideConfig.mouseReleased(menu, mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    private void renderFuelUsageGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int historyCapacity = menu.getHistoryCapacity();
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + FUEL_GRAPH_X,
                topPos + FUEL_GRAPH_Y,
                FUEL_GRAPH_WIDTH,
                FUEL_GRAPH_HEIGHT,
                menu.getHistorySize(),
                historyCapacity,
                visibleIndex -> getVisibleSample(visibleIndex).fuelUsageMilliRate(),
                menu.getCurrentFuelUsageMilliRate(),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + FUEL_GRAPH_X, topPos + FUEL_GRAPH_Y, FUEL_GRAPH_WIDTH, FUEL_GRAPH_HEIGHT, historyCapacity),
                FUEL_GRAPH_COLOR,
                FUEL_GRAPH_AREA_COLOR
        );
    }

    private void renderGenerationGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int historyCapacity = menu.getHistoryCapacity();
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + ENERGY_GRAPH_X,
                topPos + ENERGY_GRAPH_Y,
                ENERGY_GRAPH_WIDTH,
                ENERGY_GRAPH_HEIGHT,
                menu.getHistorySize(),
                historyCapacity,
                visibleIndex -> getVisibleSample(visibleIndex).generationRate(),
                menu.getCurrentGenerationRate(),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + ENERGY_GRAPH_X, topPos + ENERGY_GRAPH_Y, ENERGY_GRAPH_WIDTH, ENERGY_GRAPH_HEIGHT, historyCapacity),
                ENERGY_GRAPH_COLOR,
                ENERGY_GRAPH_AREA_COLOR
        );
    }

    private void renderDataTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + FUEL_BAR_X, topPos + FUEL_BAR_Y, FUEL_BAR_WIDTH, FUEL_BAR_HEIGHT)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Fuel: " + formatFuelAmount(menu.getStoredFuelMilliBurn())));
            lines.add(TooltipBarHelper.buildBar(
                    getFuelRatio(),
                    TOOLTIP_BAR_WIDTH,
                    ChatFormatting.GOLD,
                    ChatFormatting.DARK_GRAY,
                    getFuelPercent() + "%"
            ));
            guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }

        if (isWithin(mouseX, mouseY, leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()));
            lines.add(TooltipBarHelper.buildBar(
                    getEnergyRatio(),
                    TOOLTIP_BAR_WIDTH,
                    ChatFormatting.RED,
                    ChatFormatting.DARK_GRAY,
                    getEnergyPercent() + "%"
            ));
            guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }

        List<Component> hoverLines = getHoveredGraphLines(mouseX, mouseY);
        if (hoverLines != null) {
            guiGraphics.renderTooltip(font, hoverLines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> getHoveredGraphLines(int mouseX, int mouseY) {
        int fuelHistoryIndex = GuiWidgets.getHoveredHistoryIndex(
                mouseX,
                mouseY,
                leftPos + FUEL_GRAPH_X,
                topPos + FUEL_GRAPH_Y,
                FUEL_GRAPH_WIDTH,
                FUEL_GRAPH_HEIGHT,
                menu.getHistorySize(),
                menu.getHistoryCapacity()
        );
        if (fuelHistoryIndex >= 0) {
            CombustionGeneratorBlockEntity.GeneratorHistorySample sample = menu.getHistorySample(fuelHistoryIndex);
            return List.of(GuiWidgets.formatDecimalRateComponent(sample.fuelUsageMilliRate() / 1000.0D, "fuel/t"));
        }

        int energyHistoryIndex = GuiWidgets.getHoveredHistoryIndex(
                mouseX,
                mouseY,
                leftPos + ENERGY_GRAPH_X,
                topPos + ENERGY_GRAPH_Y,
                ENERGY_GRAPH_WIDTH,
                ENERGY_GRAPH_HEIGHT,
                menu.getHistorySize(),
                menu.getHistoryCapacity()
        );
        if (energyHistoryIndex < 0) {
            return null;
        }

        CombustionGeneratorBlockEntity.GeneratorHistorySample sample = menu.getHistorySample(energyHistoryIndex);
        return List.of(GuiWidgets.formatRateComponent(sample.generationRate(), "FE/t", false));
    }

    private CombustionGeneratorBlockEntity.GeneratorHistorySample getVisibleSample(int visibleIndex) {
        int historyIndex = GuiWidgets.getHistoryIndexForVisibleIndex(visibleIndex, menu.getHistorySize(), menu.getHistoryCapacity());
        return historyIndex < 0
                ? CombustionGeneratorBlockEntity.GeneratorHistorySample.EMPTY
                : menu.getHistorySample(historyIndex);
    }

    private float getFuelRatio() {
        long capacity = menu.getFuelCapacityMilliBurn();
        return capacity <= 0L ? 0.0F : menu.getStoredFuelMilliBurn() / (float) capacity;
    }

    private int getFuelPercent() {
        return Math.round(getFuelRatio() * 100.0F);
    }

    private float getEnergyRatio() {
        int capacity = menu.getEnergyCapacity();
        return capacity <= 0 ? 0.0F : menu.getEnergyStored() / (float) capacity;
    }

    private int getEnergyPercent() {
        return Math.round(getEnergyRatio() * 100.0F);
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String formatFuelAmount(long milliBurn) {
        return milliBurn % 1000L == 0L
                ? String.format(java.util.Locale.ROOT, "%,d fuel", milliBurn / 1000L)
                : String.format(java.util.Locale.ROOT, "%,.2f fuel", milliBurn / 1000.0D);
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
