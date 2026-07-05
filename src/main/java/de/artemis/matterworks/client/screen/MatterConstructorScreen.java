package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterConstructorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MatterConstructorScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<MatterConstructorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/matter_constructor.png");
    private static final int PROGRESS_BAR_X = 29;
    private static final int PROGRESS_BAR_Y = 108;
    private static final int ENERGY_BAR_X = 29;
    private static final int ENERGY_BAR_Y = 118;
    private static final int BAR_WIDTH = 118;
    private static final int BAR_HEIGHT = 6;
    private static final int REFINED_TANK_X = 8;
    private static final int SLUDGE_TANK_X = 119;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 49;
    private static final int TANK_HEIGHT = 49;
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public MatterConstructorScreen(MatterConstructorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (sideConfig.isShowing()) {
            return;
        }
        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (sideConfig.isShowing()) {
            sideConfig.renderBackground(guiGraphics, leftPos, topPos);
            return;
        }

        GuiWidgets.fillHorizontalProgressGauge(
                guiGraphics,
                leftPos + PROGRESS_BAR_X,
                topPos + PROGRESS_BAR_Y,
                menu.getScaledProgress(BAR_WIDTH),
                BAR_HEIGHT
        );
        GuiWidgets.fillHorizontalEnergyGauge(
                guiGraphics,
                leftPos + ENERGY_BAR_X,
                topPos + ENERGY_BAR_Y,
                menu.getScaledEnergyAmount(BAR_WIDTH),
                BAR_HEIGHT
        );

        GuiWidgets.fillVerticalFluidGauge(
                guiGraphics,
                leftPos + REFINED_TANK_X,
                topPos + TANK_Y,
                TANK_WIDTH,
                TANK_HEIGHT,
                menu.getScaledFluidAmount(TANK_HEIGHT),
                menu.getFluidStack()
        );

        GuiWidgets.fillVerticalFluidGauge(
                guiGraphics,
                leftPos + SLUDGE_TANK_X,
                topPos + TANK_Y,
                TANK_WIDTH,
                TANK_HEIGHT,
                menu.getScaledSludgeAmount(TANK_HEIGHT),
                menu.getSludgeFluidStack()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sideConfig.syncSlotLayout(menu);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, titleLabelX, titleLabelY, 0x404040, title.getString(), inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else {
            renderEnergyTooltip(guiGraphics, mouseX, mouseY);
            renderProgressTooltip(guiGraphics, mouseX, mouseY);
            renderRefinedTankTooltip(guiGraphics, mouseX, mouseY);
            renderSludgeTankTooltip(guiGraphics, mouseX, mouseY);
        }
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + ENERGY_BAR_X;
        int y = this.topPos + ENERGY_BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.energy", menu.getEnergyStored(), menu.getEnergyCapacity()), mouseX, mouseY);
        }
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + PROGRESS_BAR_X;
        int y = this.topPos + PROGRESS_BAR_Y;
        if (mouseX >= x && mouseX < x + BAR_WIDTH && mouseY >= y && mouseY < y + BAR_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.progress", menu.getProgress(), menu.getMaxProgress()), mouseX, mouseY);
        }
    }

    private void renderRefinedTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + REFINED_TANK_X;
        int y = this.topPos + TANK_Y;
        if (mouseX >= x && mouseX < x + TANK_WIDTH && mouseY >= y && mouseY < y + TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.refined_matter_tank", menu.getFluidAmount(), menu.getFluidCapacity()), mouseX, mouseY);
        }
    }

    private void renderSludgeTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + SLUDGE_TANK_X;
        int y = this.topPos + TANK_Y;
        if (mouseX >= x && mouseX < x + TANK_WIDTH && mouseY >= y && mouseY < y + TANK_HEIGHT) {
            guiGraphics.renderTooltip(this.font, Component.translatable("tooltip.matterworks.matter_sludge_tank", menu.getSludgeAmount(), menu.getSludgeCapacity()), mouseX, mouseY);
        }
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, null);
    }
}
