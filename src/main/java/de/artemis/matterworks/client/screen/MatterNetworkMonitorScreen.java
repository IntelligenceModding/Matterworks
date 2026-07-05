package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatterNetworkMonitorScreen extends AbstractRenamableContainerScreen<MatterNetworkMonitorMenu> {
    private static final GraphSpec[] GRAPH_SPECS = new GraphSpec[]{
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_ENERGY, "Energy Transfer", 0xFFE23D2D, "FE/t", 8, 92, 160, 32),
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_ITEMS, "Item Transfer", 0xFFD8B55B, "i/t", 8, 55, 160, 32),
            new GraphSpec(MatterPylonBlockEntity.CHANNEL_FLUIDS, "Fluid Transfer", 0xFF3A93FF, "mB/t", 8, 18, 160, 32)
    };
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_network_monitor.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int GRAPH_GRID = 0x22373737;
    private static final int GRAPH_HOVER_LINE = 0x66FFFFFF;
    private static final int GRAPH_HOVER_POINT = 0xFFFFFFFF;
    private static final int GRAPH_ZERO_LINE = 0x44373737;

    public MatterNetworkMonitorScreen(MatterNetworkMonitorMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        for (GraphSpec spec : GRAPH_SPECS) {
            renderGraph(guiGraphics, spec, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 8, 6, 0x404040);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        List<Component> hoverLines = getHoveredGraphLines(mouseX, mouseY);
        if (hoverLines != null) {
            guiGraphics.renderTooltip(font, hoverLines, Optional.empty(), mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected int getEditableTitleX() {
        return 8;
    }

    @Override
    protected int getEditableTitleY() {
        return 6;
    }

    @Override
    protected int getEditableTitleWidth() {
        return 160;
    }

    private void renderGraph(GuiGraphics guiGraphics, GraphSpec spec, int mouseX, int mouseY) {
        int chartLeft = leftPos + spec.x();
        int chartTop = topPos + spec.y();
        int chartWidth = spec.width();
        int chartHeight = spec.height();
        int bottom = chartTop + chartHeight - 1;

        renderGraphGrid(guiGraphics, chartLeft, chartTop, chartWidth, chartHeight);
        MatterNetworkMonitorBlockEntity blockEntity = menu.getBlockEntity();
        int historySize = blockEntity.getHistorySize();
        int historyCapacity = blockEntity.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return;
        }

        int minValue = 0;
        int maxValue = 0;
        for (int index = 0; index < historyCapacity; index++) {
            int value = getNetworkAmount(getVisibleSample(blockEntity, spec.channel(), index));
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
        }
        if (minValue == 0 && maxValue == 0) {
            maxValue = 1;
        }
        int zeroY = getSampleY(0, minValue, maxValue, chartTop, chartHeight);
        if (minValue < 0 && maxValue > 0) {
            guiGraphics.fill(chartLeft, zeroY, chartLeft + chartWidth, zeroY + 1, GRAPH_ZERO_LINE);
        }

        for (int index = 0; index < historyCapacity - 1; index++) {
            MatterNetworkMonitorBlockEntity.HistorySample current = getVisibleSample(blockEntity, spec.channel(), index);
            MatterNetworkMonitorBlockEntity.HistorySample next = getVisibleSample(blockEntity, spec.channel(), index + 1);
            int x1 = getSampleX(index, historyCapacity, chartLeft, chartWidth);
            int y1 = getSampleY(getNetworkAmount(current), minValue, maxValue, chartTop, chartHeight);
            int x2 = getSampleX(index + 1, historyCapacity, chartLeft, chartWidth);
            int y2 = getSampleY(getNetworkAmount(next), minValue, maxValue, chartTop, chartHeight);
            drawAreaSegment(guiGraphics, x1, y1, x2, y2, zeroY, withAlpha(spec.color(), 0x35));
            drawLineSegment(guiGraphics, x1, y1, x2, y2, spec.color());
        }

        if (historySize == 1) {
            int visibleIndex = historyCapacity - 1;
            MatterNetworkMonitorBlockEntity.HistorySample sample = getVisibleSample(blockEntity, spec.channel(), visibleIndex);
            int x = getSampleX(visibleIndex, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(getNetworkAmount(sample), minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, spec.color());
        } else if (historySize > 0) {
            MatterNetworkMonitorBlockEntity.HistorySample latest = blockEntity.getHistorySample(spec.channel(), historySize - 1);
            int latestX = getSampleX(historyCapacity - 1, historyCapacity, chartLeft, chartWidth);
            int latestY = getSampleY(getNetworkAmount(latest), minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(latestX - 1, latestY - 1, latestX + 2, latestY + 2, spec.color());
        }

        int hoveredSampleIndex = getHoveredSampleIndex(mouseX, mouseY, chartLeft, chartTop, chartWidth, chartHeight, historyCapacity);
        if (hoveredSampleIndex >= 0) {
            MatterNetworkMonitorBlockEntity.HistorySample hoveredSample = getVisibleSample(blockEntity, spec.channel(), hoveredSampleIndex);
            int hoveredX = getSampleX(hoveredSampleIndex, historyCapacity, chartLeft, chartWidth);
            int hoveredY = getSampleY(getNetworkAmount(hoveredSample), minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(hoveredX, chartTop, hoveredX + 1, chartTop + chartHeight, GRAPH_HOVER_LINE);
            guiGraphics.fill(hoveredX - 2, hoveredY - 2, hoveredX + 3, hoveredY + 3, GRAPH_HOVER_POINT);
            guiGraphics.fill(hoveredX - 1, hoveredY - 1, hoveredX + 2, hoveredY + 2, spec.color());
        }
    }

    private void renderGraphGrid(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        for (int step = 1; step < 4; step++) {
            int y = top + Math.round(step * (height - 1) / 4.0F);
            guiGraphics.fill(left, y, right, y + 1, GRAPH_GRID);
        }
        for (int step = 1; step < 4; step++) {
            int x = left + Math.round(step * (width - 1) / 4.0F);
            guiGraphics.fill(x, top, x + 1, bottom, GRAPH_GRID);
        }
    }

    private List<Component> getHoveredGraphLines(int mouseX, int mouseY) {
        MatterNetworkMonitorBlockEntity blockEntity = menu.getBlockEntity();
        int historySize = blockEntity.getHistorySize();
        int historyCapacity = blockEntity.getHistoryCapacity();
        if (historyCapacity <= 0) {
            return null;
        }

        for (int graphIndex = 0; graphIndex < GRAPH_SPECS.length; graphIndex++) {
            GraphSpec spec = GRAPH_SPECS[graphIndex];
            int historyIndex = GuiWidgets.getHoveredHistoryIndex(
                    mouseX,
                    mouseY,
                    leftPos + spec.x(),
                    topPos + spec.y(),
                    spec.width(),
                    spec.height(),
                    historySize,
                    historyCapacity
            );
            if (historyIndex < 0) {
                continue;
            }
            MatterNetworkMonitorBlockEntity.HistorySample sample = blockEntity.getHistorySample(spec.channel(), historyIndex);
            int ticksAgo = (historySize - 1 - historyIndex) * 4;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(spec.title()));
            lines.add(Component.literal("Throughput: " + formatAmount(getNetworkAmount(sample), spec.unit())));
            lines.add(Component.literal("Export: " + formatAmount(sample.sourceAmount(), spec.unit())));
            lines.add(Component.literal("Import: " + formatAmount(sample.sinkAmount(), spec.unit())));
            lines.add(Component.literal("Transit: " + formatAmount(sample.transitAmount(), spec.unit())));
            lines.add(Component.literal("Active Nodes: " + sample.activeNodes()));
            lines.add(Component.literal(ticksAgo <= 0 ? "Now" : formatTicksAgo(ticksAgo)));
            return lines;
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

    private static int getSampleY(int value, int minValue, int maxValue, int chartTop, int chartHeight) {
        int bottom = chartTop + chartHeight - 1;
        int range = maxValue - minValue;
        if (range <= 0) {
            return bottom;
        }
        float normalized = (value - minValue) / (float) range;
        return bottom - Math.round(normalized * (chartHeight - 1));
    }

    private static void drawAreaSegment(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int baselineY, int color) {
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX; x <= endX; x++) {
            float delta = endX == startX ? 0.0F : (x - startX) / (float) (endX - startX);
            int y = Math.round(Mth.lerp(delta, y1, y2));
            int fillTop = Math.min(y, baselineY);
            int fillBottom = Math.max(y, baselineY) + 1;
            guiGraphics.fill(x, fillTop, x + 1, fillBottom, color);
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
        return GuiWidgets.formatRateText(amount, unit, true);
    }

    private static int getNetworkAmount(MatterNetworkMonitorBlockEntity.HistorySample sample) {
        return Math.max(sample.sourceAmount(), sample.sinkAmount());
    }

    private static String formatTicksAgo(int ticksAgo) {
        if (ticksAgo % 20 == 0) {
            return (ticksAgo / 20) + "s ago";
        }
        return String.format("%.1fs ago", ticksAgo / 20.0F);
    }

    private record GraphSpec(int channel, String title, int color, String unit, int x, int y, int width, int height) {
    }
}
