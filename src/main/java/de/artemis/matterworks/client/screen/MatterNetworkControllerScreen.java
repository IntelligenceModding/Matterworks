package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.network.CycleMatterNetworkControllerNodeModePayload;
import de.artemis.matterworks.common.network.MatterNetworkControllerActionPayload;
import de.artemis.matterworks.common.network.SetMatterNetworkControllerNodeIdPayload;
import de.artemis.matterworks.common.network.SetMatterNetworkControllerSelectedNodePayload;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import de.artemis.matterworks.common.transport.PylonMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MatterNetworkControllerScreen extends AbstractContainerScreen<MatterNetworkControllerMenu> {
    private static final int ID_SUBMIT_DEBOUNCE_TICKS = 8;
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_LEFT = 8;
    private static final int LIST_WIDTH = 126;
    private static final int SEARCH_TOP = 39;
    private static final int SEARCH_WIDTH = 145;
    private static final int SEARCH_HEIGHT = 12;
    private static final int LIST_TOP = 54;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_CLIP_INSET = 1;
    private static final int DETAIL_CHANNEL_BUTTONS_X = 172;
    private static final int DETAIL_CHANNEL_BUTTONS_Y = 38;
    private static final int DETAIL_CHANNEL_BUTTONS_WIDTH = 145;
    private static final int DETAIL_CHANNEL_BUTTONS_HEIGHT = 12;
    private static final int DETAIL_CHANNEL_BUTTON_GAP = 2;
    private static final int DETAIL_FILTER_SECTION_X = 195;
    private static final int DETAIL_FILTER_SECTION_Y = 75;
    private static final int DETAIL_FILTER_SECTION_WIDTH = 38;
    private static final int DETAIL_FILTER_SECTION_HEIGHT = 37;
    private static final int DETAIL_ID_FIELD_X = 260;
    private static final int DETAIL_ID_FIELD_Y = 54;
    private static final int DETAIL_ID_FIELD_WIDTH = 55;
    private static final int DETAIL_ID_FIELD_HEIGHT = 15;
    private static final int DETAIL_MODE_BUTTON_X = 172;
    private static final int DETAIL_MODE_BUTTON_Y = 53;
    private static final int DETAIL_MODE_BUTTON_WIDTH = 83;
    private static final int DETAIL_MODE_BUTTON_HEIGHT = 17;
    private static final int[] DETAIL_COLOR_SLOT_XS = {260, 280, 300};
    private static final int DETAIL_COLOR_SLOT_Y = 75;
    private static final int DETAIL_INFO_X = 173;
    private static final int DETAIL_INFO_Y = 117;
    private static final int DETAIL_INFO_WIDTH = 126;
    private static final int DETAIL_INFO_HEIGHT = 97;
    private static final int DETAIL_INFO_LINE_HEIGHT = 12;
    private static final int DETAIL_SCROLLBAR_X = 304;
    private static final int DETAIL_SCROLLBAR_Y = 117;
    private static final int DETAIL_SCROLLBAR_WIDTH = 12;
    private static final int DETAIL_SCROLLBAR_HEIGHT = 97;
    private static final int LOCATE_BUTTON_X = 246;
    private static final int LOCATE_BUTTON_Y = 17;
    private static final int OPEN_GUI_BUTTON_X = 172;
    private static final int OPEN_GUI_BUTTON_Y = 17;
    private static final int SCROLLBAR_X = 139;
    private static final int SCROLLBAR_Y = 54;
    private static final int SCROLLBAR_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int SCROLLBAR_HANDLE_HEIGHT = 15;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_network_controller.png");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final int TEXTURE_SIZE = 512;
    private static final int GUI_BACKGROUND_FILL = 0xFFC6C6C6;

    private final List<GuiWidgets.SelectablePanelButton> rowButtons = new ArrayList<>();
    private final List<ChannelButtonEntry> detailChannelButtons = new ArrayList<>();
    private final List<MatterNetworkControllerBlockEntity.ControllerEntry> allEntries = new ArrayList<>();
    private final List<MatterNetworkControllerBlockEntity.ControllerEntry> displayEntries = new ArrayList<>();
    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(DETAIL_COLOR_SLOT_XS, new int[]{DETAIL_COLOR_SLOT_Y, DETAIL_COLOR_SLOT_Y, DETAIL_COLOR_SLOT_Y});
    private final Map<BlockPos, int[]> pendingNodeColorOverrides = new HashMap<>();
    private EditBox searchBox;
    private EditBox detailIdBox;
    private Button filterButton;
    private Button sortButton;
    private Button detailModeButton;
    private Button openButton;
    private Button locateButton;
    private BlockPos selectedPos;
    private float scrollOffset;
    private float detailScrollOffset;
    private boolean scrolling;
    private boolean detailScrolling;
    private FilterMode filterMode = FilterMode.ALL;
    private SortMode sortMode = SortMode.NAME;
    private int selectedDetailChannel = MatterPylonBlockEntity.CHANNEL_ENERGY;
    private String detailLastSubmittedId = "";
    private int detailPendingSubmitTicks = -1;
    private boolean detailUserEditedIdBox;
    private boolean detailSuppressIdResponder;
    private BlockPos detailBoundPos;
    private int detailBoundChannel = -1;

    public MatterNetworkControllerScreen(MatterNetworkControllerMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 324;
        this.imageHeight = 222;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = GuiWidgets.textField(font, leftPos + LIST_LEFT - 1, topPos + SEARCH_TOP - 1, SEARCH_WIDTH, SEARCH_HEIGHT, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setResponder(ignored -> rebuildDisplayEntries());
        addRenderableWidget(searchBox);
        detailIdBox = GuiWidgets.textField(font, leftPos + DETAIL_ID_FIELD_X, topPos + DETAIL_ID_FIELD_Y, DETAIL_ID_FIELD_WIDTH, DETAIL_ID_FIELD_HEIGHT, Component.translatable("screen.matterworks.matter_pylon.id"));
        detailIdBox.setMaxLength(10);
        detailIdBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        detailIdBox.setResponder(value -> {
            if (detailSuppressIdResponder) {
                return;
            }
            detailUserEditedIdBox = true;
            detailPendingSubmitTicks = value.isEmpty() ? -1 : ID_SUBMIT_DEBOUNCE_TICKS;
        });
        addRenderableWidget(detailIdBox);

        rowButtons.clear();
        int listClipLeft = leftPos + LIST_LEFT + LIST_CLIP_INSET;
        int listClipTop = topPos + LIST_TOP + LIST_CLIP_INSET;
        int listClipRight = leftPos + LIST_LEFT + LIST_WIDTH - LIST_CLIP_INSET;
        int listClipBottom = topPos + LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT - LIST_CLIP_INSET;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            rowButtons.add(addRenderableWidget(GuiWidgets.clippedLeftAlignedSelectablePanelButton(
                    leftPos + LIST_LEFT,
                    topPos + LIST_TOP + row * ROW_HEIGHT,
                    LIST_WIDTH,
                    18,
                    listClipLeft,
                    listClipTop,
                    listClipRight,
                    listClipBottom,
                    Component.empty(),
                    button -> selectVisibleRow(rowIndex)
            )));
        }

        detailChannelButtons.clear();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            final int selectedChannel = channel;
            detailChannelButtons.add(new ChannelButtonEntry(
                    channel,
                    addRenderableWidget(GuiWidgets.selectablePanelButton(
                            leftPos + DETAIL_CHANNEL_BUTTONS_X,
                            topPos + DETAIL_CHANNEL_BUTTONS_Y,
                            20,
                            DETAIL_CHANNEL_BUTTONS_HEIGHT,
                            getChannelButtonLabel(channel),
                            button -> selectDetailChannel(selectedChannel)
                    ))
            ));
        }

        filterButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 7, topPos + 17, 71, 18, filterMode.label, button -> cycleFilterMode()));
        sortButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + 81, topPos + 17, 71, 18, sortMode.label, button -> cycleSortMode()));
        detailModeButton = addRenderableWidget(GuiWidgets.panelButton(
                leftPos + DETAIL_MODE_BUTTON_X,
                topPos + DETAIL_MODE_BUTTON_Y,
                DETAIL_MODE_BUTTON_WIDTH,
                DETAIL_MODE_BUTTON_HEIGHT,
                Component.literal("..."),
                button -> cycleSelectedNodeMode(false)
        ));
        openButton = addRenderableWidget(GuiWidgets.panelButton(
                leftPos + OPEN_GUI_BUTTON_X,
                topPos + OPEN_GUI_BUTTON_Y,
                71,
                18,
                Component.translatable("screen.matterworks.matter_network_controller.open_gui"),
                button -> triggerAction(MatterNetworkControllerActionPayload.ACTION_OPEN_GUI)
        ));
        locateButton = addRenderableWidget(GuiWidgets.panelButton(
                leftPos + LOCATE_BUTTON_X,
                topPos + LOCATE_BUTTON_Y,
                71,
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
        if (sortMode == SortMode.NEAREST || sortMode == SortMode.FURTHEST) {
            rebuildDisplayEntries();
        }
        tickDetailControls();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        renderInactiveFilterCover(guiGraphics);
        renderSelectedNodeSlots(guiGraphics);
        int handleTop = getScrollbarHandleTop();
        guiGraphics.blitSprite(
                displayEntries.size() > VISIBLE_ROWS ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
                leftPos + SCROLLBAR_X,
                handleTop,
                SCROLLBAR_WIDTH,
                getScrollbarHandleHeight()
        );
        List<DetailTextLine> detailLines = buildDetailTextLines();
        guiGraphics.blitSprite(
                canScrollDetailInfo(detailLines) ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
                leftPos + DETAIL_SCROLLBAR_X,
                getDetailScrollbarHandleTop(detailLines),
                DETAIL_SCROLLBAR_WIDTH,
                getScrollbarHandleHeight()
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, getDisplayedTitleText(), 8, 6, 0x404040, false);
        renderDetailInfo(guiGraphics);
    }

    private void renderDetailInfo(GuiGraphics guiGraphics) {
        List<DetailTextLine> detailLines = buildDetailTextLines();
        int firstVisibleLine = getFirstVisibleDetailLine(detailLines);
        int visibleLineCount = getVisibleDetailLineCount();
        int y = DETAIL_INFO_Y;
        guiGraphics.enableScissor(
                leftPos + DETAIL_INFO_X,
                topPos + DETAIL_INFO_Y,
                leftPos + DETAIL_INFO_X + DETAIL_INFO_WIDTH,
                topPos + DETAIL_INFO_Y + DETAIL_INFO_HEIGHT
        );
        for (int index = 0; index < visibleLineCount && firstVisibleLine + index < detailLines.size(); index++) {
            DetailTextLine line = detailLines.get(firstVisibleLine + index);
            guiGraphics.drawString(font, trimToWidth(line.text(), DETAIL_INFO_WIDTH), DETAIL_INFO_X, y, line.color(), false);
            y += DETAIL_INFO_LINE_HEIGHT;
        }
        guiGraphics.disableScissor();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (detailIdBox != null && detailIdBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submitDetailId(true);
                return true;
            }
            if (detailIdBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return keyCode != GLFW.GLFW_KEY_ESCAPE;
        }
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
        if (detailIdBox != null && detailIdBox.isFocused() && detailIdBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        submitDetailId(true);
        super.onClose();
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
            updateSelectedNode(displayEntries.get(absoluteIndex).pos(), true);
            refreshButtons();
        }
    }

    private void refreshEntries() {
        List<MatterNetworkControllerBlockEntity.ControllerEntry> latest = applyPendingColorOverrides(menu.getBlockEntity().getOverviewEntries());
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

        BlockPos nextSelectedPos = selectedPos;
        if (nextSelectedPos == null || findEntry(displayEntries, nextSelectedPos) == null) {
            nextSelectedPos = displayEntries.isEmpty() ? null : displayEntries.get(0).pos();
        }
        updateSelectedNode(nextSelectedPos, true);

        ensureSelectionVisible();
        refreshButtons();
    }

    private void updateSelectedNode(BlockPos nextSelectedPos, boolean syncServer) {
        boolean changed = !Objects.equals(selectedPos, nextSelectedPos);
        selectedPos = nextSelectedPos;
        menu.setSelectedTargetPos(nextSelectedPos);
        if (changed) {
            detailScrollOffset = 0.0F;
        }
        if (syncServer && changed) {
            PacketDistributor.sendToServer(new SetMatterNetworkControllerSelectedNodePayload(menu.getBlockPos(), nextSelectedPos));
        }
    }

    private void refreshButtons() {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        ensureSelectedDetailChannel(selectedEntry);
        menu.setActiveFilterChannel(selectedDetailChannel);
        int firstVisibleIndex = getFirstVisibleIndex();
        for (int row = 0; row < rowButtons.size(); row++) {
            GuiWidgets.SelectablePanelButton button = rowButtons.get(row);
            int absoluteIndex = firstVisibleIndex + row;
            if (absoluteIndex < displayEntries.size()) {
                MatterNetworkControllerBlockEntity.ControllerEntry entry = displayEntries.get(absoluteIndex);
                button.setMessage(Component.literal(trimToWidth(getPrimaryLabel(entry), LIST_WIDTH - 12)));
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
        refreshDetailChannelButtons(selectedEntry);
        refreshDetailControls(selectedEntry);

        boolean hasSelection = selectedEntry != null;
        openButton.active = hasSelection;
        locateButton.active = hasSelection;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry != null && colorPicker.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight,
                (index, color) -> setSelectedNodeColor(selectedEntry, index, color),
                index -> getSelectedNodeColor(selectedEntry, index))) {
            return true;
        }
        if (button == 1 && detailModeButton != null && detailModeButton.active && detailModeButton.visible && detailModeButton.isMouseOver(mouseX, mouseY)) {
            GuiWidgets.playButtonClickSound();
            cycleSelectedNodeMode(true);
            return true;
        }
        if (button == 0 && canScrollList() && isOverScrollbar(mouseX, mouseY)) {
            scrolling = true;
            setScrollOffsetFromMouse(mouseY);
            return true;
        }
        List<DetailTextLine> detailLines = buildDetailTextLines();
        if (button == 0 && canScrollDetailInfo(detailLines) && isOverDetailScrollbar(mouseX, mouseY)) {
            detailScrolling = true;
            setDetailScrollOffsetFromMouse(mouseY, detailLines);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSelectedNodeSlotTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            setScrollOffsetFromMouse(mouseY);
            return true;
        }
        if (detailScrolling) {
            setDetailScrollOffsetFromMouse(mouseY, buildDetailTextLines());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrolling = false;
            detailScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry != null && colorPicker.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, imageHeight,
                (index, color) -> setSelectedNodeColor(selectedEntry, index, color),
                index -> getSelectedNodeColor(selectedEntry, index))) {
            return true;
        }
        if (isOverList(mouseX, mouseY) || isOverScrollbar(mouseX, mouseY)) {
            scrollByRows((int) -Math.signum(scrollY));
            return true;
        }
        if (isOverDetailInfo(mouseX, mouseY) || isOverDetailScrollbar(mouseX, mouseY)) {
            scrollDetailByRows((int) -Math.signum(scrollY), buildDetailTextLines());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private String getDisplayedTitleText() {
        String text = menu.getBlockDisplayName() + " - " + allEntries.size() + " Connected Nodes";
        return font.plainSubstrByWidth(text, 220);
    }

    private void cycleFilterMode() {
        filterMode = filterMode.next();
        rebuildDisplayEntries();
    }

    private void cycleSortMode() {
        sortMode = sortMode.next();
        rebuildDisplayEntries();
    }

    private void tickDetailControls() {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null || detailIdBox == null) {
            detailPendingSubmitTicks = -1;
            detailUserEditedIdBox = false;
            return;
        }
        if (!detailUserEditedIdBox) {
            return;
        }
        if (detailIdBox.isFocused()) {
            if (detailPendingSubmitTicks >= 0) {
                detailPendingSubmitTicks--;
                if (detailPendingSubmitTicks <= 0) {
                    submitDetailId(false);
                }
            }
        } else {
            detailPendingSubmitTicks = -1;
            String syncedValue = Integer.toString(getSelectedChannelId(selectedEntry, selectedDetailChannel));
            if (!detailIdBox.getValue().equals(syncedValue)) {
                setDetailIdBoxValue(syncedValue);
            }
            detailLastSubmittedId = syncedValue;
            detailUserEditedIdBox = false;
        }
    }

    private void selectDetailChannel(int channel) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null || !supportsChannel(selectedEntry, channel)) {
            return;
        }
        submitDetailId(true);
        selectedDetailChannel = channel;
        detailScrollOffset = 0.0F;
        refreshButtons();
    }

    private void cycleSelectedNodeMode(boolean backward) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null || !supportsChannel(selectedEntry, selectedDetailChannel)) {
            return;
        }
        PacketDistributor.sendToServer(new CycleMatterNetworkControllerNodeModePayload(
                menu.getBlockPos(),
                selectedEntry.pos(),
                selectedDetailChannel,
                backward
        ));
    }

    private MatterNetworkControllerBlockEntity.ControllerEntry getSelectedEntry() {
        return selectedPos == null ? null : findEntry(displayEntries, selectedPos);
    }

    private void renderSelectedNodeSlots(GuiGraphics guiGraphics) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) {
            return;
        }

        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, index -> getSelectedNodeColor(selectedEntry, index));
    }

    private void renderSelectedNodeSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) {
            return;
        }

        colorPicker.renderTooltip(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY, index -> getSelectedNodeColor(selectedEntry, index));
        renderSelectedNodeFilterTooltip(guiGraphics, mouseX, mouseY);
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

    private static DyeColor getSelectedNodeColor(MatterNetworkControllerBlockEntity.ControllerEntry entry, int index) {
        return switch (index) {
            case 0 -> DyeColor.byId(entry.colorOneId());
            case 1 -> DyeColor.byId(entry.colorTwoId());
            case 2 -> DyeColor.byId(entry.colorThreeId());
            default -> DyeColor.WHITE;
        };
    }

    private void setSelectedNodeColor(MatterNetworkControllerBlockEntity.ControllerEntry entry, int index, DyeColor color) {
        if (entry == null || color == null) {
            return;
        }
        int[] override = pendingNodeColorOverrides.computeIfAbsent(entry.pos(), ignored -> new int[]{entry.colorOneId(), entry.colorTwoId(), entry.colorThreeId()});
        if (index >= 0 && index < override.length) {
            override[index] = color.getId();
        }
        applyLocalColorOverride(entry.pos(), override);
        PacketDistributor.sendToServer(new SetPylonColorCodePayload(entry.pos(), index, color.getId()));
    }

    private List<MatterNetworkControllerBlockEntity.ControllerEntry> applyPendingColorOverrides(List<MatterNetworkControllerBlockEntity.ControllerEntry> sourceEntries) {
        if (pendingNodeColorOverrides.isEmpty()) {
            return sourceEntries;
        }
        List<MatterNetworkControllerBlockEntity.ControllerEntry> patchedEntries = new ArrayList<>(sourceEntries.size());
        for (MatterNetworkControllerBlockEntity.ControllerEntry entry : sourceEntries) {
            int[] override = pendingNodeColorOverrides.get(entry.pos());
            if (override == null) {
                patchedEntries.add(entry);
                continue;
            }
            if (matchesColorOverride(entry, override)) {
                pendingNodeColorOverrides.remove(entry.pos());
                patchedEntries.add(entry);
                continue;
            }
            patchedEntries.add(withUpdatedNodeColors(entry, override));
        }
        return patchedEntries;
    }

    private void applyLocalColorOverride(BlockPos pos, int[] override) {
        replaceEntryColors(allEntries, pos, override);
        replaceEntryColors(displayEntries, pos, override);
    }

    private static void replaceEntryColors(List<MatterNetworkControllerBlockEntity.ControllerEntry> entries, BlockPos pos, int[] override) {
        for (int index = 0; index < entries.size(); index++) {
            MatterNetworkControllerBlockEntity.ControllerEntry entry = entries.get(index);
            if (entry.pos().equals(pos)) {
                entries.set(index, withUpdatedNodeColors(entry, override));
                return;
            }
        }
    }

    private static boolean matchesColorOverride(MatterNetworkControllerBlockEntity.ControllerEntry entry, int[] override) {
        return override.length >= 3
                && entry.colorOneId() == override[0]
                && entry.colorTwoId() == override[1]
                && entry.colorThreeId() == override[2];
    }

    private static MatterNetworkControllerBlockEntity.ControllerEntry withUpdatedNodeColors(MatterNetworkControllerBlockEntity.ControllerEntry entry, int[] override) {
        return new MatterNetworkControllerBlockEntity.ControllerEntry(
                entry.pos(),
                entry.displayName(),
                override.length > 0 ? override[0] : entry.colorOneId(),
                override.length > 1 ? override[1] : entry.colorTwoId(),
                override.length > 2 ? override[2] : entry.colorThreeId(),
                entry.supportedChannelMask(),
                entry.active(),
                entry.linkCount(),
                entry.energyMode(),
                entry.itemMode(),
                entry.fluidMode(),
                entry.redstoneMode(),
                entry.energyId(),
                entry.itemId(),
                entry.fluidId(),
                entry.redstoneId(),
                entry.energyAmount(),
                entry.energyRole(),
                entry.itemAmount(),
                entry.itemRole(),
                entry.fluidAmount(),
                entry.fluidRole(),
                entry.redstoneAmount(),
                entry.redstoneRole(),
                entry.supportsUpgradeCrystals(),
                entry.crystalOneStackTag(),
                entry.crystalTwoStackTag(),
                entry.crystalThreeStackTag()
        );
    }

    private String getPrimaryLabel(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return entry.displayName();
    }

    private List<DetailTextLine> buildDetailTextLines() {
        List<DetailTextLine> detailLines = new ArrayList<>();
        NetworkTotals totals = calculateTotals();
        detailLines.add(new DetailTextLine("Nodes " + totals.totalNodes + " | Active " + totals.activeNodes, 0x404040));
        detailLines.add(new DetailTextLine("E " + totals.energyAmount + " fe/t | I " + totals.itemAmount + " i/t", 0x406040));
        detailLines.add(new DetailTextLine("F " + totals.fluidAmount + " mb/t | R " + totals.redstoneAmount + " rs", 0x406090));
        detailLines.add(new DetailTextLine("", 0x404040));

        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null) {
            detailLines.add(new DetailTextLine(allEntries.isEmpty() ? "No connected nodes" : "No matching nodes", 0x606060));
            return detailLines;
        }

        ensureSelectedDetailChannel(selectedEntry);
        detailLines.add(new DetailTextLine(getPrimaryLabel(selectedEntry), 0x202020));
        detailLines.add(new DetailTextLine((selectedEntry.active() ? "Active" : "Idle") + " | Links " + selectedEntry.linkCount(), selectedEntry.active() ? 0x507D50 : 0x505050));
        detailLines.add(new DetailTextLine("", 0x404040));
        detailLines.add(new DetailTextLine(getDetailChannelSummary(selectedEntry, MatterPylonBlockEntity.CHANNEL_ENERGY), getDetailChannelColor(MatterPylonBlockEntity.CHANNEL_ENERGY, selectedDetailChannel == MatterPylonBlockEntity.CHANNEL_ENERGY)));
        detailLines.add(new DetailTextLine(getDetailChannelSummary(selectedEntry, MatterPylonBlockEntity.CHANNEL_ITEMS), getDetailChannelColor(MatterPylonBlockEntity.CHANNEL_ITEMS, selectedDetailChannel == MatterPylonBlockEntity.CHANNEL_ITEMS)));
        detailLines.add(new DetailTextLine(getDetailChannelSummary(selectedEntry, MatterPylonBlockEntity.CHANNEL_FLUIDS), getDetailChannelColor(MatterPylonBlockEntity.CHANNEL_FLUIDS, selectedDetailChannel == MatterPylonBlockEntity.CHANNEL_FLUIDS)));
        detailLines.add(new DetailTextLine(getDetailChannelSummary(selectedEntry, MatterPylonBlockEntity.CHANNEL_REDSTONE), getDetailChannelColor(MatterPylonBlockEntity.CHANNEL_REDSTONE, selectedDetailChannel == MatterPylonBlockEntity.CHANNEL_REDSTONE)));
        detailLines.add(new DetailTextLine("", 0x404040));
        detailLines.add(new DetailTextLine("Edit " + getDetailChannelName(selectedDetailChannel) + " " + getModeShortLabel(getSelectedChannelMode(selectedEntry, selectedDetailChannel)) + " #" + getSelectedChannelId(selectedEntry, selectedDetailChannel), 0x404040));
        return detailLines;
    }

    private void renderSelectedNodeFilterTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component tooltip = null;
        if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_ITEMS) {
            if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_WHITELIST)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_BLACKLIST)) {
                tooltip = Component.literal("Blacklist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_WHITELIST)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_BLACKLIST)) {
                tooltip = Component.literal("Blacklist");
            }
        } else if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
            if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_WHITELIST)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_BLACKLIST)) {
                tooltip = Component.literal("Blacklist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_WHITELIST)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_BLACKLIST)) {
                tooltip = Component.literal("Blacklist");
            }
        }

        if (tooltip != null) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderInactiveFilterCover(GuiGraphics guiGraphics) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry != null && supportsFilterChannel(selectedEntry, selectedDetailChannel)) {
            return;
        }

        guiGraphics.fill(
                leftPos + DETAIL_FILTER_SECTION_X - 1,
                topPos + DETAIL_FILTER_SECTION_Y - 1,
                leftPos + DETAIL_FILTER_SECTION_X + DETAIL_FILTER_SECTION_WIDTH + 1,
                topPos + DETAIL_FILTER_SECTION_Y + DETAIL_FILTER_SECTION_HEIGHT + 1,
                GUI_BACKGROUND_FILL
        );
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }
        var slot = menu.slots.get(slotIndex);
        int slotX = leftPos + slot.x;
        int slotY = topPos + slot.y;
        return mouseX >= slotX && mouseX < slotX + 16
                && mouseY >= slotY && mouseY < slotY + 16;
    }

    private void refreshDetailChannelButtons(MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry) {
        List<Integer> supportedChannels = getSupportedChannels(selectedEntry);
        if (supportedChannels.isEmpty()) {
            for (ChannelButtonEntry entry : detailChannelButtons) {
                entry.button().visible = false;
                entry.button().active = false;
                entry.button().setSelected(false);
            }
            return;
        }

        int buttonWidth = (DETAIL_CHANNEL_BUTTONS_WIDTH - DETAIL_CHANNEL_BUTTON_GAP * (supportedChannels.size() - 1)) / supportedChannels.size();
        int extraWidth = DETAIL_CHANNEL_BUTTONS_WIDTH - (buttonWidth * supportedChannels.size() + DETAIL_CHANNEL_BUTTON_GAP * (supportedChannels.size() - 1));
        float textScale = getChannelLabelScale(supportedChannels, buttonWidth);
        int x = leftPos + DETAIL_CHANNEL_BUTTONS_X;

        for (ChannelButtonEntry entry : detailChannelButtons) {
            if (!supportsChannel(selectedEntry, entry.channel())) {
                entry.button().visible = false;
                entry.button().active = false;
                entry.button().setSelected(false);
                continue;
            }

            int width = buttonWidth + (extraWidth > 0 ? 1 : 0);
            if (extraWidth > 0) {
                extraWidth--;
            }
            entry.button().setPosition(x, topPos + DETAIL_CHANNEL_BUTTONS_Y);
            entry.button().setWidth(width);
            entry.button().setHeight(DETAIL_CHANNEL_BUTTONS_HEIGHT);
            entry.button().setMessage(getChannelButtonLabel(entry.channel()));
            entry.button().setTextScale(textScale);
            entry.button().setTextOffsetY(1);
            entry.button().visible = true;
            entry.button().active = true;
            entry.button().setSelected(entry.channel() == selectedDetailChannel);
            x += width + DETAIL_CHANNEL_BUTTON_GAP;
        }
    }

    private void refreshDetailControls(MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry) {
        boolean hasSelection = selectedEntry != null && supportsChannel(selectedEntry, selectedDetailChannel);
        detailIdBox.visible = hasSelection;
        detailIdBox.active = hasSelection;
        detailModeButton.visible = hasSelection;
        detailModeButton.active = hasSelection;
        if (!hasSelection) {
            detailPendingSubmitTicks = -1;
            detailUserEditedIdBox = false;
            detailBoundPos = null;
            detailBoundChannel = -1;
            setDetailIdBoxValue("");
            detailLastSubmittedId = "";
            detailModeButton.setMessage(Component.literal("..."));
            return;
        }

        BlockPos selectedPos = selectedEntry.pos();
        int selectedId = getSelectedChannelId(selectedEntry, selectedDetailChannel);
        if (!detailUserEditedIdBox || !selectedPos.equals(detailBoundPos) || selectedDetailChannel != detailBoundChannel) {
            String syncedValue = Integer.toString(selectedId);
            setDetailIdBoxValue(syncedValue);
            detailLastSubmittedId = syncedValue;
            detailPendingSubmitTicks = -1;
            detailUserEditedIdBox = false;
        }
        detailBoundPos = selectedPos;
        detailBoundChannel = selectedDetailChannel;
        detailModeButton.setMessage(Component.translatable(getSelectedChannelMode(selectedEntry, selectedDetailChannel).translationKey()));
    }

    private void ensureSelectedDetailChannel(MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry) {
        if (selectedEntry == null) {
            return;
        }
        if (!supportsChannel(selectedEntry, selectedDetailChannel)) {
            List<Integer> supportedChannels = getSupportedChannels(selectedEntry);
            selectedDetailChannel = supportedChannels.isEmpty() ? MatterPylonBlockEntity.CHANNEL_ENERGY : supportedChannels.get(0);
        }
    }

    private List<Integer> getSupportedChannels(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        if (entry == null) {
            return List.of();
        }
        List<Integer> supportedChannels = new ArrayList<>();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            if (supportsChannel(entry, channel)) {
                supportedChannels.add(channel);
            }
        }
        return supportedChannels;
    }

    private boolean supportsChannel(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        return entry != null && channel >= 0 && channel < MatterPylonBlockEntity.CHANNEL_COUNT
                && (entry.supportedChannelMask() & (1 << channel)) != 0;
    }

    private boolean supportsFilterChannel(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        return entry != null && (channel == MatterPylonBlockEntity.CHANNEL_ITEMS || channel == MatterPylonBlockEntity.CHANNEL_FLUIDS)
                && supportsChannel(entry, channel);
    }

    private float getChannelLabelScale(List<Integer> supportedChannels, int buttonWidth) {
        int maxLabelWidth = 0;
        for (int channel : supportedChannels) {
            maxLabelWidth = Math.max(maxLabelWidth, this.font.width(getChannelButtonLabel(channel)));
        }
        if (maxLabelWidth <= 0) {
            return 1.0F;
        }
        return Mth.clamp((buttonWidth - 6) / (float) maxLabelWidth, 0.35F, 1.0F);
    }

    private static Component getChannelButtonLabel(int channel) {
        return Component.literal(getChannelName(channel));
    }

    private static String getChannelName(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> "Energy";
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> "Items";
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> "Liquids";
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> "Redstone";
            default -> Integer.toString(channel + 1);
        };
    }

    private static String getChannelUnit(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> "fe/t";
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> "i/t";
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> "mb/t";
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> "rs";
            default -> "";
        };
    }

    private static String getDetailChannelName(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> "Energy";
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> "Items";
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> "Fluids";
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> "Redstone";
            default -> Integer.toString(channel + 1);
        };
    }

    private PylonMode getSelectedChannelMode(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        int modeOrdinal = switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> entry.energyMode();
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> entry.itemMode();
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> entry.fluidMode();
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> entry.redstoneMode();
            default -> PylonMode.DISABLED.ordinal();
        };
        PylonMode[] modes = PylonMode.values();
        return modes[Math.max(0, Math.min(modes.length - 1, modeOrdinal))];
    }

    private int getSelectedChannelId(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> entry.energyId();
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> entry.itemId();
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> entry.fluidId();
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> entry.redstoneId();
            default -> MatterPylonBlockEntity.DEFAULT_PYLON_ID;
        };
    }

    private int getSelectedChannelAmount(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> entry.energyAmount();
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> entry.itemAmount();
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> entry.fluidAmount();
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> entry.redstoneAmount();
            default -> 0;
        };
    }

    private int getSelectedChannelRole(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> entry.energyRole();
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> entry.itemRole();
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> entry.fluidRole();
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> entry.redstoneRole();
            default -> 0;
        };
    }

    private String getDetailChannelSummary(MatterNetworkControllerBlockEntity.ControllerEntry entry, int channel) {
        if (!supportsChannel(entry, channel)) {
            return getDetailChannelName(channel) + " OFF";
        }
        return getDetailChannelName(channel)
                + " "
                + getModeShortLabel(getSelectedChannelMode(entry, channel))
                + " #"
                + getSelectedChannelId(entry, channel)
                + " "
                + formatSelectedChannel(getSelectedChannelAmount(entry, channel), getSelectedChannelRole(entry, channel), getChannelUnit(channel));
    }

    private static String getModeShortLabel(PylonMode mode) {
        return switch (mode) {
            case DISABLED -> "OFF";
            case EXPORT -> "OUT";
            case IMPORT -> "IN";
            case IMPORT_EXPORT -> "IO";
        };
    }

    private static int getDetailChannelColor(int channel, boolean selected) {
        int base = switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> 0x406040;
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> 0x806030;
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> 0x406090;
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> 0x904040;
            default -> 0x404040;
        };
        return selected ? (base | 0x202020) : base;
    }

    private void submitDetailId(boolean normalizeField) {
        MatterNetworkControllerBlockEntity.ControllerEntry selectedEntry = getSelectedEntry();
        if (selectedEntry == null || detailIdBox == null || !supportsChannel(selectedEntry, selectedDetailChannel)) {
            return;
        }

        String rawValue = detailIdBox.getValue().trim();
        if (rawValue.isEmpty()) {
            return;
        }

        int parsed = Mth.clamp(parseIntSafely(rawValue), MatterPylonBlockEntity.DEFAULT_PYLON_ID, Integer.MAX_VALUE);
        String normalized = Integer.toString(parsed);
        detailPendingSubmitTicks = -1;

        if (normalizeField) {
            setDetailIdBoxValue(normalized);
        }

        if (normalized.equals(detailLastSubmittedId)
                && selectedEntry.pos().equals(detailBoundPos)
                && selectedDetailChannel == detailBoundChannel) {
            return;
        }

        PacketDistributor.sendToServer(new SetMatterNetworkControllerNodeIdPayload(
                menu.getBlockPos(),
                selectedEntry.pos(),
                selectedDetailChannel,
                parsed
        ));
        detailLastSubmittedId = normalized;
        detailUserEditedIdBox = false;
        detailBoundPos = selectedEntry.pos();
        detailBoundChannel = selectedDetailChannel;
    }

    private void setDetailIdBoxValue(String value) {
        detailSuppressIdResponder = true;
        detailIdBox.setValue(value);
        detailSuppressIdResponder = false;
    }

    private static int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return MatterPylonBlockEntity.DEFAULT_PYLON_ID;
        }
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

    private int getVisibleDetailLineCount() {
        return Math.max(1, DETAIL_INFO_HEIGHT / DETAIL_INFO_LINE_HEIGHT);
    }

    private int getFirstVisibleDetailLine(List<DetailTextLine> detailLines) {
        int maxScrollIndex = getMaxDetailScrollIndex(detailLines);
        return maxScrollIndex <= 0 ? 0 : Math.round(detailScrollOffset * maxScrollIndex);
    }

    private int getMaxDetailScrollIndex(List<DetailTextLine> detailLines) {
        return Math.max(0, detailLines.size() - getVisibleDetailLineCount());
    }

    private boolean canScrollDetailInfo(List<DetailTextLine> detailLines) {
        return getMaxDetailScrollIndex(detailLines) > 0;
    }

    private int getFirstVisibleIndex() {
        int maxScrollIndex = getMaxScrollIndex();
        return maxScrollIndex <= 0 ? 0 : Math.round(scrollOffset * maxScrollIndex);
    }

    private int getMaxScrollIndex() {
        return Math.max(0, displayEntries.size() - VISIBLE_ROWS);
    }

    private boolean canScrollList() {
        return getMaxScrollIndex() > 0;
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

    private void scrollDetailByRows(int rows, List<DetailTextLine> detailLines) {
        int maxScrollIndex = getMaxDetailScrollIndex(detailLines);
        if (maxScrollIndex <= 0 || rows == 0) {
            return;
        }
        int newIndex = Mth.clamp(getFirstVisibleDetailLine(detailLines) + rows, 0, maxScrollIndex);
        detailScrollOffset = newIndex / (float) maxScrollIndex;
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

    private void setDetailScrollOffsetFromMouse(double mouseY, List<DetailTextLine> detailLines) {
        int maxScrollIndex = getMaxDetailScrollIndex(detailLines);
        if (maxScrollIndex <= 0) {
            detailScrollOffset = 0.0F;
            return;
        }
        int handleHeight = getScrollbarHandleHeight();
        float trackHeight = DETAIL_SCROLLBAR_HEIGHT - handleHeight;
        if (trackHeight <= 0.0F) {
            detailScrollOffset = 0.0F;
        } else {
            float relative = (float) ((mouseY - (topPos + DETAIL_SCROLLBAR_Y)) - handleHeight / 2.0D);
            detailScrollOffset = Mth.clamp(relative / trackHeight, 0.0F, 1.0F);
        }
    }

    private int getScrollbarHandleTop() {
        return topPos + SCROLLBAR_Y + (int) ((SCROLLBAR_HEIGHT - getScrollbarHandleHeight()) * scrollOffset);
    }

    private int getDetailScrollbarHandleTop(List<DetailTextLine> detailLines) {
        if (!canScrollDetailInfo(detailLines)) {
            return topPos + DETAIL_SCROLLBAR_Y;
        }
        return topPos + DETAIL_SCROLLBAR_Y + (int) ((DETAIL_SCROLLBAR_HEIGHT - getScrollbarHandleHeight()) * detailScrollOffset);
    }

    private int getScrollbarHandleHeight() {
        return SCROLLBAR_HANDLE_HEIGHT;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLLBAR_X
                && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= topPos + SCROLLBAR_Y
                && mouseY < topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    private boolean isOverDetailScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + DETAIL_SCROLLBAR_X
                && mouseX < leftPos + DETAIL_SCROLLBAR_X + DETAIL_SCROLLBAR_WIDTH
                && mouseY >= topPos + DETAIL_SCROLLBAR_Y
                && mouseY < topPos + DETAIL_SCROLLBAR_Y + DETAIL_SCROLLBAR_HEIGHT;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return mouseX >= leftPos + LIST_LEFT
                && mouseX < leftPos + LIST_LEFT + LIST_WIDTH
                && mouseY >= topPos + LIST_TOP
                && mouseY < topPos + LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private boolean isOverDetailInfo(double mouseX, double mouseY) {
        return mouseX >= leftPos + DETAIL_INFO_X
                && mouseX < leftPos + DETAIL_INFO_X + DETAIL_INFO_WIDTH
                && mouseY >= topPos + DETAIL_INFO_Y
                && mouseY < topPos + DETAIL_INFO_Y + DETAIL_INFO_HEIGHT;
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

    private record NetworkTotals(int totalNodes, int activeNodes, int energyAmount, int itemAmount, int fluidAmount, int redstoneAmount) {
    }

    private record ChannelButtonEntry(int channel, GuiWidgets.SelectablePanelButton button) {
    }

    private record DetailTextLine(String text, int color) {
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
        },
        INACTIVE("Inactive") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return !entry.active();
            }
        },
        ENERGY("Energy") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return entry.energyAmount() > 0;
            }
        },
        FLUIDS("Fluids") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return entry.fluidAmount() > 0;
            }
        },
        ITEMS("Items") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return entry.itemAmount() > 0;
            }
        },
        REDSTONE("Redstone") {
            @Override
            boolean matches(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
                return entry.redstoneAmount() > 0;
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
        ID("ID") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.<MatterNetworkControllerBlockEntity.ControllerEntry>comparingInt(MatterNetworkControllerBlockEntity.ControllerEntry::energyId)
                        .thenComparingInt(MatterNetworkControllerBlockEntity.ControllerEntry::itemId)
                        .thenComparingInt(MatterNetworkControllerBlockEntity.ControllerEntry::fluidId)
                        .thenComparingInt(MatterNetworkControllerBlockEntity.ControllerEntry::redstoneId)
                        .thenComparing(screen::getPrimaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.pos().asLong());
            }
        },
        NEAREST("Nearest") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.<MatterNetworkControllerBlockEntity.ControllerEntry>comparingDouble(screen::getDistanceToPlayer)
                        .thenComparing(screen::getPrimaryLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.pos().asLong());
            }
        },
        FURTHEST("Furthest") {
            @Override
            Comparator<MatterNetworkControllerBlockEntity.ControllerEntry> comparator(MatterNetworkControllerScreen screen) {
                return Comparator.<MatterNetworkControllerBlockEntity.ControllerEntry>comparingDouble(screen::getDistanceToPlayer)
                        .reversed()
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

    private double getDistanceToPlayer(MatterNetworkControllerBlockEntity.ControllerEntry entry) {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.blockPosition().distSqr(entry.pos())
                : 0.0D;
    }
}
