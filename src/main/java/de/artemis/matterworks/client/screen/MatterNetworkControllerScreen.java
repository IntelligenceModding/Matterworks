package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.network.MatterNetworkControllerActionPayload;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MatterNetworkControllerScreen extends AbstractRenamableContainerScreen<MatterNetworkControllerMenu> {
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_LEFT = 12;
    private static final int SEARCH_TOP = 34;
    private static final int LIST_TOP = 58;
    private static final int ROW_HEIGHT = 20;
    private static final int SCROLLBAR_X = 144;
    private static final int SCROLLBAR_Y = 58;
    private static final int SCROLLBAR_HEIGHT = 180;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int[] COLOR_SWATCH_XS = {216, 236, 256};
    private static final int[] COLOR_SWATCH_YS = {163, 163, 163};

    private final List<GuiWidgets.SelectablePanelButton> rowButtons = new ArrayList<>();
    private final List<MatterNetworkControllerBlockEntity.ControllerEntry> allEntries = new ArrayList<>();
    private final List<MatterNetworkControllerBlockEntity.ControllerEntry> displayEntries = new ArrayList<>();
    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(COLOR_SWATCH_XS, COLOR_SWATCH_YS);
    private EditBox searchBox;
    private Button filterButton;
    private Button sortButton;
    private Button openButton;
    private Button locateButton;
    private BlockPos selectedPos;
    private float scrollOffset;
    private boolean scrolling;
    private FilterMode filterMode = FilterMode.ALL;
    private SortMode sortMode = SortMode.NAME;

    public MatterNetworkControllerScreen(MatterNetworkControllerMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 336;
        this.imageHeight = 258;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = GuiWidgets.textField(font, leftPos + LIST_LEFT, topPos + SEARCH_TOP, 128, 18, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setResponder(ignored -> rebuildDisplayEntries());
        addRenderableWidget(searchBox);

        rowButtons.clear();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            rowButtons.add(addRenderableWidget(GuiWidgets.leftAlignedSelectablePanelButton(
                    leftPos + LIST_LEFT,
                    topPos + LIST_TOP + row * ROW_HEIGHT,
                    128,
                    18,
                    Component.empty(),
                    button -> selectVisibleRow(rowIndex)
            )));
        }

        filterButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 176, topPos + 34, 68, 20, filterMode.label, button -> cycleFilterMode()));
        sortButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 252, topPos + 34, 68, 20, sortMode.label, button -> cycleSortMode()));
        openButton = addRenderableWidget(GuiWidgets.panelButton(
                leftPos + 176,
                topPos + 212,
                144,
                18,
                Component.translatable("screen.matterworks.matter_network_controller.open_gui"),
                button -> triggerAction(MatterNetworkControllerActionPayload.ACTION_OPEN_GUI)
        ));
        locateButton = addRenderableWidget(GuiWidgets.panelButton(
                leftPos + 176,
                topPos + 234,
                144,
                18,
                Component.translatable("screen.matterworks.matter_network_controller.locate"),
                button -> triggerAction(MatterNetworkControllerActionPayload.ACTION_LOCATE)
        ));

        refreshEntries();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshEntries();
        if (sortMode == SortMode.DISTANCE) {
            rebuildDisplayEntries();
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiHelper.drawScreenBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 8, topPos + 30, 150, 220);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + 168, topPos + 30, 160, 220);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + SCROLLBAR_X, topPos + SCROLLBAR_Y, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
        int handleTop = getScrollbarHandleTop();
        VanillaGuiHelper.drawInsetPanel(guiGraphics, leftPos + SCROLLBAR_X + 1, handleTop, SCROLLBAR_WIDTH - 2, getScrollbarHandleHeight());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, 12, 10, 0x404040);

        int shownCount = displayEntries.size();
        int totalCount = allEntries.size();
        if (shownCount == totalCount) {
            guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.connected_nodes", totalCount), 12, 20, 0x404040, false);
        } else {
            guiGraphics.drawString(font, Component.literal("Showing " + shownCount + " / " + totalCount), 12, 20, 0x404040, false);
        }

        guiGraphics.drawString(font, Component.literal("Filter"), 176, 22, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("Sort"), 252, 22, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.details"), 176, 60, 0x404040, false);

        NetworkTotals totals = calculateTotals();
        guiGraphics.drawString(font, Component.literal("Nodes " + totals.totalNodes + " | Active " + totals.activeNodes), 176, 74, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("E " + totals.energyAmount + " fe/t | I " + totals.itemAmount + " i/t"), 176, 86, 0x406040, false);
        guiGraphics.drawString(font, Component.literal("F " + totals.fluidAmount + " mb/t | R " + totals.redstoneAmount + " rs"), 176, 98, 0x406090, false);

        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected == null) {
            String emptyText = allEntries.isEmpty() ? "No connected nodes" : "No matching nodes";
            guiGraphics.drawString(font, Component.literal(emptyText), 176, 118, 0x606060, false);
            return;
        }

        int y = 118;
        guiGraphics.drawString(font, trimToWidth(getPrimaryLabel(selected), 144), 176, y, 0x202020, false);
        y += 12;
        guiGraphics.drawString(font, Component.literal("Pos " + selected.pos().toShortString()), 176, y, 0x404040, false);
        y += 12;
        guiGraphics.drawString(font, Component.literal((selected.active() ? "Active" : "Idle") + " | Links " + selected.linkCount()), 176, y, selected.active() ? 0x507D50 : 0x505050, false);
        y += 12;
        guiGraphics.drawString(font, Component.literal("E " + formatSelectedChannel(selected.energyAmount(), selected.energyRole(), "fe/t")
                + " | I " + formatSelectedChannel(selected.itemAmount(), selected.itemRole(), "i/t")), 176, y, 0x404040, false);
        y += 12;
        guiGraphics.drawString(font, Component.literal("F " + formatSelectedChannel(selected.fluidAmount(), selected.fluidRole(), "mb/t")
                + " | R " + formatSelectedChannel(selected.redstoneAmount(), selected.redstoneRole(), "rs")), 176, y, 0x404040, false);
        y += 16;
        guiGraphics.drawString(font, Component.literal("Code"), 176, y, 0x404040, false);
        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, index -> getSelectedEntryColor(index, selected));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return keyCode != GLFW.GLFW_KEY_ENTER
                    && keyCode != GLFW.GLFW_KEY_KP_ENTER
                    && keyCode != GLFW.GLFW_KEY_ESCAPE;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void triggerAction(int action) {
        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new MatterNetworkControllerActionPayload(menu.getBlockPos(), selected.pos(), action));
        if (action == MatterNetworkControllerActionPayload.ACTION_LOCATE) {
            onClose();
        }
    }

    private void selectVisibleRow(int rowIndex) {
        int absoluteIndex = getFirstVisibleIndex() + rowIndex;
        if (absoluteIndex >= 0 && absoluteIndex < displayEntries.size()) {
            selectedPos = displayEntries.get(absoluteIndex).pos();
            refreshButtons();
        }
    }

    private void refreshEntries() {
        List<MatterNetworkControllerBlockEntity.ControllerEntry> latest = menu.getBlockEntity().getOverviewEntries();
        if (allEntries.equals(latest)) {
            refreshButtons();
            return;
        }

        allEntries.clear();
        allEntries.addAll(latest);
        rebuildDisplayEntries();
    }

    private void rebuildDisplayEntries() {
        String query = searchBox == null ? "" : searchBox.getValue().strip().toLowerCase(Locale.ROOT);

        displayEntries.clear();
        for (MatterNetworkControllerBlockEntity.ControllerEntry entry : allEntries) {
            if (!filterMode.matches(entry)) {
                continue;
            }
            if (!query.isEmpty() && !matchesSearch(entry, query)) {
                continue;
            }
            displayEntries.add(entry);
        }

        displayEntries.sort(sortMode.comparator(this));

        if (selectedPos == null || findEntry(displayEntries, selectedPos) == null) {
            selectedPos = displayEntries.isEmpty() ? null : displayEntries.get(0).pos();
        }

        ensureSelectionVisible();
        refreshButtons();
    }

    private void refreshButtons() {
        int firstVisibleIndex = getFirstVisibleIndex();
        for (int row = 0; row < rowButtons.size(); row++) {
            GuiWidgets.SelectablePanelButton button = rowButtons.get(row);
            int absoluteIndex = firstVisibleIndex + row;
            if (absoluteIndex < displayEntries.size()) {
                MatterNetworkControllerBlockEntity.ControllerEntry entry = displayEntries.get(absoluteIndex);
                button.setMessage(Component.literal(trimToWidth(getPrimaryLabel(entry) + " [" + entry.pos().toShortString() + "]", 116)));
                button.visible = true;
                button.active = true;
                button.setSelected(entry.pos().equals(selectedPos));
            } else {
                button.setMessage(Component.empty());
                button.visible = false;
                button.active = false;
                button.setSelected(false);
            }
        }

        filterButton.setMessage(filterMode.label);
        sortButton.setMessage(sortMode.label);

        boolean hasSelection = getSelectedEntry() != null;
        openButton.active = hasSelection;
        locateButton.active = hasSelection;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected != null && colorPicker.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight,
                (index, color) -> PacketDistributor.sendToServer(new SetPylonColorCodePayload(selected.pos(), index, color.getId())),
                index -> getSelectedEntryColor(index, selected))) {
            return true;
        }
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            scrolling = true;
            setScrollOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected != null) {
            colorPicker.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY,
                    index -> getSelectedEntryColor(index, selected));
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            setScrollOffsetFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverList(mouseX, mouseY) || isOverScrollbar(mouseX, mouseY)) {
            scrollByRows((int) -Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        return 146;
    }

    private void cycleFilterMode() {
        filterMode = filterMode.next();
        rebuildDisplayEntries();
    }

    private void cycleSortMode() {
        sortMode = sortMode.next();
        rebuildDisplayEntries();
    }

    private MatterNetworkControllerBlockEntity.ControllerEntry getSelectedEntry() {
        return selectedPos == null ? null : findEntry(displayEntries, selectedPos);
    }

    private MatterNetworkControllerBlockEntity.ControllerEntry findEntry(List<MatterNetworkControllerBlockEntity.ControllerEntry> entries, BlockPos pos) {
        for (MatterNetworkControllerBlockEntity.ControllerEntry entry : entries) {
            if (entry.pos().equals(pos)) {
                return entry;
            }
        }
        return null;
    }

    private NetworkTotals calculateTotals() {
        int activeNodes = 0;
        int energyAmount = 0;
        int itemAmount = 0;
        int fluidAmount = 0;
        int redstoneAmount = 0;
        for (MatterNetworkControllerBlockEntity.ControllerEntry entry : allEntries) {
            if (entry.active()) {
                activeNodes++;
            }
            energyAmount += entry.energyAmount();
            itemAmount += entry.itemAmount();
            fluidAmount += entry.fluidAmount();
            redstoneAmount += entry.redstoneAmount();
        }
        return new NetworkTotals(allEntries.size(), activeNodes, energyAmount, itemAmount, fluidAmount, redstoneAmount);
    }

    private boolean matchesSearch(MatterNetworkControllerBlockEntity.ControllerEntry entry, String query) {
        return entry.displayName().toLowerCase(Locale.ROOT).contains(query)
                || entry.pos().toShortString().toLowerCase(Locale.ROOT).contains(query);
    }

    private String getPrimaryLabel(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return entry.displayName();
    }

    private String formatSelectedChannel(int amount, int roleOrdinal, String suffix) {
        if (amount <= 0) {
            return "--";
        }
        String rolePrefix = switch (roleOrdinal) {
            case 1 -> "+";
            case 2 -> "-";
            default -> "";
        };
        return rolePrefix + amount + " " + suffix;
    }

    private String trimToWidth(String text, int width) {
        String trimmed = font.plainSubstrByWidth(text, width);
        return trimmed.length() < text.length() ? trimmed + "..." : trimmed;
    }

    private int getFirstVisibleIndex() {
        int maxScrollIndex = getMaxScrollIndex();
        return maxScrollIndex <= 0 ? 0 : Math.round(scrollOffset * maxScrollIndex);
    }

    private int getMaxScrollIndex() {
        return Math.max(0, displayEntries.size() - VISIBLE_ROWS);
    }

    private void scrollByRows(int rows) {
        int maxScrollIndex = getMaxScrollIndex();
        if (maxScrollIndex <= 0 || rows == 0) {
            return;
        }
        int newIndex = Mth.clamp(getFirstVisibleIndex() + rows, 0, maxScrollIndex);
        scrollOffset = newIndex / (float) maxScrollIndex;
        refreshButtons();
    }

    private void setScrollOffsetFromMouse(double mouseY) {
        int handleHeight = getScrollbarHandleHeight();
        float trackHeight = SCROLLBAR_HEIGHT - handleHeight;
        if (trackHeight <= 0.0F) {
            scrollOffset = 0.0F;
        } else {
            float relative = (float) ((mouseY - (topPos + SCROLLBAR_Y)) - handleHeight / 2.0D);
            scrollOffset = Mth.clamp(relative / trackHeight, 0.0F, 1.0F);
        }
        refreshButtons();
    }

    private int getScrollbarHandleTop() {
        return topPos + SCROLLBAR_Y + (int) ((SCROLLBAR_HEIGHT - getScrollbarHandleHeight()) * scrollOffset);
    }

    private int getScrollbarHandleHeight() {
        if (displayEntries.isEmpty()) {
            return SCROLLBAR_HEIGHT - 2;
        }
        return Mth.clamp((int) ((VISIBLE_ROWS / (float) displayEntries.size()) * SCROLLBAR_HEIGHT), 18, SCROLLBAR_HEIGHT - 2);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLLBAR_X
                && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + SCROLLBAR_Y
                && mouseY < topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return mouseX >= leftPos + LIST_LEFT
                && mouseX < leftPos + LIST_LEFT + 128
                && mouseY >= topPos + LIST_TOP
                && mouseY < topPos + LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private void ensureSelectionVisible() {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) {
            scrollOffset = 0.0F;
            return;
        }
        int selectedIndex = displayEntries.indexOf(selectedEntry);
        int maxScrollIndex = getMaxScrollIndex();
        if (maxScrollIndex <= 0) {
            scrollOffset = 0.0F;
            return;
        }
        int firstVisibleIndex = getFirstVisibleIndex();
        if (selectedIndex < firstVisibleIndex) {
            scrollOffset = selectedIndex / (float) maxScrollIndex;
        } else if (selectedIndex >= firstVisibleIndex + VISIBLE_ROWS) {
            scrollOffset = (selectedIndex - VISIBLE_ROWS + 1) / (float) maxScrollIndex;
        }
        scrollOffset = Mth.clamp(scrollOffset, 0.0F, 1.0F);
    }

    private DyeColor getSelectedEntryColor(int index, MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return switch (index) {
            case 0 -> DyeColor.byId(entry.colorOneId());
            case 1 -> DyeColor.byId(entry.colorTwoId());
            case 2 -> DyeColor.byId(entry.colorThreeId());
            default -> DyeColor.WHITE;
        };
    }

    private record NetworkTotals(int totalNodes, int activeNodes, int energyAmount, int itemAmount, int fluidAmount, int redstoneAmount) {
    }

    private enum FilterMode {
        ALL("All") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return true;
            }
        },
        ACTIVE("Active") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return entry.active();
            }
        };

        private final Component label;

        FilterMode(String label) {
            this.label = Component.literal(label);
        }

        abstract boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry);

        FilterMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum SortMode {
        NAME("Name") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.comparing(screen::getPrimaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.pos().asLong());
            }
        },
        ACTIVITY("Hot") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.<MatterNetworkControllerBlockEntity.ControllerEntry>comparingInt(screen::getActivityScore)
                        .reversed()
                        .thenComparing(screen::getPrimaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.pos().asLong());
            }
        },
        DISTANCE("Near") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.<MatterNetworkControllerBlockEntity.ControllerEntry>comparingDouble(screen::getDistanceToPlayer)
                        .thenComparing(screen::getPrimaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.pos().asLong());
            }
        };

        private final Component label;

        SortMode(String label) {
            this.label = Component.literal(label);
        }

        abstract Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen);

        SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private int getActivityScore(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return entry.energyAmount() + entry.itemAmount() + entry.fluidAmount() + entry.redstoneAmount();
    }

    private double getDistanceToPlayer(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.blockPosition().distSqr(entry.pos())
                : 0.0D;
    }
}
