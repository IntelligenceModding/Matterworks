package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterAnalyzerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MatterAnalyzerScreen extends AbstractContainerScreen<MatterAnalyzerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_analyzer.png");
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 39;
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 64;
    private static final int PROGRESS_BAR_X = 80;
    private static final int PROGRESS_BAR_Y = 18;
    private static final int PROGRESS_BAR_WIDTH = 16;
    private static final int PROGRESS_BAR_HEIGHT = 85;

    public MatterAnalyzerScreen(MatterAnalyzerMenu menu, Inventory playerInventory, Component title) {
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
        int progressColor = menu.getProgressBarColor();

        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        drawEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, menu.getScaledEnergyAmount(ENERGY_BAR_HEIGHT));
        drawProgress(guiGraphics, left + PROGRESS_BAR_X, top + PROGRESS_BAR_Y, menu.getScaledProgress(PROGRESS_BAR_HEIGHT), progressColor);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        renderProgressTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        if (filledHeight > 0) {
            guiGraphics.fill(x, y + ENERGY_BAR_HEIGHT - filledHeight, x + ENERGY_BAR_WIDTH, y + ENERGY_BAR_HEIGHT, 0xFFE23D2D);
        }
    }

    private void drawProgress(GuiGraphics guiGraphics, int x, int y, int filledHeight, int color) {
        if (filledHeight > 0) {
            guiGraphics.fill(x, y + PROGRESS_BAR_HEIGHT - filledHeight, x + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, color);
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int energyX = this.leftPos + ENERGY_BAR_X;
        int energyY = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= energyX && mouseX < energyX + ENERGY_BAR_WIDTH && mouseY >= energyY && mouseY < energyY + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()),
                    mouseX,
                    mouseY
            );
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int progressX = this.leftPos + PROGRESS_BAR_X;
        int progressY = this.topPos + PROGRESS_BAR_Y;
        if (mouseX >= progressX && mouseX < progressX + PROGRESS_BAR_WIDTH && mouseY >= progressY && mouseY < progressY + PROGRESS_BAR_HEIGHT) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()),
                    mouseX,
                    mouseY
            );
        }
    }
}
