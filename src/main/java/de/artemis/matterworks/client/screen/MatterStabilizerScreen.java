package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterStabilizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MatterStabilizerScreen extends AbstractContainerScreen<MatterStabilizerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_stabilizer.png");
    private static final int PROGRESS_BAR_X = 29;
    private static final int PROGRESS_BAR_Y = 108;
    private static final int ENERGY_BAR_X = 29;
    private static final int ENERGY_BAR_Y = 118;
    private static final int BAR_WIDTH = 118;
    private static final int BAR_HEIGHT = 6;
    private static final int RAW_TANK_X = 8;
    private static final int REFINED_TANK_X = 63;
    private static final int UNSTABLE_TANK_X = 118;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 50;
    private static final int TANK_HEIGHT = 49;
    private static final int ENERGY_FILL_COLOR = 0xFFE23D2D;
    private static final int ENERGY_FILL_TOP_COLOR = 0xFFF06A5E;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public MatterStabilizerScreen(MatterStabilizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (sideConfig.isShowing()) {
            return;
        }
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (sideConfig.isShowing()) {
            sideConfig.renderBackground(guiGraphics, leftPos, topPos);
            return;
        }
        GuiWidgets.fillHorizontalGauge(
                guiGraphics,
                leftPos + PROGRESS_BAR_X,
                topPos + PROGRESS_BAR_Y,
                menu.getScaledProgress(BAR_WIDTH),
                BAR_HEIGHT,
                menu.getProgressBarColor(),
                menu.getProgressBarColor()
        );
        GuiWidgets.fillHorizontalGauge(
                guiGraphics,
                leftPos + ENERGY_BAR_X,
                topPos + ENERGY_BAR_Y,
                menu.getScaledEnergyAmount(BAR_WIDTH),
                BAR_HEIGHT,
                ENERGY_FILL_COLOR,
                ENERGY_FILL_TOP_COLOR
        );
        int rawFillColor = GuiWidgets.getFluidFillColor(menu.getRawMatterFluidStack(), 0xFFE2DED6);
        int rawHighlightColor = GuiWidgets.getFluidHighlightColor(menu.getRawMatterFluidStack(), 0xFFF3EFE8);
        int refinedFillColor = GuiWidgets.getFluidFillColor(menu.getRefinedMatterFluidStack(), 0xFF9CE5FF);
        int refinedHighlightColor = GuiWidgets.getFluidHighlightColor(menu.getRefinedMatterFluidStack(), 0xFFC7F2FF);
        int unstableFillColor = GuiWidgets.getFluidFillColor(menu.getUnstableMatterFluidStack(), 0xFFF08A7A);
        int unstableHighlightColor = GuiWidgets.getFluidHighlightColor(menu.getUnstableMatterFluidStack(), 0xFFFFB09D);
        GuiWidgets.fillVerticalGauge(guiGraphics, leftPos + RAW_TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT, menu.getScaledRawMatterAmount(TANK_HEIGHT), rawFillColor, rawHighlightColor);
        GuiWidgets.fillVerticalGauge(guiGraphics, leftPos + REFINED_TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT, menu.getScaledRefinedMatterAmount(TANK_HEIGHT), refinedFillColor, refinedHighlightColor);
        GuiWidgets.fillVerticalGauge(guiGraphics, leftPos + UNSTABLE_TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT, menu.getScaledUnstableMatterAmount(TANK_HEIGHT), unstableFillColor, unstableHighlightColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sideConfig.syncSlotLayout(menu);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, titleLabelX, titleLabelY, 0x404040, title.getString(), inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderEnergyTooltip(guiGraphics, mouseX, mouseY);
            renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + RAW_TANK_X, "tooltip.matterworks.raw_matter_tank", menu.getRawMatterAmount(), menu.getRawMatterCapacity());
            renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + REFINED_TANK_X, "tooltip.matterworks.refined_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity());
            renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + UNSTABLE_TANK_X, "tooltip.matterworks.unstable_matter_tank", menu.getUnstableMatterAmount(), menu.getUnstableMatterCapacity());
            renderProgressTooltip(guiGraphics, mouseX, mouseY);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, String key, int amount, int capacity) {
        int y = this.topPos + TANK_Y;
        if (mouseX >= x && mouseX < x + TANK_WIDTH && mouseY >= y && mouseY < y + TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable(key, amount, capacity), mouseX, mouseY);
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + PROGRESS_BAR_X;
        int y = this.topPos + PROGRESS_BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
