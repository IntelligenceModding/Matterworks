package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatterNetworkMonitorScreen extends AbstractRenamableContainerScreen<MatterNetworkMonitorMenu> {
    private static final int PANEL_LEFT = 12;
    private static final int PANEL_TOP = 28;
    private static final int PANEL_GAP = 8;
    private static final int PANEL_HEIGHT = 66;
    private static final int PANEL_INNER_PADDING = 8;
    private static final int CHART_TOP_OFFSET = 20;
    private static final int CHART_BOTTOM_PADDING = 8;

    private static final GraphSpec[] GRAPH_SPECS = new GraphSpec[]{
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_ENERGY, "Energy Transfer", 0xFFE23D2D, "FE/t"),
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_ITEMS, "Item Transfer", 0xFFD8B55B, "i/t"),
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_FLUIDS, "Fluid Transfer", 0xFF3A93FF, "mB/t")
    };

    public MatterNetworkMonitorScreen(MatterNetworkMonitorMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 340;
        this.imageHeight = 250;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiHelper.drawScreenBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        for (int graphIndex = 0; graphIndex < GRAPH_SPECS.length; graphIndex++) {
            renderGraphPanel(guiGraphics, GRAPH_SPECS[graphIndex], graphIndex, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 12, 10, 0x404040);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        GraphHover hover = getHoveredGraph(mouseX, mouseY);
        if (hover != null) {
            guiGraphics.renderTooltip(font, hover.lines(), Optional.empty(), mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected int getEditableTitleX() {
        return 12;
    }

    @Override
    protected int getEditableTitleY() {
        return 10;
    }

    @Override
    protected int getEditableTitleWidth() {
        return 220;
    }

    private void renderGraphPanel(GuiGraphics guiGraphics, GraphSpec spec, int graphIndex, int mouseX, int mouseY) {
        int panelLeft = leftPos + PANEL_LEFT;
        int panelTop = topPos + PANEL_TOP + graphIndex * (PANEL_HEIGHT + PANEL_GAP);
        int panelWidth = imageWidth - PANEL_LEFT * 2;
        int panelBottom = panelTop + PANEL_HEIGHT;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, panelLeft, panelTop, panelWidth, PANEL_HEIGHT);
        guiGraphics.fill(panelLeft + 6, panelTop + 7, panelLeft + panelWidth - 6, panelTop + 8, spec.color());
        guiGraphics.drawString(font, Component.literal(spec.title()), panelLeft + 10, panelTop + 6, 0x202020, false);

        int chartLeft = panelLeft + PANEL_INNER_PADDING;
        int chartTop = panelTop + CHART_TOP_OFFSET;
        int chartWidth = panelWidth - PANEL_INNER_PADDING * 2;
        int chartHeight = PANEL_HEIGHT - CHART_TOP_OFFSET - CHART_BOTTOM_PADDING;
        int innerLeft = chartLeft + 2;
        int innerTop = chartTop + 2;
        int innerWidth = chartWidth - 4;
        int innerHeight = chartHeight - 4;
        int bottom = innerTop + innerHeight - 1;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, chartLeft, chartTop, chartWidth, chartHeight);
        guiGraphics.fill(innerLeft, innerTop, innerLeft + innerWidth, innerTop + innerHeight, 0xFF1F1F1F);
        renderGraphGrid(guiGraphics, innerLeft, innerTop, innerWidth, innerHeight);

        MatterNetworkMonitorBlockEntity blockEntity = menu.getBlockEntity();
        int historySize = blockEntity.getHistorySize();
        int historyCapacity = blockEntity.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return;
        }

        int maxValue = 1;
        for (int index = 0; index < historyCapacity; index++) {
            maxValue = Math.max(maxValue, getVisibleSample(blockEntity, spec.channel(), index).totalAmount());
        }

        for (int index = 0; index < historyCapacity - 1; index++) {
            MatterNetworkMonitorBlockEntity.HistorySample current = getVisibleSample(blockEntity, spec.channel(), index);
            MatterNetworkMonitorBlockEntity.HistorySample next = getVisibleSample(blockEntity, spec.channel(), index + 1);
            int x1 = getSampleX(index, historyCapacity, innerLeft, innerWidth);
            int y1 = getSampleY(current.totalAmount(), maxValue, innerTop, innerHeight);
            int x2 = getSampleX(index + 1, historyCapacity, innerLeft, innerWidth);
            int y2 = getSampleY(next.totalAmount(), maxValue, innerTop, innerHeight);
            drawAreaSegment(guiGraphics, x1, y1, x2, y2, bottom, withAlpha(spec.color(), 0x35));
            drawLineSegment(guiGraphics, x1, y1, x2, y2, spec.color());
        }

        if (historySize == 1) {
            int visibleIndex = historyCapacity - 1;
            MatterNetworkMonitorBlockEntity.HistorySample sample = getVisibleSample(blockEntity, spec.channel(), visibleIndex);
            int x = getSampleX(visibleIndex, historyCapacity, innerLeft, innerWidth);
            int y = getSampleY(sample.totalAmount(), maxValue, innerTop, innerHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, spec.color());
        } else if (historySize > 0) {
            MatterNetworkMonitorBlockEntity.HistorySample latest = blockEntity.getHistorySample(spec.channel(), historySize - 1);
            int latestX = getSampleX(historyCapacity - 1, historyCapacity, innerLeft, innerWidth);
            int latestY = getSampleY(latest.totalAmount(), maxValue, innerTop, innerHeight);
            guiGraphics.fill(latestX - 1, latestY - 1, latestX + 2, latestY + 2, spec.color());
        }

        int hoveredSampleIndex = getHoveredSampleIndex(mouseX, mouseY, innerLeft, innerTop, innerWidth, innerHeight, historyCapacity);
        if (hoveredSampleIndex >= 0) {
            MatterNetworkMonitorBlockEntity.HistorySample hoveredSample = getVisibleSample(blockEntity, spec.channel(), hoveredSampleIndex);
            int hoveredX = getSampleX(hoveredSampleIndex, historyCapacity, innerLeft, innerWidth);
            int hoveredY = getSampleY(hoveredSample.totalAmount(), maxValue, innerTop, innerHeight);
            guiGraphics.fill(hoveredX, innerTop, hoveredX + 1, panelBottom - CHART_BOTTOM_PADDING - 2, 0x66FFFFFF);
            guiGraphics.fill(hoveredX - 2, hoveredY - 2, hoveredX + 3, hoveredY + 3, 0xFFFFFFFF);
            guiGraphics.fill(hoveredX - 1, hoveredY - 1, hoveredX + 2, hoveredY + 2, spec.color());
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

    private GraphHover getHoveredGraph(int mouseX, int mouseY) {
        MatterNetworkMonitorBlockEntity blockEntity = menu.getBlockEntity();
        int historySize = blockEntity.getHistorySize();
        int historyCapacity = blockEntity.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return null;
        }

        for (int graphIndex = 0; graphIndex < GRAPH_SPECS.length; graphIndex++) {
            GraphSpec spec = GRAPH_SPECS[graphIndex];
            int panelLeft = leftPos + PANEL_LEFT;
            int panelTop = topPos + PANEL_TOP + graphIndex * (PANEL_HEIGHT + PANEL_GAP);
            int panelWidth = imageWidth - PANEL_LEFT * 2;
            int chartLeft = panelLeft + PANEL_INNER_PADDING + 2;
            int chartTop = panelTop + CHART_TOP_OFFSET + 2;
            int chartWidth = panelWidth - PANEL_INNER_PADDING * 2 - 4;
            int chartHeight = PANEL_HEIGHT - CHART_TOP_OFFSET - CHART_BOTTOM_PADDING - 4;

            int hoveredSampleIndex = getHoveredSampleIndex(mouseX, mouseY, chartLeft, chartTop, chartWidth, chartHeight, historyCapacity);
            if (hoveredSampleIndex < 0) {
                continue;
            }

            int historyIndex = getHistoryIndexForVisibleIndex(blockEntity, hoveredSampleIndex);
            if (historyIndex < 0) {
                return null;
            }
            MatterNetworkMonitorBlockEntity.HistorySample sample = blockEntity.getHistorySample(spec.channel(), historyIndex);
            int ticksAgo = (historySize - 1 - historyIndex) * 4;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(spec.title()));
            lines.add(Component.literal(formatAmount(sample.totalAmount(), spec.unit())));
            lines.add(Component.literal("Source: " + formatAmount(sample.sourceAmount(), spec.unit())));
            lines.add(Component.literal("Sink: " + formatAmount(sample.sinkAmount(), spec.unit())));
            lines.add(Component.literal("Transit: " + formatAmount(sample.transitAmount(), spec.unit())));
            lines.add(Component.literal("Active Nodes: " + sample.activeNodes()));
            lines.add(Component.literal(ticksAgo <= 0 ? "Now" : formatTicksAgo(ticksAgo)));
            return new GraphHover(lines);
        }

        return null;
    }

    private static MatterNetworkMonitorBlockEntity.HistorySample getVisibleSample(MatterNetworkMonitorBlockEntity blockEntity, int channel, int visibleIndex) {
        int historyIndex = getHistoryIndexForVisibleIndex(blockEntity, visibleIndex);
        return historyIndex < 0
                ? MatterNetworkMonitorBlockEntity.HistorySample.EMPTY
                : blockEntity.getHistorySample(channel, historyIndex);
    }

    private static int getHistoryIndexForVisibleIndex(MatterNetworkMonitorBlockEntity blockEntity, int visibleIndex) {
        int historySize = blockEntity.getHistorySize();
        int historyCapacity = blockEntity.getHistoryCapacity();
        int leadingEmptySlots = Math.max(0, historyCapacity - historySize);
        if (visibleIndex < leadingEmptySlots) {
            return -1;
        }
        int historyIndex = visibleIndex - leadingEmptySlots;
        return historyIndex >= 0 && historyIndex < historySize ? historyIndex : -1;
    }

    private static int getHoveredSampleIndex(int mouseX, int mouseY, int chartLeft, int chartTop, int chartWidth, int chartHeight, int historySize) {
        if (mouseX < chartLeft || mouseX >= chartLeft + chartWidth || mouseY < chartTop || mouseY >= chartTop + chartHeight) {
            return -1;
        }
        if (historySize <= 1) {
            return 0;
        }
        float relative = (mouseX - chartLeft) / (float) Math.max(1, chartWidth - 1);
        return Mth.clamp(Math.round(relative * (historySize - 1)), 0, historySize - 1);
    }

    private static int getSampleX(int sampleIndex, int historySize, int chartLeft, int chartWidth) {
        if (historySize <= 1) {
            return chartLeft + chartWidth - 1;
        }
        return chartLeft + Math.round(sampleIndex * (chartWidth - 1) / (float) (historySize - 1));
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

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static String formatAmount(int amount, String unit) {
        return GuiWidgets.formatRateText(amount, unit, false);
    }

    private static String formatTicksAgo(int ticksAgo) {
        if (ticksAgo % 20 == 0) {
            return (ticksAgo / 20) + "s ago";
        }
        return String.format("%.1fs ago", ticksAgo / 20.0F);
    }

    private record GraphSpec(int channel, String title, int color, String unit) {
    }

    private record GraphHover(List<Component> lines) {
    }
}
