package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterRecyclerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MatterRecyclerScreen extends AbstractContainerScreen<MatterRecyclerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_recycler.png");
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 39;
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 64;
    private static final int PROGRESS_BAR_X = 80;
    private static final int PROGRESS_BAR_Y = 18;
    private static final int PROGRESS_BAR_WIDTH = 16;
    private static final int PROGRESS_BAR_HEIGHT = 85;
    private static final int FLUID_BAR_X = 152;
    private static final int FLUID_BAR_Y = 39;
    private static final int FLUID_BAR_WIDTH = 16;
    private static final int FLUID_BAR_HEIGHT = 64;

    public MatterRecyclerScreen(MatterRecyclerMenu menu, Inventory playerInventory, Component title) {
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
        drawVerticalBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, menu.getScaledEnergyAmount(ENERGY_BAR_HEIGHT), 0xFFE23D2D);
        drawVerticalBar(guiGraphics, left + PROGRESS_BAR_X, top + PROGRESS_BAR_Y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, menu.getScaledProgress(PROGRESS_BAR_HEIGHT), progressColor);
        drawVerticalBar(guiGraphics, left + FLUID_BAR_X, top + FLUID_BAR_Y, FLUID_BAR_WIDTH, FLUID_BAR_HEIGHT, menu.getScaledFluidAmount(FLUID_BAR_HEIGHT), 0xFFD4B27A);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        renderProgressTooltip(guiGraphics, mouseX, mouseY);
        renderTankTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawVerticalBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int filledHeight, int color) {
        if (filledHeight <= 0) {
            return;
        }
        guiGraphics.fill(x, y + height - filledHeight, x + width, y + height, color);
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= x && mouseX < x + ENERGY_BAR_WIDTH && mouseY >= y && mouseY < y + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + FLUID_BAR_X;
        int y = this.topPos + FLUID_BAR_Y;
        if (mouseX >= x && mouseX < x + FLUID_BAR_WIDTH && mouseY >= y && mouseY < y + FLUID_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.raw_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity()), mouseX, mouseY);
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + PROGRESS_BAR_X;
        int y = this.topPos + PROGRESS_BAR_Y;
        if (mouseX >= x && mouseX < x + PROGRESS_BAR_WIDTH && mouseY >= y && mouseY < y + PROGRESS_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }
}
