package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.item.MatterArchitectItem;
import de.artemis.matterworks.common.menu.MatterArchitectMenu;
import de.artemis.matterworks.common.multiblock.MatterArchitectBlueprintType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MatterArchitectScreen extends AbstractContainerScreen<MatterArchitectMenu> {
    public MatterArchitectScreen(MatterArchitectMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 128;
        this.imageHeight = 28 + MatterArchitectBlueprintType.values().length * 22;
    }

    @Override
    protected void init() {
        super.init();
        int buttonTop = topPos + 18;
        for (MatterArchitectBlueprintType type : MatterArchitectBlueprintType.values()) {
            GuiWidgets.SelectablePanelButton button = GuiWidgets.leftAlignedSelectablePanelButton(
                    leftPos + 8,
                    buttonTop,
                    112,
                    18,
                    type.displayName(),
                    pressed -> selectBlueprint(type)
            );
            button.setSelected(type == menu.getSelectedBlueprintType());
            addRenderableWidget(button);
            buttonTop += 22;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        VanillaGuiHelper.drawScreenBackground(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
    }

    private void selectBlueprint(MatterArchitectBlueprintType type) {
        MatterArchitectItem.setBlueprintType(menu.getArchitectStack(), type);
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, type.id());
            onClose();
        }
    }
}
