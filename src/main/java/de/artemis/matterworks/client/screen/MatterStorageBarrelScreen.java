package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
import de.artemis.matterworks.common.network.OpenMatterNetworkMenuPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class MatterStorageBarrelScreen extends AbstractRenamableContainerScreen<MatterStorageBarrelMenu> {
    private final MachineSideConfigController sideConfig = new MachineSideConfigController();

    public MatterStorageBarrelScreen(MatterStorageBarrelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 244;
        this.inventoryLabelY = 150;
    }

    @Override
    protected void init() {
        super.init();
        sideConfig.init(menu);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 6, top + 16, 164, 110);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 6, top + 160, 164, 56);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 6, top + 218, 164, 20);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);
        sideConfig.renderOverlay(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (sideConfig.isShowing()) {
            sideConfig.renderTooltip(guiGraphics, this.font, menu, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
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

    private List<TopCategoryTabs.Tab> buildTabs() {
        return sideConfig.buildTabs(menu, () -> PacketDistributor.sendToServer(new OpenMatterNetworkMenuPayload(menu.getBlockPos(), menu.isRemoteAccess())));
    }
}
