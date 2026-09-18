package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MatterBatteryCoreScreen extends AbstractRenamableContainerScreen<MatterBatteryCoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/energy_cell.png");
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 18;
    private static final int GRAPH_WIDTH = 160;
    private static final int GRAPH_HEIGHT = 41;
    private static final int CHARGE_BAR_X = 8;
    private static final int CHARGE_BAR_Y = 63;
    private static final int CHARGE_BAR_WIDTH = 160;
    private static final int CHARGE_BAR_HEIGHT = 41;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int GRAPH_COLOR = 0xFFE23D2D;
    private static final int GRAPH_AREA_COLOR = 0x35E23D2D;

    private final List<MatterBatteryCoreBlockEntity.PortOverview> portEntries = new ArrayList<>();

    public MatterBatteryCoreScreen(MatterBatteryCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        refreshPortEntries();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshPortEntries();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        if (isMainTabSelected()) {
            renderMainTabBackground(guiGraphics, mouseX, mouseY);
            VanillaGuiHelper.drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 8, 6, 0x404040);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        if (isMainTabSelected()) {
            renderMainTabLabels(guiGraphics);
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
        renderCapacitorSlotTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TopCategoryTabs.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, getTabs())) {
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
            return true;
        }
        return false;
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
        String rateText = GuiWidgets.formatRateText(getCurrentNetTransferRate(), "FE/t", true);
        return Math.max(40, imageWidth - 22 - font.width(rateText));
    }

    @Override
    protected int getEditableTitleRightEdge() {
        String rateText = GuiWidgets.formatRateText(getCurrentNetTransferRate(), "FE/t", true);
        return imageWidth - 8 - font.width(rateText) - 6;
    }

    private void renderMainTabBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int[] inputHistory = menu.getBlockEntity().getOrderedInputHistory();
        int[] outputHistory = menu.getBlockEntity().getOrderedOutputHistory();
        int historySize = Math.max(inputHistory.length, outputHistory.length);
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + GRAPH_X,
                topPos + GRAPH_Y,
                GRAPH_WIDTH,
                GRAPH_HEIGHT,
                historySize,
                MatterBatteryCoreBlockEntity.HISTORY_SIZE,
                visibleIndex -> getVisibleNetTransfer(visibleIndex, inputHistory, outputHistory),
                getCurrentNetTransferRate(),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + GRAPH_X, topPos + GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT, MatterBatteryCoreBlockEntity.HISTORY_SIZE),
                GRAPH_COLOR,
                GRAPH_AREA_COLOR
        );
        GuiWidgets.drawInsetVerticalEnergyBar(
                guiGraphics,
                leftPos + CHARGE_BAR_X,
                topPos + CHARGE_BAR_Y,
                CHARGE_BAR_WIDTH,
                CHARGE_BAR_HEIGHT,
                menu.getScaledEnergyAmount(CHARGE_BAR_HEIGHT - 4)
        );
    }

    private void renderMainTabLabels(GuiGraphics guiGraphics) {
        String rateText = GuiWidgets.formatRateText(getCurrentNetTransferRate(), "FE/t", true);
        guiGraphics.drawString(font, rateText, imageWidth - 8 - font.width(rateText), 6, 0x404040, false);
    }

    private void renderTabSpecificTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderMainTabTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderMainTabTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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

        List<Component> hoverLines = getHoveredGraphLines(mouseX, mouseY);
        if (hoverLines != null) {
            guiGraphics.renderTooltip(font, hoverLines, Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderCapacitorSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isMouseOverSlot(MatterBatteryCoreMenu.CAPACITOR_CELL_MENU_SLOT, mouseX, mouseY)) {
            return;
        }

        ItemStack capacitorCell = new ItemStack(ModBlocks.MATTER_CAPACITOR_CELL.get());
        guiGraphics.renderTooltip(font, List.of(
                capacitorCell.getHoverName(),
                Component.literal("Installed: " + menu.getCapacitorCellCount() + " / " + menu.getMaxCapacitorCellCount())
        ), Optional.empty(), mouseX, mouseY);
    }

    private List<Component> getHoveredGraphLines(int mouseX, int mouseY) {
        int[] inputHistory = menu.getBlockEntity().getOrderedInputHistory();
        int[] outputHistory = menu.getBlockEntity().getOrderedOutputHistory();
        int historyIndex = GuiWidgets.getHoveredHistoryIndex(
                mouseX,
                mouseY,
                leftPos + GRAPH_X,
                topPos + GRAPH_Y,
                GRAPH_WIDTH,
                GRAPH_HEIGHT,
                getHistorySize(inputHistory, outputHistory),
                MatterBatteryCoreBlockEntity.HISTORY_SIZE
        );
        if (historyIndex < 0) {
            return null;
        }
        return List.of(GuiWidgets.formatRateComponent(
                getOrderedHistoryValue(inputHistory, historyIndex) - getOrderedHistoryValue(outputHistory, historyIndex),
                "FE/t",
                true
        ));
    }

    private static int getOrderedHistoryValue(int[] values, int index) {
        return index >= 0 && index < values.length ? values[index] : 0;
    }

    private static int getHistorySize(int[] inputHistory, int[] outputHistory) {
        return Math.max(inputHistory.length, outputHistory.length);
    }

    private static int getVisibleHistoryValue(int[] values, int visibleIndex) {
        int historyIndex = GuiWidgets.getHistoryIndexForVisibleIndex(visibleIndex, values.length, MatterBatteryCoreBlockEntity.HISTORY_SIZE);
        return getOrderedHistoryValue(values, historyIndex);
    }

    private static int getVisibleNetTransfer(int visibleIndex, int[] inputHistory, int[] outputHistory) {
        return getVisibleHistoryValue(inputHistory, visibleIndex) - getVisibleHistoryValue(outputHistory, visibleIndex);
    }

    private int getCurrentNetTransferRate() {
        return menu.getBlockEntity().getLatestInputRate() - menu.getBlockEntity().getLatestOutputRate();
    }

    private float getEnergyRatio() {
        int capacity = menu.getEnergyCapacity();
        return capacity <= 0 ? 0.0F : menu.getEnergyStored() / (float) capacity;
    }

    private int getFillPercent() {
        return Math.round(getEnergyRatio() * 100.0F);
    }

    private List<TopCategoryTabs.Tab> getTabs() {
        List<TopCategoryTabs.Tab> tabs = new ArrayList<>();
        tabs.add(new TopCategoryTabs.Tab(
                new ItemStack(ModBlocks.MATTER_BATTERY_CORE.get()),
                Component.literal("Main"),
                isMainTabSelected(),
                () -> {
                }
        ));
        for (MatterBatteryCoreBlockEntity.PortOverview overview : portEntries) {
            BlockPos portPos = overview.pos();
            tabs.add(new TopCategoryTabs.Tab(
                    createPortIcon(overview),
                    Component.literal("Port"),
                    false,
                    () -> openPortNetwork(portPos)
            ));
        }
        return tabs;
    }

    private void refreshPortEntries() {
        portEntries.clear();
        portEntries.addAll(menu.getBlockEntity().getPortOverview());
        BatteryTopTabCache.remember(menu.getBlockPos(), portEntries);
    }

    private void openPortNetwork(BlockPos portPos) {
        PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(portPos, false));
    }

    private static ItemStack createPortIcon(MatterBatteryCoreBlockEntity.PortOverview overview) {
        return MultiblockPortBlockEntity.createColoredPortStack(DyeColor.byId(overview.colorId()));
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isMouseOverSlot(int slotIndex, int mouseX, int mouseY) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }
        var slot = menu.slots.get(slotIndex);
        return slot.isActive() && isWithin(mouseX, mouseY, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
    }

    private boolean isMainTabSelected() {
        return true;
    }
}
