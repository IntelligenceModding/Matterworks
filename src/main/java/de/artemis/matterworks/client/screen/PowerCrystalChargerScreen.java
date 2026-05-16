package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PowerCrystalChargerScreen extends AbstractContainerScreen<PowerCrystalChargerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/power_crystal_charger.png");
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 39;
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 64;

    public PowerCrystalChargerScreen(PowerCrystalChargerMenu menu, Inventory playerInventory, Component title) {
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

        guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        drawEnergyBar(guiGraphics, left + ENERGY_BAR_X, top + ENERGY_BAR_Y, menu.getScaledEnergyAmount(ENERGY_BAR_HEIGHT));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawEnergyBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        if (filledHeight > 0) {
            guiGraphics.fill(x, y + ENERGY_BAR_HEIGHT - filledHeight, x + ENERGY_BAR_WIDTH, y + ENERGY_BAR_HEIGHT, 0xFFE23D2D);
        }
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int energyX = this.leftPos + ENERGY_BAR_X;
        int energyY = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= energyX && mouseX < energyX + ENERGY_BAR_WIDTH && mouseY >= energyY && mouseY < energyY + ENERGY_BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }
}
