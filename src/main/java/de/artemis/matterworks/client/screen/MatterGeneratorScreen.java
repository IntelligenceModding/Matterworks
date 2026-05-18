package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MatterGeneratorScreen extends AbstractContainerScreen<MatterGeneratorMenu> {
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

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

        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + 21, top + 18, 16, 52);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + 23, top + 20, 12, 48, menu.getScaledEnergyAmount(48), 0xFFE23D2D);
        VanillaGuiHelper.drawVerticalBarFrame(guiGraphics, left + 60, top + 25, 12, 30);
        VanillaGuiHelper.fillVerticalBar(guiGraphics, left + 62, top + 27, 8, 26, menu.getScaledBurnProgress(26), 0xFFFFB347);
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
            renderEnergyTooltip(guiGraphics, mouseX, mouseY);
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

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
