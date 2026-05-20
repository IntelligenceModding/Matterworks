package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.network.ConfigureMatterBatteryPortPayload;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MatterBatteryCoreScreen extends AbstractRenamableContainerScreen<MatterBatteryCoreMenu> {
    private static final int VISIBLE_PORT_ROWS = 4;
    private static final int PORT_LIST_LEFT = 12;
    private static final int PORT_LIST_TOP = 40;
    private static final int PORT_ROW_HEIGHT = 16;
    private static final int PORT_SCROLLBAR_X = 114;
    private static final int PORT_SCROLLBAR_Y = 40;
    private static final int PORT_SCROLLBAR_WIDTH = 8;
    private static final int PORT_SCROLLBAR_HEIGHT = 64;
    private static final int PORT_SPEED_STEP = 100_000;

    private final List<GuiWidgets.SelectablePanelButton> portRowButtons = new ArrayList<>();
    private final List<MatterBatteryCoreBlockEntity.PortOverview> portEntries = new ArrayList<>();
    private Button modeButton;
    private Button decreaseSpeedButton;
    private Button increaseSpeedButton;
    private Button minSpeedButton;
    private Button maxSpeedButton;
    private Button networkButton;
    private BatteryTab currentTab = BatteryTab.STATS;
    private BlockPos selectedPortPos;
    private float portScrollOffset;
    private boolean portScrolling;

    public MatterBatteryCoreScreen(MatterBatteryCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        portRowButtons.clear();
        for (int row = 0; row < VISIBLE_PORT_ROWS; row++) {
            final int rowIndex = row;
            portRowButtons.add(addRenderableWidget(GuiWidgets.leftAlignedSelectablePanelButton(
                    leftPos + PORT_LIST_LEFT,
                    topPos + PORT_LIST_TOP + row * PORT_ROW_HEIGHT,
                    98,
                    14,
                    Component.empty(),
                    button -> selectPortRow(rowIndex)
            )));
        }

        modeButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 132, topPos + 70, 100, 18, Component.literal("Mode"), button -> cycleSelectedPortMode()));
        decreaseSpeedButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 132, topPos + 92, 48, 18, Component.literal("-100k"), button -> adjustSelectedPortSpeed(-PORT_SPEED_STEP)));
        increaseSpeedButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 184, topPos + 92, 48, 18, Component.literal("+100k"), button -> adjustSelectedPortSpeed(PORT_SPEED_STEP)));
        minSpeedButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 132, topPos + 114, 48, 18, Component.literal("0"), button -> setSelectedPortSpeed(0)));
        maxSpeedButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 184, topPos + 114, 48, 18, Component.literal("Max"), button -> setSelectedPortSpeed(MatterBatteryCoreBlockEntity.BASE_TRANSFER_RATE)));
        networkButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 132, topPos + 40, 100, 18, Component.literal("Network"), button -> openSelectedPortNetwork()));

        refreshPortEntries();
        refreshPortWidgets();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshPortEntries();
        refreshPortWidgets();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiHelper.drawScreenBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, leftPos, topPos);

        if (currentTab == BatteryTab.STATS) {
            renderStatsTabBackground(guiGraphics);
        } else {
            renderPortsTabBackground(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 8, 8, 0x404040);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        if (currentTab == BatteryTab.STATS) {
            renderStatsTabLabels(guiGraphics);
        } else {
            renderPortsTabLabels(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, getTabs());
        renderTabSpecificTooltips(guiGraphics, mouseX, mouseY);
        TopCategoryTabs.renderTooltip(guiGraphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, getTabs());
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TopCategoryTabs.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, getTabs())) {
            refreshPortWidgets();
            return true;
        }
        if (currentTab == BatteryTab.PORTS && button == 0 && isOverPortScrollbar(mouseX, mouseY)) {
            portScrolling = true;
            setPortScrollOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (TopCategoryTabs.keyPressed(keyCode, getTabs())) {
            refreshPortWidgets();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (currentTab == BatteryTab.PORTS && portScrolling) {
            setPortScrollOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            portScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentTab == BatteryTab.PORTS && (isOverPortList(mouseX, mouseY) || isOverPortScrollbar(mouseX, mouseY))) {
            scrollPorts((int) -Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected int getEditableTitleX() {
        return 8;
    }

    @Override
    protected int getEditableTitleY() {
        return 8;
    }

    @Override
    protected int getEditableTitleWidth() {
        return 120;
    }

    private void renderStatsTabBackground(GuiGraphics guiGraphics) {
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 8, topPos + 20, 112, 82);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 128, topPos + 20, 112, 82);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 8, topPos + 106, 232, 8);
        drawHistoryGraph(guiGraphics, leftPos + 12, topPos + 44, 104, 52,
                menu.getBlockEntity().getOrderedEnergyHistory(), null,
                Math.max(1, menu.getEnergyCapacity()),
                0xFF5FD06E, 0);
        drawHistoryGraph(guiGraphics, leftPos + 132, topPos + 44, 104, 52,
                menu.getBlockEntity().getOrderedInputHistory(),
                menu.getBlockEntity().getOrderedOutputHistory(),
                Math.max(1, Math.max(menu.getTransferRate(), Math.max(menu.getBlockEntity().getLatestInputRate(), menu.getBlockEntity().getLatestOutputRate()))),
                0xFF4CB4FF, 0xFFE97A4D);
    }

    private void renderStatsTabLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.structure"), 12, 22, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable(menu.isFormed()
                ? "screen.matterworks.matter_battery.formed"
                : "screen.matterworks.matter_battery.not_formed"), 12, 32, menu.isFormed() ? 0x2F7A31 : 0x8A2F2F, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.cells", menu.getCellCount()), 12, 42, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.stored", menu.getEnergyStored()), 12, 104, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.capacity", menu.getEnergyCapacity()), 12, 114, 0x404040, false);

        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.graph_charge"), 132, 22, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.graph_rates"), 132, 32, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.input_rate", menu.getBlockEntity().getLatestInputRate()), 132, 104, 0x2F6C92, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.output_rate", menu.getBlockEntity().getLatestOutputRate()), 132, 114, 0x8D5A2A, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.transfer", menu.getTransferRate()), 12, 124, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.status_hint"), 12, 134, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.shell_hint"), 12, 144, 0x404040, false);
    }

    private void renderPortsTabBackground(GuiGraphics guiGraphics) {
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 8, topPos + 20, 114, 86);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + PORT_SCROLLBAR_X, topPos + PORT_SCROLLBAR_Y, PORT_SCROLLBAR_WIDTH, PORT_SCROLLBAR_HEIGHT);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 124, topPos + 20, 116, 122);
        guiGraphics.fill(leftPos + PORT_SCROLLBAR_X + 1, getPortScrollbarHandleTop(), leftPos + PORT_SCROLLBAR_X + PORT_SCROLLBAR_WIDTH - 1, getPortScrollbarHandleTop() + getPortScrollbarHandleHeight(), 0xFFC6C6C6);
    }

    private void renderPortsTabLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.port_tab"), 12, 22, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("Ports: " + portEntries.size()), 12, 32, 0x404040, false);

        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.port_details"), 128, 22, 0x404040, false);
        if (selected == null) {
            guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.port_none"), 128, 34, 0x606060, false);
            return;
        }

        guiGraphics.drawString(font, Component.literal(trimToWidth(selected.displayName(), 108)), 128, 62, 0x202020, false);
        guiGraphics.drawString(font, Component.literal("ID " + selected.networkId()), 128, 74, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("Face " + selected.outwardSide().getName().toUpperCase()), 128, 86, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("Mode " + selected.mode().getShortLabel()), 128, 98, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.input_rate", selected.inputRate()), 128, 144, 0x2F6C92, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.output_rate", selected.outputRate()), 128, 154, 0x8D5A2A, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.port_limit", selected.maxTransfer()), 128, 166, 0x404040, false);
    }

    private void renderTabSpecificTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (currentTab != BatteryTab.STATS) {
            return;
        }

        if (isWithin(mouseX, mouseY, leftPos + 12, topPos + 44, 104, 52)) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        } else if (isWithin(mouseX, mouseY, leftPos + 132, topPos + 44, 104, 52)) {
            guiGraphics.renderTooltip(font, Component.literal(
                    "In " + GuiWidgets.formatRateText(menu.getBlockEntity().getLatestInputRate(), "FE/t", false)
                            + " | Out " + GuiWidgets.formatRateText(menu.getBlockEntity().getLatestOutputRate(), "FE/t", false)
            ), mouseX, mouseY);
        }
    }

    private void drawHistoryGraph(GuiGraphics guiGraphics, int x, int y, int width, int height, int[] primary, int[] secondary, int maxValue, int primaryColor, int secondaryColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF1F1F1F);
        guiGraphics.fill(x, y + height / 2, x + width, y + height / 2 + 1, 0xFF3A3A3A);
        if (primary.length == 0 && (secondary == null || secondary.length == 0)) {
            return;
        }

        drawGraphSeries(guiGraphics, x, y, width, height, primary, maxValue, primaryColor);
        if (secondary != null) {
            drawGraphSeries(guiGraphics, x, y, width, height, secondary, maxValue, secondaryColor);
        }
    }

    private void drawGraphSeries(GuiGraphics guiGraphics, int x, int y, int width, int height, int[] values, int maxValue, int color) {
        if (values.length == 0 || maxValue <= 0) {
            return;
        }

        for (int column = 0; column < width; column++) {
            int sampleIndex = (int) Math.floor(column * (values.length / (double) width));
            sampleIndex = Math.max(0, Math.min(sampleIndex, values.length - 1));
            int barHeight = Mth.clamp((int) Math.round((values[sampleIndex] / (double) maxValue) * (height - 2)), 0, height - 2);
            if (barHeight > 0) {
                guiGraphics.fill(x + column, y + height - 1 - barHeight, x + column + 1, y + height - 1, color);
            }
        }
    }

    private List<TopCategoryTabs.Tab> getTabs() {
        return List.of(
                new TopCategoryTabs.Tab(new ItemStack(ModBlocks.MATTER_BATTERY_CORE.get()), Component.translatable("screen.matterworks.matter_battery.tab_stats"), currentTab == BatteryTab.STATS, () -> currentTab = BatteryTab.STATS),
                new TopCategoryTabs.Tab(new ItemStack(ModBlocks.MULTIBLOCK_PORT.get()), Component.translatable("screen.matterworks.matter_battery.tab_ports"), currentTab == BatteryTab.PORTS, () -> currentTab = BatteryTab.PORTS)
        );
    }

    private void refreshPortEntries() {
        portEntries.clear();
        portEntries.addAll(menu.getBlockEntity().getPortOverview());
        if (selectedPortPos == null || getSelectedPort() == null) {
            selectedPortPos = portEntries.isEmpty() ? null : portEntries.get(0).pos();
        }
        ensureSelectedPortVisible();
    }

    private void refreshPortWidgets() {
        boolean portsTab = currentTab == BatteryTab.PORTS;
        int firstVisible = getFirstVisiblePortIndex();
        for (int row = 0; row < portRowButtons.size(); row++) {
            GuiWidgets.SelectablePanelButton button = portRowButtons.get(row);
            int absoluteIndex = firstVisible + row;
            if (portsTab && absoluteIndex < portEntries.size()) {
                MatterBatteryCoreBlockEntity.PortOverview overview = portEntries.get(absoluteIndex);
                button.visible = true;
                button.active = true;
                button.setSelected(overview.pos().equals(selectedPortPos));
                button.setMessage(Component.literal(trimToWidth(overview.displayName(), 84)));
            } else {
                button.visible = false;
                button.active = false;
                button.setSelected(false);
                button.setMessage(Component.empty());
            }
        }

        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        boolean enableConfig = portsTab && selected != null;
        modeButton.visible = portsTab;
        decreaseSpeedButton.visible = portsTab;
        increaseSpeedButton.visible = portsTab;
        minSpeedButton.visible = portsTab;
        maxSpeedButton.visible = portsTab;
        networkButton.visible = portsTab;
        modeButton.active = enableConfig;
        decreaseSpeedButton.active = enableConfig;
        increaseSpeedButton.active = enableConfig;
        minSpeedButton.active = enableConfig;
        maxSpeedButton.active = enableConfig;
        networkButton.active = enableConfig;
        if (selected != null) {
            modeButton.setMessage(Component.literal("Mode: " + selected.mode().getShortLabel()));
        } else {
            modeButton.setMessage(Component.literal("Mode"));
        }
    }

    private void selectPortRow(int rowIndex) {
        int absoluteIndex = getFirstVisiblePortIndex() + rowIndex;
        if (absoluteIndex >= 0 && absoluteIndex < portEntries.size()) {
            selectedPortPos = portEntries.get(absoluteIndex).pos();
            refreshPortWidgets();
        }
    }

    private MatterBatteryCoreBlockEntity.PortOverview getSelectedPort() {
        if (selectedPortPos == null) {
            return null;
        }
        for (MatterBatteryCoreBlockEntity.PortOverview overview : portEntries) {
            if (overview.pos().equals(selectedPortPos)) {
                return overview;
            }
        }
        return null;
    }

    private void cycleSelectedPortMode() {
        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        if (selected == null) {
            return;
        }
        sendPortConfiguration(selected.pos(), nextMode(selected.mode()), selected.maxTransfer());
    }

    private void adjustSelectedPortSpeed(int delta) {
        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        if (selected == null) {
            return;
        }
        setSelectedPortSpeed(selected.maxTransfer() + delta);
    }

    private void setSelectedPortSpeed(int newSpeed) {
        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        if (selected == null) {
            return;
        }
        sendPortConfiguration(selected.pos(), selected.mode(), Mth.clamp(newSpeed, 0, MatterBatteryCoreBlockEntity.BASE_TRANSFER_RATE));
    }

    private void sendPortConfiguration(BlockPos portPos, SideAccessMode mode, int maxTransfer) {
        PacketDistributor.sendToServer(new ConfigureMatterBatteryPortPayload(menu.getBlockPos(), portPos, mode.ordinal(), maxTransfer));
    }

    private void openSelectedPortNetwork() {
        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(selected.pos(), false));
    }

    private SideAccessMode nextMode(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> SideAccessMode.INPUT;
            case INPUT -> SideAccessMode.OUTPUT;
            case OUTPUT -> SideAccessMode.BOTH;
            case BOTH -> SideAccessMode.DISABLED;
        };
    }

    private int getFirstVisiblePortIndex() {
        int maxIndex = getMaxPortScrollIndex();
        return maxIndex <= 0 ? 0 : Math.round(portScrollOffset * maxIndex);
    }

    private int getMaxPortScrollIndex() {
        return Math.max(0, portEntries.size() - VISIBLE_PORT_ROWS);
    }

    private void scrollPorts(int rows) {
        int maxIndex = getMaxPortScrollIndex();
        if (rows == 0 || maxIndex <= 0) {
            return;
        }
        int newIndex = Mth.clamp(getFirstVisiblePortIndex() + rows, 0, maxIndex);
        portScrollOffset = newIndex / (float) maxIndex;
        refreshPortWidgets();
    }

    private void setPortScrollOffsetFromMouse(double mouseY) {
        int handleHeight = getPortScrollbarHandleHeight();
        float trackHeight = PORT_SCROLLBAR_HEIGHT - handleHeight;
        if (trackHeight <= 0.0F) {
            portScrollOffset = 0.0F;
        } else {
            float relative = (float) ((mouseY - (topPos + PORT_SCROLLBAR_Y)) - handleHeight / 2.0D);
            portScrollOffset = Mth.clamp(relative / trackHeight, 0.0F, 1.0F);
        }
        refreshPortWidgets();
    }

    private int getPortScrollbarHandleTop() {
        return topPos + PORT_SCROLLBAR_Y + (int) ((PORT_SCROLLBAR_HEIGHT - getPortScrollbarHandleHeight()) * portScrollOffset);
    }

    private int getPortScrollbarHandleHeight() {
        if (portEntries.isEmpty()) {
            return PORT_SCROLLBAR_HEIGHT - 2;
        }
        return Mth.clamp((int) ((VISIBLE_PORT_ROWS / (float) portEntries.size()) * PORT_SCROLLBAR_HEIGHT), 16, PORT_SCROLLBAR_HEIGHT - 2);
    }

    private boolean isOverPortScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + PORT_SCROLLBAR_X
                && mouseX < leftPos + PORT_SCROLLBAR_X + PORT_SCROLLBAR_WIDTH
                && mouseY >= topPos + PORT_SCROLLBAR_Y
                && mouseY < topPos + PORT_SCROLLBAR_Y + PORT_SCROLLBAR_HEIGHT;
    }

    private boolean isOverPortList(double mouseX, double mouseY) {
        return mouseX >= leftPos + PORT_LIST_LEFT
                && mouseX < leftPos + PORT_LIST_LEFT + 98
                && mouseY >= topPos + PORT_LIST_TOP
                && mouseY < topPos + PORT_LIST_TOP + VISIBLE_PORT_ROWS * PORT_ROW_HEIGHT;
    }

    private void ensureSelectedPortVisible() {
        MatterBatteryCoreBlockEntity.PortOverview selected = getSelectedPort();
        int maxScrollIndex = getMaxPortScrollIndex();
        if (selected == null || maxScrollIndex <= 0) {
            portScrollOffset = 0.0F;
            return;
        }
        int selectedIndex = portEntries.indexOf(selected);
        int firstVisible = getFirstVisiblePortIndex();
        if (selectedIndex < firstVisible) {
            portScrollOffset = selectedIndex / (float) maxScrollIndex;
        } else if (selectedIndex >= firstVisible + VISIBLE_PORT_ROWS) {
            portScrollOffset = (selectedIndex - VISIBLE_PORT_ROWS + 1) / (float) maxScrollIndex;
        }
        portScrollOffset = Mth.clamp(portScrollOffset, 0.0F, 1.0F);
    }

    private String trimToWidth(String text, int width) {
        String trimmed = font.plainSubstrByWidth(text, width);
        return trimmed.length() < text.length() ? trimmed + "..." : trimmed;
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private enum BatteryTab {
        STATS,
        PORTS
    }
}
