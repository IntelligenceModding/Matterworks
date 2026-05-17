package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class MatterStorageBarrelScreen extends AbstractContainerScreen<MatterStorageBarrelMenu> {
    public MatterStorageBarrelScreen(MatterStorageBarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
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

        drawSlotGrid(guiGraphics, left + 7, top + 17, 6);
        drawSlotGrid(guiGraphics, left + 7, top + 139, 3);
        drawHotbar(guiGraphics, left + 7, top + 197);
    }

    private void drawSlotGrid(GuiGraphics guiGraphics, int left, int top, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, left + column * 18, top + row * 18);
            }
        }
    }

    private void drawHotbar(GuiGraphics guiGraphics, int left, int top) {
        for (int slot = 0; slot < 9; slot++) {
            drawSlot(guiGraphics, left + slot * 18, top);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1B1B1F);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B93);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF232329);
    }
}
