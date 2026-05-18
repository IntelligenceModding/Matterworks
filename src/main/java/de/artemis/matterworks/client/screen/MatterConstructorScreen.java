package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterConstructorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MatterConstructorScreen extends AbstractContainerScreen<MatterConstructorMenu> {
    private static final int ENERGY_BAR_X = 8;
    private static final int REFINED_BAR_X = 30;
    private static final int PROGRESS_BAR_X = 80;
    private static final int SLUDGE_BAR_X = 152;
    private static final int BAR_Y = 39;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 64;
    private static final int PROGRESS_HEIGHT = 85;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public MatterConstructorScreen(MatterConstructorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.topPos -= 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + ENERGY_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + ENERGY_BAR_X + 2, top + BAR_Y + 2, BAR_WIDTH - 4, BAR_HEIGHT - 4, menu.getScaledEnergyAmount(BAR_HEIGHT - 4), 0xFFE23D2D);
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + REFINED_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + REFINED_BAR_X + 2, top + BAR_Y + 2, BAR_WIDTH - 4, BAR_HEIGHT - 4, menu.getScaledFluidAmount(BAR_HEIGHT - 4), 0xFF9CE5FF);
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + PROGRESS_BAR_X, top + 18, BAR_WIDTH, PROGRESS_HEIGHT);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + PROGRESS_BAR_X + 2, top + 20, BAR_WIDTH - 4, PROGRESS_HEIGHT - 4, menu.getScaledProgress(PROGRESS_HEIGHT - 4), menu.getProgressBarColor());
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + SLUDGE_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + SLUDGE_BAR_X + 2, top + BAR_Y + 2, BAR_WIDTH - 4, BAR_HEIGHT - 4, menu.getScaledSludgeAmount(BAR_HEIGHT - 4), 0xFF7B5739);
        sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + ENERGY_BAR_X, "tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity());
            renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + REFINED_BAR_X, "tooltip.matterworks.refined_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity());
            renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + SLUDGE_BAR_X, "tooltip.matterworks.matter_sludge_tank", menu.getSludgeAmount(), menu.getSludgeCapacity());
            renderProgressTooltip(guiGraphics, mouseX, mouseY);
        }
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

    private void renderBarTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, String key, int amount, int capacity) {
        int y = this.topPos + BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable(key, amount, capacity), mouseX, mouseY);
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + PROGRESS_BAR_X;
        int y = this.topPos + 18;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + PROGRESS_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
