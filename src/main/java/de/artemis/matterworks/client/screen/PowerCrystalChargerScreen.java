package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.PowerCrystalChargerBlockEntity;
import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PowerCrystalChargerScreen extends AbstractRenamableContainerScreen<PowerCrystalChargerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/charger.png");
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 18;
    private static final int GRAPH_WIDTH = 160;
    private static final int GRAPH_HEIGHT = 52;
    private static final int PROGRESS_BAR_X = 29;
    private static final int PROGRESS_BAR_Y = 108;
    private static final int BAR_WIDTH = 118;
    private static final int BAR_HEIGHT = 6;
    private static final int ENERGY_BAR_X = 29;
    private static final int ENERGY_BAR_Y = 118;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int GRAPH_COLOR = 0xFFE23D2D;
    private static final int GRAPH_AREA_COLOR = 0x35E23D2D;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public PowerCrystalChargerScreen(PowerCrystalChargerMenu menu, Inventory playerInventory, Component title) {
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (sideConfig.isShowing()) {
            sideConfig.renderBackground(guiGraphics, leftPos, topPos);
            return;
        }
        renderChargeGraph(guiGraphics, mouseX, mouseY);
        GuiWidgets.fillHorizontalProgressGauge(
                guiGraphics,
                leftPos + PROGRESS_BAR_X,
                topPos + PROGRESS_BAR_Y,
                menu.getScaledProgress(BAR_WIDTH),
                BAR_HEIGHT
        );
        GuiWidgets.fillHorizontalEnergyGauge(
                guiGraphics,
                leftPos + ENERGY_BAR_X,
                topPos + ENERGY_BAR_Y,
                menu.getScaledEnergyAmount(BAR_WIDTH),
                BAR_HEIGHT
        );
        VanillaGuiHelper.drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (sideConfig.isShowing()) {
            return;
        }
        renderEditableTitle(guiGraphics, titleLabelX, titleLabelY, 0x404040);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sideConfig.syncSlotLayout(menu);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, getEditableTitleX(), getEditableTitleY(), getEditableTitleColor(), isEditingName() ? "" : getDisplayedTitleText(), inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderBarTooltips(guiGraphics, mouseX, mouseY);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    private void renderBarTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + PROGRESS_BAR_X, topPos + PROGRESS_BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
            return;
        }

        if (isWithin(mouseX, mouseY, leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()));
            lines.add(TooltipBarHelper.buildBar(
                    getEnergyRatio(),
                    TOOLTIP_BAR_WIDTH,
                    ChatFormatting.RED,
                    ChatFormatting.DARK_GRAY,
                    getFillPercent() + "%"
            ));
            guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }

        List<Component> hoverLines = getHoveredGraphLines(mouseX, mouseY);
        if (hoverLines != null) {
            guiGraphics.renderTooltip(font, hoverLines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderChargeGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int historyCapacity = menu.getHistoryCapacity();
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + GRAPH_X,
                topPos + GRAPH_Y,
                GRAPH_WIDTH,
                GRAPH_HEIGHT,
                menu.getHistorySize(),
                historyCapacity,
                visibleIndex -> getVisibleSample(visibleIndex).chargeRate(),
                menu.getCurrentChargeRate(),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + GRAPH_X, topPos + GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT, historyCapacity),
                GRAPH_COLOR,
                GRAPH_AREA_COLOR
        );
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private float getEnergyRatio() {
        int capacity = menu.getEnergyCapacity();
        return capacity <= 0 ? 0.0F : menu.getEnergyStored() / (float) capacity;
    }

    private int getFillPercent() {
        return Math.round(getEnergyRatio() * 100.0F);
    }

    private List<Component> getHoveredGraphLines(int mouseX, int mouseY) {
        int historyIndex = GuiWidgets.getHoveredHistoryIndex(
                mouseX,
                mouseY,
                leftPos + GRAPH_X,
                topPos + GRAPH_Y,
                GRAPH_WIDTH,
                GRAPH_HEIGHT,
                menu.getHistorySize(),
                menu.getHistoryCapacity()
        );
        if (historyIndex < 0) {
            return null;
        }
        int sampledChargeRate = menu.getHistorySample(historyIndex).chargeRate();
        return List.of(GuiWidgets.formatRateComponent(sampledChargeRate, "FE/t", false));
    }

    private PowerCrystalChargerBlockEntity.ChargeHistorySample getVisibleSample(int visibleIndex) {
        int historyIndex = GuiWidgets.getHistoryIndexForVisibleIndex(visibleIndex, menu.getHistorySize(), menu.getHistoryCapacity());
        return historyIndex < 0
                ? PowerCrystalChargerBlockEntity.ChargeHistorySample.EMPTY
                : menu.getHistorySample(historyIndex);
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
