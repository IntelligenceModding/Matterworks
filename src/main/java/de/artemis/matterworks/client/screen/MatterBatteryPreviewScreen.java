package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.client.render.MatterBatteryPreviewState;
import de.artemis.matterworks.common.menu.MatterBatteryPreviewMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MatterBatteryPreviewScreen extends AbstractRenamableContainerScreen<MatterBatteryPreviewMenu> {
    private Button previewButton;
    private Button layerDownButton;
    private Button layerUpButton;

    public MatterBatteryPreviewScreen(MatterBatteryPreviewMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        previewButton = addRenderableWidget(Button.builder(getPreviewButtonLabel(), button -> {
                    MatterBatteryPreviewState.toggle(menu.getBlockEntity());
                    refreshPreviewButtons();
                })
                .bounds(leftPos + 8, topPos + 62, 68, 20)
                .build());
        layerDownButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    MatterBatteryPreviewState.cycleLayer(-1);
                    refreshPreviewButtons();
                })
                .bounds(leftPos + 86, topPos + 62, 20, 20)
                .build());
        layerUpButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    MatterBatteryPreviewState.cycleLayer(1);
                    refreshPreviewButtons();
                })
                .bounds(leftPos + 148, topPos + 62, 20, 20)
                .build());
        refreshPreviewButtons();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshPreviewButtons();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, imageWidth, imageHeight);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);

        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 8, top + 18, 72, 54);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 86, top + 18, 82, 54);

        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.structure"), left + 14, top + 24, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable(menu.isFormed()
                ? "screen.matterworks.matter_battery.formed"
                : "screen.matterworks.matter_battery.not_formed"), left + 14, top + 36, menu.isFormed() ? 0x2F7A31 : 0x8A2F2F, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.cells", menu.getCellCount()), left + 14, top + 46, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.layer", MatterBatteryPreviewState.getSelectedLayer() + 1, MatterBatteryPreviewState.getLayerCount()), left + 14, top + 56, 0x404040, false);

        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.preview_title"), left + 92, top + 24, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.preview_hint"), left + 92, top + 36, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.port_hint"), left + 92, top + 48, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_battery.glass_hint"), left + 92, top + 60, 0x404040, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (MatterBatteryPreviewState.isActive() && scrollY != 0.0D) {
            MatterBatteryPreviewState.cycleLayer(scrollY > 0.0D ? 1 : -1);
            refreshPreviewButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshPreviewButtons() {
        if (previewButton != null) {
            previewButton.setMessage(getPreviewButtonLabel());
        }
        boolean active = MatterBatteryPreviewState.isActive();
        if (layerDownButton != null) {
            layerDownButton.active = active && MatterBatteryPreviewState.getSelectedLayer() > 0;
        }
        if (layerUpButton != null) {
            layerUpButton.active = active && MatterBatteryPreviewState.getSelectedLayer() < MatterBatteryPreviewState.getLayerCount() - 1;
        }
    }

    private Component getPreviewButtonLabel() {
        return Component.translatable(MatterBatteryPreviewState.isActive()
                ? "screen.matterworks.matter_battery.preview_on"
                : "screen.matterworks.matter_battery.preview_off");
    }
}
