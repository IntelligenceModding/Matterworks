package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterFluidTankMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class MatterFluidTankScreen extends AbstractContainerScreen<MatterFluidTankMenu> {
    public MatterFluidTankScreen(MatterFluidTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.translatable("screen.matterworks.matter_network.open"), button ->
                        PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(menu.getBlockPos())))
                .bounds(this.leftPos + 120, this.topPos + 6, 48, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF2B2B31);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xFF3A3A42);

        drawSlot(guiGraphics, left + 52, top + 34);
        drawSlot(guiGraphics, left + 106, top + 34);
        drawFluidBar(guiGraphics, left + 80, top + 18, menu.getScaledFluidAmount(50));
        drawTransferMarkers(guiGraphics, left + 70, top + 42);
        drawPlayerInventorySlots(guiGraphics, left + 7, top + 83);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderFluidTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B93);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF232329);
    }

    private void drawFluidBar(GuiGraphics guiGraphics, int x, int y, int filledHeight) {
        guiGraphics.fill(x, y, x + 16, y + 52, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 51, 0xFF232329);
        if (filledHeight > 0) {
            guiGraphics.fill(x + 2, y + 50 - filledHeight, x + 14, y + 50, 0xFF3A93FF);
        }
    }

    private void drawTransferMarkers(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x - 10, y, x - 1, y + 4, 0xFF5EC2FF);
        guiGraphics.fill(x + 16, y, x + 25, y + 4, 0xFF7CE3FF);
        guiGraphics.fill(x - 1, y - 1, x + 16, y + 5, 0xFF1B1B1F);
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

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int fluidX = this.leftPos + 80;
        int fluidY = this.topPos + 18;
        if (mouseX >= fluidX && mouseX < fluidX + 16 && mouseY >= fluidY && mouseY < fluidY + 52) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable("tooltip.matterworks.fluid", menu.getFluidAmount(), menu.getFluidCapacity()),
                    mouseX,
                    mouseY
            );
        }
    }
}
