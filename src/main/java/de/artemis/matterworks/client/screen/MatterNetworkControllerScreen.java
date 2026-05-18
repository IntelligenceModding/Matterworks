package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.network.MatterNetworkControllerActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MatterNetworkControllerScreen extends AbstractContainerScreen<MatterNetworkControllerMenu> {
    private static final int ROWS_PER_PAGE = 7;

    private final List<Button> rowButtons = new ArrayList<>();
    private final List<MatterNetworkControllerBlockEntity.ControllerEntry> entries = new ArrayList<>();
    private Button prevPageButton;
    private Button nextPageButton;
    private Button openButton;
    private Button locateButton;
    private int pageIndex;
    private BlockPos selectedPos;

    public MatterNetworkControllerScreen(MatterNetworkControllerMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 336;
        this.imageHeight = 214;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        rowButtons.clear();
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            final int rowIndex = row;
            rowButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> selectVisibleRow(rowIndex))
                    .bounds(leftPos + 12, topPos + 34 + row * 22, 142, 20)
                    .build()));
        }

        prevPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(leftPos + 12, topPos + 190, 20, 20)
                .build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(leftPos + 134, topPos + 190, 20, 20)
                .build());
        openButton = addRenderableWidget(Button.builder(Component.translatable("screen.matterworks.matter_network_controller.open_gui"), button -> triggerAction(MatterNetworkControllerActionPayload.ACTION_OPEN_GUI))
                .bounds(leftPos + 176, topPos + 160, 144, 20)
                .build());
        locateButton = addRenderableWidget(Button.builder(Component.translatable("screen.matterworks.matter_network_controller.locate"), button -> triggerAction(MatterNetworkControllerActionPayload.ACTION_LOCATE))
                .bounds(leftPos + 176, topPos + 186, 144, 20)
                .build());

        refreshEntries();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshEntries();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF24262B);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF32353C);
        guiGraphics.fill(leftPos + 8, topPos + 30, leftPos + 158, topPos + 186, 0xFF191B20);
        guiGraphics.fill(leftPos + 168, topPos + 30, leftPos + 328, topPos + 150, 0xFF191B20);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 12, 10, 0xE6E8EC, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.connected_nodes", entries.size()), 12, 20, 0xA8AFBB, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.details"), 176, 10, 0xE6E8EC, false);

        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected == null) {
            guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.no_selection"), 176, 34, 0x7B818C, false);
            return;
        }

        int y = 34;
        guiGraphics.drawString(font, trimToWidth(selected.displayName(), 148), 176, y, 0xFFFFFF, false);
        y += 14;
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.position", selected.pos().getX(), selected.pos().getY(), selected.pos().getZ()), 176, y, 0xC5C9D1, false);
        y += 14;
        guiGraphics.drawString(font, Component.translatable(
                selected.active()
                        ? "screen.matterworks.matter_network_controller.status.active"
                        : "screen.matterworks.matter_network_controller.status.idle"
        ), 176, y, selected.active() ? 0x67E08A : 0xA8AFBB, false);
        y += 14;
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_network_controller.links", selected.linkCount()), 176, y, 0xC5C9D1, false);
        y += 18;
        drawTransferLine(guiGraphics, y, Component.translatable("screen.matterworks.matter_pylon.channel.energy"), selected.energyAmount(), selected.energyRole(), 0x55D26A, " fe/t");
        y += 14;
        drawTransferLine(guiGraphics, y, Component.translatable("screen.matterworks.matter_pylon.channel.items"), selected.itemAmount(), selected.itemRole(), 0xE14B4B, " i/t");
        y += 14;
        drawTransferLine(guiGraphics, y, Component.translatable("screen.matterworks.matter_pylon.channel.fluids"), selected.fluidAmount(), selected.fluidRole(), 0x4C86F5, " mb/t");
        y += 14;
        drawTransferLine(guiGraphics, y, Component.translatable("screen.matterworks.matter_pylon.channel.redstone"), selected.redstoneAmount(), selected.redstoneRole(), 0xF0C34A, " rs");
    }

    private void drawTransferLine(GuiGraphics guiGraphics, int y, Component label, int amount, int roleOrdinal, int color, String suffix) {
        String rolePrefix = switch (roleOrdinal) {
            case 1 -> "+";
            case 2 -> "-";
            default -> "";
        };
        String text = amount > 0 ? rolePrefix + amount + suffix : Component.translatable("screen.matterworks.matter_network_controller.inactive").getString();
        guiGraphics.drawString(font, label, 176, y, color, false);
        guiGraphics.drawString(font, text, 250, y, 0xE6E8EC, false);
    }

    private void triggerAction(int action) {
        MatterNetworkControllerBlockEntity.ControllerEntry selected = getSelectedEntry();
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new MatterNetworkControllerActionPayload(menu.getBlockPos(), selected.pos(), action));
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (entries.size() - 1) / ROWS_PER_PAGE);
        pageIndex = Math.max(0, Math.min(maxPage, pageIndex + delta));
        refreshButtons();
    }

    private void selectVisibleRow(int rowIndex) {
        int absoluteIndex = pageIndex * ROWS_PER_PAGE + rowIndex;
        if (absoluteIndex >= 0 && absoluteIndex < entries.size()) {
            selectedPos = entries.get(absoluteIndex).pos();
            refreshButtons();
        }
    }

    private void refreshEntries() {
        List<MatterNetworkControllerBlockEntity.ControllerEntry> latest = menu.getBlockEntity().getOverviewEntries();
        if (entries.equals(latest)) {
            refreshButtons();
            return;
        }

        entries.clear();
        entries.addAll(latest);
        if (selectedPos == null || entries.stream().noneMatch(entry -> entry.pos().equals(selectedPos))) {
            selectedPos = entries.isEmpty() ? null : entries.get(0).pos();
        }
        int maxPage = Math.max(0, (entries.size() - 1) / ROWS_PER_PAGE);
        pageIndex = Math.max(0, Math.min(maxPage, pageIndex));
        refreshButtons();
    }

    private void refreshButtons() {
        for (int row = 0; row < rowButtons.size(); row++) {
            Button button = rowButtons.get(row);
            int absoluteIndex = pageIndex * ROWS_PER_PAGE + row;
            if (absoluteIndex < entries.size()) {
                MatterNetworkControllerBlockEntity.ControllerEntry entry = entries.get(absoluteIndex);
                String label = trimToWidth(entry.displayName() + " [" + entry.pos().toShortString() + "]", 130);
                if (entry.pos().equals(selectedPos)) {
                    label = "> " + trimToWidth(entry.displayName(), 122);
                }
                button.setMessage(Component.literal(label));
                button.visible = true;
                button.active = !entry.pos().equals(selectedPos);
            } else {
                button.setMessage(Component.empty());
                button.visible = false;
                button.active = false;
            }
        }

        int maxPage = Math.max(0, (entries.size() - 1) / ROWS_PER_PAGE);
        prevPageButton.active = pageIndex > 0;
        nextPageButton.active = pageIndex < maxPage;
        boolean hasSelection = getSelectedEntry() != null;
        openButton.active = hasSelection;
        locateButton.active = hasSelection;
    }

    private MatterNetworkControllerBlockEntity.ControllerEntry getSelectedEntry() {
        if (selectedPos == null) {
            return null;
        }
        for (MatterNetworkControllerBlockEntity.ControllerEntry entry : entries) {
            if (entry.pos().equals(selectedPos)) {
                return entry;
            }
        }
        return null;
    }

    private String trimToWidth(String text, int width) {
        String trimmed = font.plainSubstrByWidth(text, width);
        return trimmed.length() < text.length() ? trimmed + "..." : trimmed;
    }
}
