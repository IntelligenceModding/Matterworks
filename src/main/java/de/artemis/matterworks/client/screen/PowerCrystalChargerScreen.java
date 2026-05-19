package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PowerCrystalChargerScreen extends AbstractContainerScreen<PowerCrystalChargerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/charger.png");
    private static final int PROGRESS_BAR_X = 29;
    private static final int PROGRESS_BAR_Y = 39;
    private static final int BAR_WIDTH = 117;
    private static final int BAR_HEIGHT = 5;
    private static final int ENERGY_BAR_X = 29;
    private static final int ENERGY_BAR_Y = 49;

    public PowerCrystalChargerScreen(PowerCrystalChargerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 60;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        VanillaGuiHelper.fillHorizontalBar(guiGraphics, leftPos + PROGRESS_BAR_X, topPos + PROGRESS_BAR_Y, menu.getScaledProgress(BAR_WIDTH), BAR_HEIGHT, menu.getProgressBarColor());
        VanillaGuiHelper.fillHorizontalBar(guiGraphics, leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, menu.getScaledEnergyAmount(BAR_WIDTH), BAR_HEIGHT, 0xFFE23D2D);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderBarTooltips(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderBarTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isWithin(mouseX, mouseY, leftPos + PROGRESS_BAR_X, topPos + PROGRESS_BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
            return;
        }

        if (isWithin(mouseX, mouseY, leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, BAR_WIDTH, BAR_HEIGHT)) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }

    private boolean isWithin(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
