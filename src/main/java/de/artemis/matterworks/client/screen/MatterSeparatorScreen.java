package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterSeparatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MatterSeparatorScreen extends AbstractContainerScreen<MatterSeparatorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_separator.png");
    private static final int ENERGY_BAR_X = 8;
    private static final int REFINED_BAR_X = 30;
    private static final int PROGRESS_BAR_X = 80;
    private static final int SLUDGE_BAR_X = 152;
    private static final int BAR_Y = 39;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 64;
    private static final int PROGRESS_HEIGHT = 85;

    public MatterSeparatorScreen(MatterSeparatorMenu menu, Inventory playerInventory, Component title) {
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
        drawVerticalBar(guiGraphics, left + ENERGY_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT, menu.getScaledEnergyAmount(BAR_HEIGHT), 0xFFE23D2D);
        drawVerticalBar(guiGraphics, left + REFINED_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT, menu.getScaledFluidAmount(BAR_HEIGHT), 0xFF9CE5FF);
        drawVerticalBar(guiGraphics, left + PROGRESS_BAR_X, top + 18, BAR_WIDTH, PROGRESS_HEIGHT, menu.getScaledProgress(PROGRESS_HEIGHT), menu.getProgressBarColor());
        drawVerticalBar(guiGraphics, left + SLUDGE_BAR_X, top + BAR_Y, BAR_WIDTH, BAR_HEIGHT, menu.getScaledSludgeAmount(BAR_HEIGHT), 0xFF7B5739);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + ENERGY_BAR_X, "tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity());
        renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + REFINED_BAR_X, "tooltip.matterworks.refined_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity());
        renderBarTooltip(guiGraphics, mouseX, mouseY, this.leftPos + SLUDGE_BAR_X, "tooltip.matterworks.matter_sludge_tank", menu.getSludgeAmount(), menu.getSludgeCapacity());
        renderProgressTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawVerticalBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int filledHeight, int color) {
        if (filledHeight > 0) {
            guiGraphics.fill(x, y + height - filledHeight, x + width, y + height, color);
        }
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
}
