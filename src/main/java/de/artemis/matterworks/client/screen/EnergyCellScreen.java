package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.menu.EnergyCellMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnergyCellScreen extends AbstractRenamableContainerScreen<EnergyCellMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/energy_cell.png");
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 18;
    private static final int GRAPH_WIDTH = 160;
    private static final int GRAPH_HEIGHT = 41;
    private static final int CHARGE_BAR_X = 8;
    private static final int CHARGE_BAR_Y = 63;
    private static final int CHARGE_BAR_WIDTH = 160;
    private static final int CHARGE_BAR_HEIGHT = 41;
    private static final int HISTORY_SAMPLE_INTERVAL = 4;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int GRAPH_COLOR = 0xFFE23D2D;
    private static final int GRAPH_AREA_COLOR = 0x35E23D2D;
    private static final int CHARGE_FILL_COLOR = 0xFFE23D2D;
    private static final int CHARGE_FILL_TOP_COLOR = 0xFFF06A5E;

    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public EnergyCellScreen(EnergyCellMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        sideConfig.init(menu);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderTransferGraph(guiGraphics, mouseX, mouseY);
        renderChargeBar(guiGraphics);
        sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 8, 6, 0x404040);

        String rateText = menu.getCurrentTotalTransferRate() + " FE/t";
        guiGraphics.drawString(font, rateText, imageWidth - 8 - font.width(rateText), 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderDataTooltips(guiGraphics, mouseX, mouseY);
        }
        TopCategoryTabs.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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
    protected int getEditableTitleWidth() {
        return 110;
    }

    private void renderTransferGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int chartFrameLeft = leftPos + GRAPH_X;
        int chartFrameTop = topPos + GRAPH_Y;
        int chartFrameWidth = GRAPH_WIDTH;
        int chartFrameHeight = GRAPH_HEIGHT;
        int chartLeft = chartFrameLeft + 2;
        int chartTop = chartFrameTop + 2;
        int chartWidth = chartFrameWidth - 4;
        int chartHeight = chartFrameHeight - 4;
        int bottom = chartTop + chartHeight - 1;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, chartFrameLeft, chartFrameTop, chartFrameWidth, chartFrameHeight);
        guiGraphics.fill(chartLeft, chartTop, chartLeft + chartWidth, chartTop + chartHeight, 0xFF1F1F1F);
        renderGraphGrid(guiGraphics, chartLeft, chartTop, chartWidth, chartHeight);

        int historySize = menu.getHistorySize();
        int historyCapacity = menu.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return;
        }

        int maxValue = 1;
        for (int visibleIndex = 0; visibleIndex < historyCapacity; visibleIndex++) {
            maxValue = Math.max(maxValue, getVisibleSample(visibleIndex).totalRate());
        }
        maxValue = Math.max(maxValue, menu.getCurrentTotalTransferRate());

        for (int visibleIndex = 0; visibleIndex < historyCapacity - 1; visibleIndex++) {
            EnergyCellBlockEntity.TransferHistorySample current = getVisibleSample(visibleIndex);
            EnergyCellBlockEntity.TransferHistorySample next = getVisibleSample(visibleIndex + 1);
            int x1 = getSampleX(visibleIndex, historyCapacity, chartLeft, chartWidth);
            int y1 = getSampleY(current.totalRate(), maxValue, chartTop, chartHeight);
            int x2 = getSampleX(visibleIndex + 1, historyCapacity, chartLeft, chartWidth);
            int y2 = getSampleY(next.totalRate(), maxValue, chartTop, chartHeight);
            drawAreaSegment(guiGraphics, x1, y1, x2, y2, bottom, GRAPH_AREA_COLOR);
            drawLineSegment(guiGraphics, x1, y1, x2, y2, GRAPH_COLOR);
        }

        if (historySize == 1) {
            int x = getSampleX(historyCapacity - 1, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(menu.getHistorySample(0).totalRate(), maxValue, chartTop, chartHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, GRAPH_COLOR);
        } else if (historySize > 1) {
            int latestVisibleIndex = historyCapacity - 1;
            EnergyCellBlockEntity.TransferHistorySample latest = menu.getHistorySample(historySize - 1);
            int x = getSampleX(latestVisibleIndex, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(latest.totalRate(), maxValue, chartTop, chartHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, GRAPH_COLOR);
        }

        int hoveredVisibleIndex = getHoveredSampleIndex(mouseX, mouseY, chartLeft, chartTop, chartWidth, chartHeight, historyCapacity);
        if (hoveredVisibleIndex >= 0) {
            EnergyCellBlockEntity.TransferHistorySample sample = getVisibleSample(hoveredVisibleIndex);
            int x = getSampleX(hoveredVisibleIndex, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(sample.totalRate(), maxValue, chartTop, chartHeight);
            guiGraphics.fill(x, chartTop, x + 1, chartTop + chartHeight, 0x66FFFFFF);
            guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFFFFF);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, GRAPH_COLOR);
        }
    }

    private void renderChargeBar(GuiGraphics guiGraphics) {
        int barFrameLeft = leftPos + CHARGE_BAR_X;
        int barFrameTop = topPos + CHARGE_BAR_Y;
        int barFrameWidth = CHARGE_BAR_WIDTH;
        int barFrameHeight = CHARGE_BAR_HEIGHT;
        int barLeft = barFrameLeft + 2;
        int barTop = barFrameTop + 2;
        int barWidth = barFrameWidth - 4;
        int barHeight = barFrameHeight - 4;
        int filledHeight = menu.getScaledEnergyAmount(barHeight);
        int barBottom = barTop + barHeight;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, barFrameLeft, barFrameTop, barFrameWidth, barFrameHeight);
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, 0xFF1F1F1F);
        if (filledHeight > 0) {
            int fillTop = barBottom - filledHeight;
            guiGraphics.fill(barLeft, fillTop, barLeft + barWidth, barBottom, CHARGE_FILL_COLOR);
            guiGraphics.fill(barLeft, fillTop, barLeft + barWidth, Math.min(barBottom, fillTop + 2), CHARGE_FILL_TOP_COLOR);
        }
    }

    private void renderGraphGrid(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        for (int step = 1; step < 4; step++) {
            int y = top + Math.round(step * (height - 1) / 4.0F);
            guiGraphics.fill(left, y, right, y + 1, 0x22373737);
        }
        for (int step = 1; step < 4; step++) {
            int x = left + Math.round(step * (width - 1) / 4.0F);
            guiGraphics.fill(x, top, x + 1, bottom, 0x22373737);
        }
    }

    private void renderDataTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + CHARGE_BAR_X, topPos + CHARGE_BAR_Y, CHARGE_BAR_WIDTH, CHARGE_BAR_HEIGHT)) {
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

        GraphHover hover = getHoveredGraph(mouseX, mouseY);
        if (hover != null) {
            guiGraphics.renderTooltip(font, hover.lines(), Optional.empty(), mouseX, mouseY);
        }
    }

    private GraphHover getHoveredGraph(int mouseX, int mouseY) {
        int historySize = menu.getHistorySize();
        int historyCapacity = menu.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return null;
        }

        int chartLeft = leftPos + GRAPH_X + 1;
        int chartTop = topPos + GRAPH_Y + 1;
        int chartWidth = GRAPH_WIDTH - 2;
        int chartHeight = GRAPH_HEIGHT - 2;
        int hoveredVisibleIndex = getHoveredSampleIndex(mouseX, mouseY, chartLeft, chartTop, chartWidth, chartHeight, historyCapacity);
        if (hoveredVisibleIndex < 0) {
            return null;
        }

        int historyIndex = getHistoryIndexForVisibleIndex(hoveredVisibleIndex, historySize, historyCapacity);
        if (historyIndex < 0) {
            return null;
        }

        EnergyCellBlockEntity.TransferHistorySample sample = menu.getHistorySample(historyIndex);
        int ticksAgo = (historySize - 1 - historyIndex) * HISTORY_SAMPLE_INTERVAL;
        return new GraphHover(List.of(Component.literal(formatSignedRate(sample.netRate()))));
    }

    private EnergyCellBlockEntity.TransferHistorySample getVisibleSample(int visibleIndex) {
        int historyIndex = getHistoryIndexForVisibleIndex(visibleIndex, menu.getHistorySize(), menu.getHistoryCapacity());
        return historyIndex < 0
                ? EnergyCellBlockEntity.TransferHistorySample.EMPTY
                : menu.getHistorySample(historyIndex);
    }

    private static int getHistoryIndexForVisibleIndex(int visibleIndex, int historySize, int historyCapacity) {
        int leadingEmptySlots = Math.max(0, historyCapacity - historySize);
        if (visibleIndex < leadingEmptySlots) {
            return -1;
        }
        int historyIndex = visibleIndex - leadingEmptySlots;
        return historyIndex >= 0 && historyIndex < historySize ? historyIndex : -1;
    }

    private static int getHoveredSampleIndex(int mouseX, int mouseY, int chartLeft, int chartTop, int chartWidth, int chartHeight, int historyCapacity) {
        if (mouseX < chartLeft || mouseX >= chartLeft + chartWidth || mouseY < chartTop || mouseY >= chartTop + chartHeight) {
            return -1;
        }
        if (historyCapacity <= 1) {
            return 0;
        }
        float relative = (mouseX - chartLeft) / (float) Math.max(1, chartWidth - 1);
        return Mth.clamp(Math.round(relative * (historyCapacity - 1)), 0, historyCapacity - 1);
    }

    private static int getSampleX(int sampleIndex, int historyCapacity, int chartLeft, int chartWidth) {
        if (historyCapacity <= 1) {
            return chartLeft + chartWidth - 1;
        }
        return chartLeft + Math.round(sampleIndex * (chartWidth - 1) / (float) (historyCapacity - 1));
    }

    private static int getSampleY(int value, int maxValue, int chartTop, int chartHeight) {
        int bottom = chartTop + chartHeight - 1;
        return bottom - Math.round((value / (float) Math.max(1, maxValue)) * (chartHeight - 1));
    }

    private static void drawAreaSegment(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int bottom, int color) {
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX; x <= endX; x++) {
            float delta = endX == startX ? 0.0F : (x - startX) / (float) (endX - startX);
            int y = Math.round(Mth.lerp(delta, y1, y2));
            guiGraphics.fill(x, y, x + 1, bottom + 1, color);
        }
    }

    private static void drawLineSegment(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            float delta = step / (float) steps;
            int x = Math.round(Mth.lerp(delta, x1, x2));
            int y = Math.round(Mth.lerp(delta, y1, y2));
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String formatTicksAgo(int ticksAgo) {
        if (ticksAgo % 20 == 0) {
            return (ticksAgo / 20) + "s ago";
        }
        return String.format("%.1fs ago", ticksAgo / 20.0F);
    }

    private float getEnergyRatio() {
        int capacity = menu.getEnergyCapacity();
        return capacity <= 0 ? 0.0F : menu.getEnergyStored() / (float) capacity;
    }

    private int getFillPercent() {
        return Math.round(getEnergyRatio() * 100.0F);
    }

    private static String formatSignedRate(int rate) {
        return "(" + (rate > 0 ? "+" : "") + rate + "fe/t)";
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, () -> PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(menu.getBlockPos(), menu.isRemoteAccess())));
    }

    private record GraphHover(List<Component> lines) {
    }
}
