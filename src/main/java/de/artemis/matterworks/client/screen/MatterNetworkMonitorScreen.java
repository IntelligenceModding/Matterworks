package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
        MatterNetworkMonitorBlockEntity blockEntity = menu.getBlockEntity();
        int historyCapacity = blockEntity.getHistoryCapacity();
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + spec.x(),
                topPos + spec.y(),
                spec.width(),
                spec.height(),
                blockEntity.getHistorySize(),
                historyCapacity,
                getNetworkAmount(blockEntity.getHistorySample(spec.channel(), blockEntity.getHistorySize() - 1)),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + spec.x(), topPos + spec.y(), spec.width(), spec.height(), historyCapacity),
                new GuiWidgets.GraphSeries(
                        visibleIndex -> getNetworkAmount(getVisibleSample(blockEntity, spec.channel(), visibleIndex)),
                        spec.color(),
                        withAlpha(spec.color(), 0x35)
                )
        );
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
        int historyIndex = GuiWidgets.getHistoryIndexForVisibleIndex(visibleIndex, blockEntity.getHistorySize(), blockEntity.getHistoryCapacity());
        return historyIndex < 0
                ? MatterNetworkMonitorBlockEntity.HistorySample.EMPTY
                : blockEntity.getHistorySample(channel, historyIndex);
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
