package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MatterGeneratorScreen extends AbstractContainerScreen<MatterGeneratorMenu> {
    public MatterGeneratorScreen(MatterGeneratorMenu menu, Inventory playerInventory, Component title) {
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

        drawEnergyBar(guiGraphics, left + 21, top + 18, menu.getScaledEnergyAmount(50));
        drawFuelSlot(guiGraphics, left + 79, top + 34);
        drawBurnBar(guiGraphics, left + 60, top + 25, menu.getScaledBurnProgress(28));
        drawPlayerInventorySlots(guiGraphics, left + 7, top + 83);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawFuelSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B93);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF232329);
    }

    private void drawBurnBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        guiGraphics.fill(x, y, x + 12, y + 30, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 11, y + 29, 0xFF232329);
        if (filledHeight > 0) {
            guiGraphics.fill(x + 2, y + 28 - filledHeight, x + 10, y + 28, 0xFFFFB347);
        }
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        guiGraphics.fill(x, y, x + 16, y + 52, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 51, 0xFF232329);
        if (filledHeight > 0) {
            guiGraphics.fill(x + 2, y + 50 - filledHeight, x + 14, y + 50, 0xFFE23D2D);
        }
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawFuelSlot(guiGraphics, left + column * 18, top + row * 18);
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            drawFuelSlot(guiGraphics, left + hotbarSlot * 18, top + 58);
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int energyX = this.leftPos + 21;
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
}
