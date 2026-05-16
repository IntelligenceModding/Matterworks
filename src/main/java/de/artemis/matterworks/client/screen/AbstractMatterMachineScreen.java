package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.AbstractMatterMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractMatterMachineScreen<T extends AbstractMatterMachineMenu> extends AbstractContainerScreen<T> {
    protected AbstractMatterMachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF2B2B31);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xFF3A3A42);

        drawSlot(guiGraphics, left + 33, top + 34);
        drawSlot(guiGraphics, left + 55, top + 34);
        drawSlot(guiGraphics, left + 115, top + 34);
        drawEnergyBar(guiGraphics, left + 13, top + 18, menu.getScaledEnergyAmount(50));
        drawTank(guiGraphics, left + 147, top + 18, menu.getScaledFluidAmount(50));

        drawPlayerInventorySlots(guiGraphics, left + 7, top + 83);
        drawProgress(guiGraphics, left + 79, top + 34, menu.getScaledProgress(24));
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

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, left + column * 18, top + row * 18);
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            drawSlot(guiGraphics, left + hotbarSlot * 18, top + 58);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B93);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF232329);
    }

    private void drawProgress(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + 24, y + 16, 0xFF1B1B1F);
        if (width > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + width, y + 15, 0xFF57C7FF);
        }
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        guiGraphics.fill(x, y, x + 16, y + 52, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 51, 0xFF232329);
        if (filledHeight > 0) {
            guiGraphics.fill(x + 2, y + 50 - filledHeight, x + 14, y + 50, 0xFFE23D2D);
        }
    }

    private void drawTank(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        guiGraphics.fill(x, y, x + 16, y + 52, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 51, 0xFF232329);
        if (filledHeight > 0) {
            guiGraphics.fill(x + 2, y + 50 - filledHeight, x + 14, y + 50, 0xFFD4B27A);
        }
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int tankX = this.leftPos + 147;
        int tankY = this.topPos + 18;
        if (mouseX >= tankX && mouseX < tankX + 16 && mouseY >= tankY && mouseY < tankY + 52) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.raw_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity()),
                    mouseX,
                    mouseY
            );
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int energyX = this.leftPos + 13;
        int energyY = this.topPos + 18;
        if (mouseX >= energyX && mouseX < energyX + 16 && mouseY >= energyY && mouseY < energyY + 52) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()),
                    mouseX,
                    mouseY
            );
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int progressX = this.leftPos + 79;
        int progressY = this.topPos + 34;
        if (mouseX >= progressX && mouseX < progressX + 24 && mouseY >= progressY && mouseY < progressY + 16) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()),
                    mouseX,
                    mouseY
            );
        }
    }
}
