package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.FluidTankBlockEntity;
import de.artemis.matterworks.common.menu.FluidTankMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidTankScreen extends AbstractRenamableContainerScreen<FluidTankMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/fluid_tank.png");
    private static final int GRAPH_X = 8;
    private static final int GRAPH_Y = 18;
    private static final int GRAPH_WIDTH = 160;
    private static final int GRAPH_HEIGHT = 41;
    private static final int TANK_BAR_X = 8;
    private static final int TANK_BAR_Y = 63;
    private static final int TANK_BAR_WIDTH = 160;
    private static final int TANK_BAR_HEIGHT = 41;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int GRAPH_COLOR = 0xFF3A93FF;
    private static final int GRAPH_AREA_COLOR = 0x353A93FF;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public FluidTankScreen(FluidTankMenu menu, Inventory playerInventory, Component title) {
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
        renderTransferGraph(guiGraphics, mouseX, mouseY);
        renderTankBar(guiGraphics);
        VanillaGuiHelper.drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (sideConfig.isShowing()) {
            return;
        }
        renderEditableTitle(guiGraphics, 8, 6, 0x404040);

        String rateText = GuiWidgets.formatRateText(menu.getCurrentNetTransferRate(), "mB/t", true);
        guiGraphics.drawString(font, rateText, imageWidth - 8 - font.width(rateText), 6, 0x404040, false);
        drawCenteredFluidName(guiGraphics);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sideConfig.syncSlotLayout(menu);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, getEditableTitleX(), getEditableTitleY(), getEditableTitleColor(), isEditingName() ? "" : getDisplayedTitleText(), inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderDataTooltips(guiGraphics, mouseX, mouseY);
        }
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sideConfig.mouseDragged(menu, mouseX, mouseY, button, dragX, dragY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sideConfig.mouseReleased(menu, mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    @Override
    protected int getEditableTitleRightEdge() {
        if (sideConfig.isShowing()) {
            return imageWidth - 8;
        }
        String rateText = GuiWidgets.formatRateText(menu.getCurrentNetTransferRate(), "mB/t", true);
        return imageWidth - 8 - font.width(rateText) - 6;
    }

    private void renderTransferGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int historyCapacity = menu.getHistoryCapacity();
        GuiWidgets.drawInsetGraph(
                guiGraphics,
                leftPos + GRAPH_X,
                topPos + GRAPH_Y,
                GRAPH_WIDTH,
                GRAPH_HEIGHT,
                menu.getHistorySize(),
                historyCapacity,
                visibleIndex -> getVisibleSample(visibleIndex).netRate(),
                menu.getCurrentNetTransferRate(),
                GuiWidgets.getHoveredHistorySampleIndex(mouseX, mouseY, leftPos + GRAPH_X, topPos + GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT, historyCapacity),
                GRAPH_COLOR,
                GRAPH_AREA_COLOR
        );
    }

    private void renderTankBar(GuiGraphics guiGraphics) {
        GuiWidgets.drawInsetVerticalFluidBar(
                guiGraphics,
                leftPos + TANK_BAR_X,
                topPos + TANK_BAR_Y,
                TANK_BAR_WIDTH,
                TANK_BAR_HEIGHT,
                menu.getScaledFluidAmount(TANK_BAR_HEIGHT - 4),
                menu.getFluidStack()
        );
    }

    private void renderDataTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + TANK_BAR_X, topPos + TANK_BAR_Y, TANK_BAR_WIDTH, TANK_BAR_HEIGHT)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(getFluidNameComponent().getString() + ": " + menu.getFluidAmount() + " / " + menu.getFluidCapacity() + " mB"));
            lines.add(TooltipBarHelper.buildBar(
                    getFluidRatio(),
                    TOOLTIP_BAR_WIDTH,
                    ChatFormatting.AQUA,
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

    private void drawCenteredFluidName(GuiGraphics guiGraphics) {
        String fluidName = getFluidNameComponent().getString();
        String trimmedName = font.plainSubstrByWidth(fluidName, TANK_BAR_WIDTH - 8);
        int x = (imageWidth - font.width(trimmedName)) / 2;
        int y = TANK_BAR_Y + 15;
        guiGraphics.drawString(font, trimmedName, x, y, 0xF0F0F0, false);
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
        FluidTankBlockEntity.TransferHistorySample sample = menu.getHistorySample(historyIndex);
        return List.of(GuiWidgets.formatRateComponent(sample.netRate(), "mB/t", true));
    }

    private FluidTankBlockEntity.TransferHistorySample getVisibleSample(int visibleIndex) {
        int historyIndex = GuiWidgets.getHistoryIndexForVisibleIndex(visibleIndex, menu.getHistorySize(), menu.getHistoryCapacity());
        return historyIndex < 0
                ? FluidTankBlockEntity.TransferHistorySample.EMPTY
                : menu.getHistorySample(historyIndex);
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private float getFluidRatio() {
        int capacity = menu.getFluidCapacity();
        return capacity <= 0 ? 0.0F : menu.getFluidAmount() / (float) capacity;
    }

    private int getFillPercent() {
        return Math.round(getFluidRatio() * 100.0F);
    }

    private Component getFluidNameComponent() {
        return menu.getFluidStack().isEmpty() ? Component.literal("Empty") : menu.getFluidStack().getHoverName();
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, () -> PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(menu.getBlockPos(), menu.isRemoteAccess())));
    }
}
